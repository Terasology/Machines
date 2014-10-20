/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.itemRendering.systems;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.itemRendering.components.RenderInventoryInCategoryComponent;
import org.terasology.itemRendering.components.RenderItemComponent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.mechanicalPower.systems.MechanicalPowerClientSystem;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;

import java.util.List;

@RegisterSystem(RegisterMode.CLIENT)
public class RenderInventoryInCategoryClientSystem extends BaseComponentSystem {

    @In
    InventoryManager inventoryManager;
    @In
    WorldProvider worldProvider;

    @ReceiveEvent
    public void addRemoveItemRendering(OnChangedComponent event,
                                       EntityRef inventoryEntity,
                                       InventoryComponent inventoryComponent) {
        refreshInventoryItems(inventoryEntity);
    }

    @ReceiveEvent
    public void initExistingItemRendering(OnAddedComponent event,
                                          EntityRef inventoryEntity,
                                          InventoryComponent inventoryComponent) {
        refreshInventoryItems(inventoryEntity);
    }

    private void refreshInventoryItems(EntityRef inventoryEntity) {
        RenderInventoryInCategoryComponent renderInventoryInCategory = inventoryEntity.getComponent(RenderInventoryInCategoryComponent.class);
        CategorizedInventoryComponent categorizedInventory = inventoryEntity.getComponent(CategorizedInventoryComponent.class);

        List<Integer> slots = Lists.newArrayList();
        if (categorizedInventory != null && renderInventoryInCategory != null) {
            slots = categorizedInventory.slotMapping.get(renderInventoryInCategory.category);
        }

        for (int slot = 0; slot < inventoryManager.getNumSlots(inventoryEntity); slot++) {
            EntityRef item = inventoryManager.getItemInSlot(inventoryEntity, slot);
            if (slots.contains(slot)) {
                addRenderingComponents(inventoryEntity, renderInventoryInCategory, item);
            } else {
                removeRenderingComponents(item);
            }

        }
    }

    private void removeRenderingComponents(EntityRef item) {
        item.removeComponent(RenderItemComponent.class);
    }

    private void addRenderingComponents(EntityRef inventoryEntity, RenderInventoryInCategoryComponent renderInventoryInCategory, EntityRef item) {
        if (item.exists()) {
            // this is inherently evil,  but multiplayer acts strangely
            item.setOwner(inventoryEntity);

            LocationComponent parentLocationComponent = inventoryEntity.getComponent(LocationComponent.class);
            RenderItemComponent renderItemTransform = renderInventoryInCategory.createRenderItemComponent(inventoryEntity, item);
            if (renderInventoryInCategory.rotateWithBlock && worldProvider.isBlockRelevant(parentLocationComponent.getWorldPosition())) {
                Block block = worldProvider.getBlock(parentLocationComponent.getWorldPosition());
                Side direction = block.getDirection();
                Rotation blockRotation = MechanicalPowerClientSystem.getRotation(direction);
                renderItemTransform.yaw = blockRotation.getYaw();
                renderItemTransform.pitch = blockRotation.getPitch();
                renderItemTransform.roll = blockRotation.getRoll();

                renderItemTransform.translate = MechanicalPowerClientSystem.rotateVector3f(renderItemTransform.translate, direction.toDirection());
            }

            if (item.hasComponent(RenderItemComponent.class)) {
                item.saveComponent(renderItemTransform);
            } else {
                item.addComponent(renderItemTransform);
            }
        }
    }
}
