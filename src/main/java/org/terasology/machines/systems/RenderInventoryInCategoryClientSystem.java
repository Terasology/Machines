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
package org.terasology.machines.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.module.inventory.components.InventoryAccessComponent;
import org.terasology.itemRendering.components.RenderInventorySlotsComponent;
import org.terasology.machines.components.RenderInventoryInCategoryComponent;
import org.terasology.workstation.process.WorkstationInventoryUtils;

import java.util.List;

@RegisterSystem(RegisterMode.CLIENT)
public class RenderInventoryInCategoryClientSystem extends BaseComponentSystem {

    @ReceiveEvent
    public void addRemoveItemRendering(OnChangedComponent event,
                                       EntityRef inventoryEntity,
                                       RenderInventoryInCategoryComponent renderInventoryInCategoryComponent,
                                       InventoryAccessComponent categorizedInventory) {
        addSlotRenderer(inventoryEntity, renderInventoryInCategoryComponent, categorizedInventory);
    }

    @ReceiveEvent
    public void initExistingItemRendering(OnActivatedComponent event,
                                          EntityRef inventoryEntity,
                                          RenderInventoryInCategoryComponent renderInventoryInCategoryComponent,
                                          InventoryAccessComponent categorizedInventory) {
        addSlotRenderer(inventoryEntity, renderInventoryInCategoryComponent, categorizedInventory);
    }

    @ReceiveEvent
    public void removeItemRendering(BeforeDeactivateComponent event, EntityRef inventoryEntity, RenderInventoryInCategoryComponent renderInventoryInCategoryComponent) {
        inventoryEntity.removeComponent(RenderInventorySlotsComponent.class);
    }

    private void addSlotRenderer(EntityRef inventoryEntity,
                                 RenderInventoryInCategoryComponent renderInventoryInCategoryComponent,
                                 InventoryAccessComponent categorizedInventory) {
        List<Integer> slots = WorkstationInventoryUtils.getAssignedSlots(
                inventoryEntity,
                renderInventoryInCategoryComponent.isOutputCategory,
                renderInventoryInCategoryComponent.category);
        RenderInventorySlotsComponent renderInventorySlotsComponent = new RenderInventorySlotsComponent();
        renderInventorySlotsComponent.slots = slots;
        renderInventorySlotsComponent.setRenderDetails(renderInventoryInCategoryComponent);

        if (inventoryEntity.hasComponent(RenderInventorySlotsComponent.class)) {
            inventoryEntity.saveComponent(renderInventorySlotsComponent);
        } else {
            inventoryEntity.addComponent(renderInventorySlotsComponent);
        }
    }
}
