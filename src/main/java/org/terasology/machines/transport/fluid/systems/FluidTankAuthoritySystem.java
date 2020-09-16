// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.transport.fluid.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.fluid.component.FluidContainerItemComponent;
import org.terasology.fluid.system.FluidUtils;
import org.terasology.inventory.logic.InventoryManager;

@RegisterSystem(RegisterMode.AUTHORITY)
public class FluidTankAuthoritySystem extends BaseComponentSystem {
    @In
    InventoryManager inventoryManager;

    // Prioritize interacting with tanks over other things
    @ReceiveEvent(components = {ItemComponent.class}, priority = EventPriority.PRIORITY_HIGH)
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
            } else if (fluidContainer.fluidType != null && fluidContainer.volume <= inputTankEmptyVolume
                    // if the fluid types are the same,  or if the block does not have a fluid type
                    && (inputTankFluidType == null || inputTankFluidType.equals(fluidContainer.fluidType))) {
                // empty the container to the block
                ExtendedFluidManager.giveFluid(targetBlockEntity, fluidContainer.volume, fluidContainer.fluidType,
                        true);
                fillFluidContainer(event, item, null, 0);
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
