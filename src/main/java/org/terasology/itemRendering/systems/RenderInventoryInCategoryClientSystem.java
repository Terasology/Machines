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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.itemRendering.components.RenderInventoryInCategoryComponent;
import org.terasology.itemRendering.components.RenderItemTransformComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.events.InventorySlotChangedEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.registry.In;
import org.terasology.world.block.BlockComponent;

import java.util.List;

@RegisterSystem(RegisterMode.CLIENT)
public class RenderInventoryInCategoryClientSystem implements ComponentSystem {

    @In
    InventoryManager inventoryManager;

    @Override
    public void initialise() {

    }

    @Override
    public void shutdown() {

    }

    @ReceiveEvent(components = {BlockComponent.class, LocationComponent.class})
    public void addToolDisplayItem(InventorySlotChangedEvent event, EntityRef inventoryEntity, RenderInventoryInCategoryComponent renderInventoryInCategory, CategorizedInventoryComponent categorizedInventory) {
        EntityRef oldItem = event.getOldItem();
        if (oldItem.exists()) {
            // ensure that rendered items get reset
            oldItem.removeComponent(RenderItemTransformComponent.class);
        }

        EntityRef newItem = event.getNewItem();
        if (newItem.exists()) {
            List<Integer> slots = categorizedInventory.slotMapping.get(renderInventoryInCategory.category);
            int newItemSlot = inventoryManager.findSlotWithItem(inventoryEntity, newItem);
            if (slots.contains(newItemSlot)) {
                newItem.addComponent(renderInventoryInCategory.createRenderItemTransformComponent(inventoryEntity, newItem));
            }
        }
    }


}
