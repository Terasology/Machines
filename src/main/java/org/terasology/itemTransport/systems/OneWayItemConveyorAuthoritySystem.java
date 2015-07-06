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
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.itemRendering.components.AnimatedMovingItemComponent;
import org.terasology.itemTransport.components.PullInventoryInDirectionComponent;
import org.terasology.itemTransport.components.PushInventoryInDirectionComponent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.events.InventorySlotChangedEvent;
import org.terasology.logic.inventory.events.InventorySlotStackSizeChangedEvent;
import org.terasology.machines.BlockFamilyUtil;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.inventory.InventoryOutputComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class OneWayItemConveyorAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final String ACTIVITY = "pushAndPullInventory";

    @In
    private InventoryManager inventoryManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private Time time;
    @In
    private EntityManager entityManager;
    @In
    private DelayManager delayManager;

    private Deque<EntityRef> pendingPushChecks = Lists.newLinkedList();
    private Deque<EntityRef> pendingPullChecks = Lists.newLinkedList();

    @Override
    public void update(float delta) {
        while (!pendingPushChecks.isEmpty() || !pendingPullChecks.isEmpty()) {
            PerformanceMonitor.startActivity(ACTIVITY);
            try {
                // Pull checks first
                while (!pendingPullChecks.isEmpty()) {
                    EntityRef entity = pendingPullChecks.removeFirst();
                    PullInventoryInDirectionComponent pullInventory = entity.getComponent(PullInventoryInDirectionComponent.class);
                    BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
                    if (pullInventory != null && blockComponent != null) {
                        processPull(entity, pullInventory, blockComponent);
                    }
                }

                // Then all the pushes
                while (!pendingPushChecks.isEmpty()) {
                    EntityRef entity = pendingPushChecks.removeFirst();
                    checkForPushFinish(entity);
                }
            } finally {
                PerformanceMonitor.endActivity();
            }
        }
    }

    private void processPull(EntityRef entity, PullInventoryInDirectionComponent pullInventory, BlockComponent blockComponent) {
        // find out what way this block is pointed
        Side side = getRelativeSide(entity, pullInventory.direction);

        // get target inventory
        Vector3i adjacentPos = side.getAdjacentPos(blockComponent.getPosition());
        EntityRef targetEntity = blockEntityRegistry.getExistingBlockEntityAt(adjacentPos);

        if (targetEntity.hasComponent(InventoryComponent.class)) {
            // iterate all the items in the inventory and pull one stack to this inventory
            Iterable<EntityRef> items = null;
            if (WorkstationInventoryUtils.hasAssignedSlots(targetEntity, true, InventoryOutputComponent.WORKSTATIONOUTPUTCATEGORY)) {
                items = ExtendedInventoryManager.iterateItems(inventoryManager, targetEntity, true, InventoryOutputComponent.WORKSTATIONOUTPUTCATEGORY);
            } else {
                items = ExtendedInventoryManager.iterateItems(inventoryManager, targetEntity);
            }

            for (EntityRef item : items) {
                if (item.exists()) {
                    // grab the item to the target inventory
                    EntityRef entityToGive = inventoryManager.removeItem(targetEntity, entity, item, false, 1);
                    inventoryManager.giveItem(entity, entity, entityToGive);

                    // only do one slots worth at a time
                    break;
                }
            }
        }
    }

    private void checkForPushFinish(EntityRef entity) {
        PushInventoryInDirectionComponent pushInventory = entity.getComponent(PushInventoryInDirectionComponent.class);
        if (pushInventory != null && pushInventory.pushFinishTime <= time.getGameTimeInMs()) {
            finishPushing(entity, pushInventory, entity.getComponent(BlockComponent.class));
        }
    }

    private void finishPushing(EntityRef entity, PushInventoryInDirectionComponent pushInventory, BlockComponent blockComponent) {
        Side side = getRelativeSide(entity, pushInventory.direction);

        // get target inventory
        Vector3i adjacentPos = side.getAdjacentPos(blockComponent.getPosition());
        EntityRef targetEntity = blockEntityRegistry.getExistingBlockEntityAt(adjacentPos);

        if (targetEntity.hasComponent(InventoryComponent.class)) {
            int targetSlotCount = InventoryUtils.getSlotCount(targetEntity);
            List<Integer> targetSlots = new ArrayList<>(targetSlotCount);
            for (int i = 0; i < targetSlotCount; i++) {
                targetSlots.add(i);
            }

            EntityRef item = InventoryUtils.getItemAt(entity, 0);
            if (item.exists()) {

                // If entity can accept item - it's not conveyor belt, or empty conveyor
                if (entityCanAcceptItem(targetEntity)) {
                    if (!targetEntity.hasComponent(PushInventoryInDirectionComponent.class)
                            || !targetEntity.getComponent(PushInventoryInDirectionComponent.class).animateMovingItem) {
                        item.removeComponent(AnimatedMovingItemComponent.class);
                    }
                    inventoryManager.moveItemToSlots(entity, entity, 0, targetEntity, targetSlots);
                }
            }
        }
    }

    @ReceiveEvent(activity = ACTIVITY)
    public void pushInventoryGotItem(InventorySlotChangedEvent event, EntityRef entity, PushInventoryInDirectionComponent pushInventory,
                                     BlockComponent block, InventoryComponent inventory) {
        Side side = getRelativeSide(entity, pushInventory.direction);

        boolean atLeastOnePushing = false;
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
            if (!item.exists()) {
                continue;
            }

            long pushStart = time.getGameTimeInMs();
            long pushEnd = pushStart + pushInventory.timeToDestination;

            if (!item.hasComponent(AnimatedMovingItemComponent.class)) {
                if (pushInventory.animateMovingItem) {
                    AnimatedMovingItemComponent animatedMovingItemComponent = new AnimatedMovingItemComponent();
                    animatedMovingItemComponent.entranceSide = side.reverse();
                    animatedMovingItemComponent.exitSide = side;
                    animatedMovingItemComponent.startTime = pushStart;
                    animatedMovingItemComponent.arrivalTime = pushEnd;

                    item.addComponent(animatedMovingItemComponent);
                }
            } else {
                AnimatedMovingItemComponent animatedMovingItemComponent = item.getComponent(AnimatedMovingItemComponent.class);
                animatedMovingItemComponent.entranceSide = animatedMovingItemComponent.exitSide.reverse();
                animatedMovingItemComponent.exitSide = side;
                animatedMovingItemComponent.startTime = pushStart;
                animatedMovingItemComponent.arrivalTime = pushEnd;

                item.saveComponent(animatedMovingItemComponent);
            }

            pushInventory.pushFinishTime = pushEnd;
            entity.saveComponent(pushInventory);

            atLeastOnePushing = true;
        }

        if (atLeastOnePushing) {
            delayManager.addDelayedAction(entity, "FINISHED_PUSHING", pushInventory.timeToDestination);
        }
    }

    // This part is responsible for finishing pushing an item, finish pushing should be attempted when:
    // 1. Delay has been finished
    // 2. Inventory of an adjacent block has changed (maybe there is space there now?)
    // 3. New inventory was placed or loaded in an adjacent block

    @ReceiveEvent(activity = ACTIVITY)
    public void pushCondition1(DelayedActionTriggeredEvent event, EntityRef entity, PushInventoryInDirectionComponent pushInventory,
                               BlockComponent blockComponent, InventoryComponent inventory) {
        if (event.getActionId().equals("FINISHED_PUSHING")) {
            pendingPushChecks.add(entity);
        }
    }

    @ReceiveEvent(activity = ACTIVITY)
    public void pushCondition2(InventorySlotChangedEvent event, EntityRef entity, InventoryComponent inventory,
                               BlockComponent block) {
        checkAdjacentBlocksForPushFinish(block);
    }

    @ReceiveEvent(activity = ACTIVITY)
    public void pushCondition2(InventorySlotStackSizeChangedEvent event, EntityRef entity, InventoryComponent inventory,
                               BlockComponent block) {
        checkAdjacentBlocksForPushFinish(block);
    }

    @ReceiveEvent(activity = ACTIVITY)
    public void pushCondition3(OnActivatedComponent event, EntityRef entity, InventoryComponent inventory,
                               BlockComponent block) {
        checkAdjacentBlocksForPushFinish(block);
    }

    private void checkAdjacentBlocksForPushFinish(BlockComponent block) {
        Vector3i position = block.getPosition();
        for (Side side : Side.values()) {
            Vector3i adjacentPos = side.getAdjacentPos(position);
            EntityRef blockEntityAt = blockEntityRegistry.getExistingBlockEntityAt(adjacentPos);
            if (blockEntityAt.hasComponent(PushInventoryInDirectionComponent.class)) {
                PushInventoryInDirectionComponent pushInventory = blockEntityAt.getComponent(PushInventoryInDirectionComponent.class);
                if (side.reverse() == getRelativeSide(blockEntityAt, pushInventory.direction)) {
                    pendingPushChecks.add(blockEntityAt);
                }
            }
        }
    }

    private boolean entityCanAcceptItem(EntityRef targetEntity) {
        return !targetEntity.hasComponent(PushInventoryInDirectionComponent.class) || inventoryHasEmptySlot(targetEntity);
    }

    // This part is responsible for pulling an item, pulling should be attempted when:
    // 1. Inventory of the PullInventory block has changed (maybe it has space now?)
    // 2. PullInventory block was placed
    // 3. Inventory of an adjacent block has changed (maybe there is something new to pull?)
    // 4. New inventory was placed or loaded in an adjacent block

    @ReceiveEvent(activity = ACTIVITY)
    public void pullCondition1(InventorySlotChangedEvent event, EntityRef entity, PullInventoryInDirectionComponent pullInventory,
                               BlockComponent blockComponent, InventoryComponent inventory) {
        tryPullingAnItem(entity);
    }

    @ReceiveEvent(activity = ACTIVITY)
    public void pullCondition2(OnAddedComponent event, EntityRef entity, PullInventoryInDirectionComponent pullInventory,
                               BlockComponent blockComponent, InventoryComponent inventory) {
        tryPullingAnItem(entity);
    }

    @ReceiveEvent(activity = ACTIVITY)
    public void pullCondition3(InventorySlotChangedEvent event, EntityRef entity, InventoryComponent inventory,
                               BlockComponent block) {
        checkAdjacentBlocksForPulling(block);
    }

    @ReceiveEvent(activity = ACTIVITY)
    public void pullCondition4(OnActivatedComponent event, EntityRef entity, InventoryComponent inventory,
                               BlockComponent block) {
        checkAdjacentBlocksForPulling(block);
    }

    private void checkAdjacentBlocksForPulling(BlockComponent block) {
        Vector3i position = block.getPosition();
        for (Side side : Side.values()) {
            EntityRef adjacentEntity = blockEntityRegistry.getExistingBlockEntityAt(side.getAdjacentPos(position));
            if (adjacentEntity.hasComponent(PullInventoryInDirectionComponent.class)) {
                PullInventoryInDirectionComponent pullInventory = adjacentEntity.getComponent(PullInventoryInDirectionComponent.class);
                if (side.reverse() == getRelativeSide(adjacentEntity, pullInventory.direction)) {
                    tryPullingAnItem(adjacentEntity);
                }
            }
        }
    }

    private void tryPullingAnItem(EntityRef entity) {
        if (inventoryHasEmptySlot(entity)) {
            pendingPullChecks.add(entity);
        }
    }

    private boolean inventoryHasEmptySlot(EntityRef entity) {
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
            if (!item.exists()) {
                return true;
            }
        }
        return false;
    }

    private Side getRelativeSide(EntityRef entity, Direction direction) {
        // find out what way this block is pointed
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        Block block = blockComponent.getBlock();
        return BlockFamilyUtil.getSideDefinedDirection(block).getRelativeSide(direction);
    }
}
