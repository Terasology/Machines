/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.itemTransport.systems;

import com.google.common.collect.Lists;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.itemRendering.components.AnimatedMovingItemComponent;
import org.terasology.itemTransport.components.PullInventoryInDirectionComponent;
import org.terasology.itemTransport.components.PushInventoryInDirectionComponent;
import org.terasology.itemTransport.events.ConveyorItemStuckEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.Vector3i;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class OneWayItemConveyorSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    static final long UPDATE_INTERVAL = 2000;

    @In
    InventoryManager inventoryManager;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    BlockManager blockManager;
    @In
    Time time;
    @In
    EntityManager entityManager;

    long nextUpdateTime;

    @Override
    public void initialise() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void update(float delta) {
        long currentTime = time.getGameTimeInMs();
        if (nextUpdateTime <= currentTime) {
            nextUpdateTime = currentTime + UPDATE_INTERVAL;

            List<EntityRef> alreadyMovedItems = Lists.newArrayList();

            for (EntityRef entity : entityManager.getEntitiesWith(
                    PushInventoryInDirectionComponent.class,
                    LocationComponent.class,
                    BlockComponent.class,
                    InventoryComponent.class)) {
                PushInventoryInDirectionComponent pushInventoryInDirectionComponent = entity.getComponent(PushInventoryInDirectionComponent.class);
                LocationComponent locationComponent = entity.getComponent(LocationComponent.class);

                Side side = getRelativeSide(entity, pushInventoryInDirectionComponent.direction);


                // get target inventory
                Vector3i adjacentPos = side.getAdjacentPos(new Vector3i(locationComponent.getWorldPosition()));
                EntityRef targetEntity = blockEntityRegistry.getBlockEntityAt(adjacentPos);

                if (targetEntity.hasComponent(InventoryComponent.class)) {
                    // iterate all the items in the inventory and send them to an adjacent inventory
                    for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
                        if (!item.exists() || alreadyMovedItems.contains(item)) {
                            continue;
                        }

                        AnimatedMovingItemComponent animatedMovingItemComponent = item.getComponent(AnimatedMovingItemComponent.class);
                        if (animatedMovingItemComponent != null && animatedMovingItemComponent.arrivalTime > currentTime) {
                            // this item is not at the endpoint yet
                            continue;
                        }

                        // add/update the animation
                        PushInventoryInDirectionComponent targetPushInventoryInDirectionComponent = targetEntity.getComponent(PushInventoryInDirectionComponent.class);
                        if (targetPushInventoryInDirectionComponent != null && targetPushInventoryInDirectionComponent.animateMovingItem) {

                            // create the animation component
                            animatedMovingItemComponent = new AnimatedMovingItemComponent();
                            animatedMovingItemComponent.entranceSide = side.reverse();
                            animatedMovingItemComponent.exitSide = getRelativeSide(targetEntity, targetPushInventoryInDirectionComponent.direction);
                            animatedMovingItemComponent.startTime = time.getGameTimeInMs();
                            animatedMovingItemComponent.arrivalTime = animatedMovingItemComponent.startTime + UPDATE_INTERVAL;
                            if (item.hasComponent(AnimatedMovingItemComponent.class)) {
                                item.saveComponent(animatedMovingItemComponent);
                            } else {
                                item.addComponent(animatedMovingItemComponent);
                            }
                        } else {
                            // remove the animation
                            item.removeComponent(AnimatedMovingItemComponent.class);
                        }

                        alreadyMovedItems.add(item);
                        // send the item to the target inventory
                        if (inventoryManager.giveItem(targetEntity, entity, item)) {
                            inventoryManager.removeItem(entity, entity, item, false);
                        }
                    }
                } else {
                    boolean hasItems = false;
                    for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
                        if (item.exists()) {
                            hasItems = true;
                            break;
                        }
                    }
                    if (hasItems) {
                        entity.send(new ConveyorItemStuckEvent(adjacentPos));
                    }
                }
            }


            // pull items
            for (EntityRef entity : entityManager.getEntitiesWith(
                    PullInventoryInDirectionComponent.class,
                    LocationComponent.class,
                    BlockComponent.class,
                    InventoryComponent.class)) {
                PullInventoryInDirectionComponent pullInventoryInDirectionComponent = entity.getComponent(PullInventoryInDirectionComponent.class);
                LocationComponent locationComponent = entity.getComponent(LocationComponent.class);
                BlockComponent blockComponent = entity.getComponent(BlockComponent.class);

                // find out what way this block is pointed
                Block block = blockComponent.getBlock();
                Side side = block.getDirection().getRelativeSide(pullInventoryInDirectionComponent.direction);

                // get target inventory
                Vector3i adjacentPos = side.getAdjacentPos(new Vector3i(locationComponent.getWorldPosition()));
                EntityRef targetEntity = blockEntityRegistry.getBlockEntityAt(adjacentPos);

                if (targetEntity.hasComponent(InventoryComponent.class)) {
                    // iterate all the items in the inventory and pull one stack to this inventory
                    for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, targetEntity, side.reverse())) {
                        if (item.exists()) {
                            // grab the item to the target inventory

                            if (inventoryManager.giveItem(entity, entity, item)) {
                                inventoryManager.removeItem(targetEntity, entity, item, false);
                            }
                            // only do one stack at a time
                            break;
                        }
                    }
                }
            }


        }
/*
        // drop all stale items
        for (EntityRef entity : entityManager.getEntitiesWith(AnimatedMovingItemComponent.class)) {
            AnimatedMovingItemComponent animatedMovingItemComponent = entity.getComponent(AnimatedMovingItemComponent.class);
            if( currentTime - animatedMovingItemComponent.arrivalTime > UPDATE_INTERVAL) {
                // this is a stale old item. create a pickup item for it
                ExtendedInventoryManager.dropItem(entity, animatedMovingItemComponent.location);
            }
        }
*/
    }

    private Side getRelativeSide(EntityRef entity, Direction direction) {
        // find out what way this block is pointed
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        Block block = blockComponent.getBlock();
        return block.getDirection().getRelativeSide(direction);
    }

}
