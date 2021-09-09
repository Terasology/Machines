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
package org.terasology.fluidTransport.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.fluid.component.FluidContainerItemComponent;
import org.terasology.fluid.system.FluidUtils;
import org.terasology.module.inventory.systems.InventoryManager;

@RegisterSystem(RegisterMode.AUTHORITY)
public class FluidTankAuthoritySystem extends BaseComponentSystem {
    @In
    InventoryManager inventoryManager;

    // Prioritize interacting with tanks over other things
    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = {ItemComponent.class})
    public void fillFluidContainerItem(ActivateEvent event, EntityRef item,
                                       FluidContainerItemComponent fluidContainer) {
        EntityRef targetBlockEntity = event.getTarget();
        if (ExtendedFluidManager.isTank(targetBlockEntity)) {
            float inputTankVolume = ExtendedFluidManager.getTankFluidVolume(targetBlockEntity, true);
            float inputTankEmptyVolume = ExtendedFluidManager.getTankEmptyVolume(targetBlockEntity, true);
            String inputTankFluidType = ExtendedFluidManager.getTankFluidType(targetBlockEntity, true);
            float outputTankVolume = ExtendedFluidManager.getTankFluidVolume(targetBlockEntity, false);
            float outputTankEmptyVolume = ExtendedFluidManager.getTankEmptyVolume(targetBlockEntity, false);
            String outputTankFluidType = ExtendedFluidManager.getTankFluidType(targetBlockEntity, false);

            if (outputTankVolume > 0 && fluidContainer.fluidType == null && outputTankFluidType != null) {
                // fill the container from the block
                float amountToFill = Math.min(fluidContainer.maxVolume, outputTankVolume);
                fillFluidContainer(event, item, outputTankFluidType, amountToFill);
                ExtendedFluidManager.removeFluid(targetBlockEntity, amountToFill, outputTankFluidType);
                event.consume();
            } else if (fluidContainer.fluidType != null && fluidContainer.volume <= inputTankEmptyVolume
                    // if the fluid types are the same,  or if the block does not have a fluid type
                    && (inputTankFluidType == null || inputTankFluidType.equals(fluidContainer.fluidType))) {
                // empty the container to the block
                ExtendedFluidManager.giveFluid(targetBlockEntity, fluidContainer.volume, fluidContainer.fluidType, true);
                fillFluidContainer(event, item, null, 0);
                event.consume();
            }
        }
    }

    private void fillFluidContainer(ActivateEvent event, EntityRef item, String fluidType, float volume) {
        EntityRef owner = item.getOwner();
        final EntityRef removedItem = inventoryManager.removeItem(owner, event.getInstigator(), item, false, 1);
        if (removedItem != null) {
            FluidUtils.setFluidForContainerItem(removedItem, fluidType, volume);
            if (!inventoryManager.giveItem(owner, event.getInstigator(), removedItem)) {
                removedItem.destroy();
            }
        }
    }
}
