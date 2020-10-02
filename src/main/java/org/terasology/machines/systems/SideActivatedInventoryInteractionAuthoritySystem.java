/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.machines.systems;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.input.binds.inventory.UseItemButton;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.machines.components.SideActivatedInventoryInteractionComponent;
import org.terasology.math.Direction;
import org.terasology.math.JomlUtil;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.registry.In;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.family.SideDefinedBlockFamily;

import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SideActivatedInventoryInteractionAuthoritySystem extends BaseComponentSystem {
    @In
    InventoryManager inventoryManager;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    Physics physics;

    private static CollisionGroup[] filter = {StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD};


    /// Capture the raw button event manually so that we can do this inventory interaction with an empty hand
    @ReceiveEvent(components = {CharacterComponent.class, InventoryComponent.class})
    public void onUseItemButton(UseItemButton event, EntityRef entity, CharacterComponent characterComponent) {

        // TODO: characterComponent no longer contains the selected item, new component available
        //EntityRef selectedItemEntity = InventoryUtils.getItemAt(entity, characterComponent.selectedItem);

        /* if (selectedItemEntity.exists()) {
            // let the normal handler get this
            return;
        }

        LocationComponent location = entity.getComponent(LocationComponent.class);
        Vector3f direction = characterComponent.getLookDirection();
        Vector3f originPos = location.getWorldPosition();
        originPos.y += characterComponent.eyeOffset;
        HitResult result = physics.rayTrace(originPos, direction, characterComponent.interactionRange, filter);

        if( result.isWorldHit()) {
            EntityRef targetBlockEntity = blockEntityRegistry.getBlockEntityAt(result.getBlockPosition());
            doInventoryInteraction(result.getHitNormal(), selectedItemEntity, targetBlockEntity, entity);
            event.consume();
        }*/
    }

    @ReceiveEvent
    public void onSideActivated(ActivateEvent event, EntityRef heldItem, ItemComponent itemComponent) {
        EntityRef target = event.getTarget();
        EntityRef instigator = event.getInstigator();
        doInventoryInteraction(JomlUtil.from(event.getHitNormal()), heldItem, target, instigator);
    }

    void doInventoryInteraction(Vector3f hitNormal, EntityRef heldItem, EntityRef target, EntityRef instigator) {
        SideActivatedInventoryInteractionComponent interactionComponent = target.getComponent(SideActivatedInventoryInteractionComponent.class);
        BlockComponent blockComponent = target.getComponent(BlockComponent.class);
        InventoryComponent blockInventoryComponent = target.getComponent(InventoryComponent.class);
        if (interactionComponent != null && blockComponent != null && blockInventoryComponent != null) {
            Direction direction = Direction.valueOf(interactionComponent.direction);
            Side blockSideHit = Side.inDirection(hitNormal);
            Side blockRelativeSideHit = blockSideHit;

            // if this is a rotatable block, ensure we are using its rotated side
            BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();
            if (blockFamily instanceof SideDefinedBlockFamily) {
                SideDefinedBlockFamily sideDefinedBlockFamily = (SideDefinedBlockFamily) blockFamily;
                blockRelativeSideHit = sideDefinedBlockFamily.getSide(blockComponent.getBlock());
            }

            if (direction.equals(blockRelativeSideHit.toDirection())) {
                // we have hit a side that does inventory interaction
                // get the range of slots to interact with

                int heldItemSlot = inventoryManager.findSlotWithItem(instigator, heldItem);
                if (heldItem.exists()) {
                    // we are holding an item, and would like to place it into this block
                    List<Integer> slots = WorkstationInventoryUtils.getAssignedSlots(target, interactionComponent.inputIsOutputType, interactionComponent.inputType);
                    inventoryManager.moveItemToSlots(instigator, instigator, heldItemSlot, target, slots);
                } else {

                    // we are not holding an item, and would like to retrieve an item from this block
                    List<Integer> slots = WorkstationInventoryUtils.getAssignedSlots(target, interactionComponent.outputIsOutputType, interactionComponent.outputType);
                    for (Integer slot : slots) {
                        EntityRef itemInSlot = inventoryManager.getItemInSlot(target, slot);
                        if (itemInSlot.exists()) {
                            inventoryManager.moveItem(target, instigator, slot, instigator, heldItemSlot, inventoryManager.getStackSize(itemInSlot));
                            break;
                        }
                    }
                }
            }
        }
    }
}
