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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.fluid.component.FluidContainerItemComponent;
import org.terasology.fluid.system.FluidUtils;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.registry.In;

@RegisterSystem(RegisterMode.AUTHORITY)
public class FluidTankAuthoritySystem extends BaseComponentSystem {
    @In
    InventoryManager inventoryManager;

    @ReceiveEvent(components = {ItemComponent.class})
    public void fillFluidContainerItem(ActivateEvent event, EntityRef item,
                                       FluidContainerItemComponent fluidContainer) {
        EntityRef targetBlockEntity = event.getTarget();
        if (ExtendedFluidManager.isTank(targetBlockEntity)) {
            float tankVolume = ExtendedFluidManager.getTankFluidVolume(targetBlockEntity);
            float tankEmptyVolume = ExtendedFluidManager.getTankEmptyVolume(targetBlockEntity);
            String tankFluidType = ExtendedFluidManager.getTankFluidType(targetBlockEntity);

            if (fluidContainer.volume <= tankVolume && fluidContainer.fluidType == null && tankFluidType != null) {
                // fill the container from the block
                fillFluidContainer(event, item, tankFluidType);
                ExtendedFluidManager.removeFluid(targetBlockEntity, fluidContainer.volume, tankFluidType);
            } else if (fluidContainer.fluidType != null && fluidContainer.volume <= tankEmptyVolume
                    // if the fluid types are the same,  or if the block does not have a fluid type
                    && (tankFluidType == null || tankFluidType.equals(fluidContainer.fluidType))) {
                // empty the container to the block
                ExtendedFluidManager.giveFluid(targetBlockEntity, fluidContainer.volume, fluidContainer.fluidType);
                fillFluidContainer(event, item, null);
            }
        }
    }

    private void fillFluidContainer(ActivateEvent event, EntityRef item, String fluidType) {
        EntityRef owner = item.getOwner();
        final EntityRef removedItem = inventoryManager.removeItem(owner, event.getInstigator(), item, false, 1);
        if (removedItem != null) {
            FluidUtils.setFluidForContainerItem(removedItem, fluidType);
            if (!inventoryManager.giveItem(owner, event.getInstigator(), removedItem)) {
                removedItem.destroy();
            }
        }
    }
}
