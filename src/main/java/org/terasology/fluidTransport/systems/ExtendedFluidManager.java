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
import org.terasology.fluid.component.FluidComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluid.system.FluidManager;
import org.terasology.registry.CoreRegistry;

public final class ExtendedFluidManager {

    private ExtendedFluidManager() {
    }

    public static void removeFluid(EntityRef entity, float volume, String fluidType) {
        FluidManager fluidManager = CoreRegistry.get(FluidManager.class);
        if (volume > 0) {
            fluidManager.removeFluid(entity, entity, fluidType, volume);
        }
    }


    public static float giveFluid(EntityRef entity, float volume, String fluidType) {
        FluidManager fluidManager = CoreRegistry.get(FluidManager.class);
        float amountToGive = Math.min(volume, getFirstFluidSlotMaximumVolume(entity) - getTankFluidVolume(entity));
        if (fluidManager.addFluid(entity, entity, fluidType, amountToGive)) {
            return amountToGive;
        } else {
            return 0;
        }
    }

    public static float getTankTotalVolume(EntityRef entity) {
        return getFirstFluidSlotMaximumVolume(entity);
    }

    public static String getTankFluidType(EntityRef entity) {
        FluidComponent fluidComponent = getFirstFluidSlotFluidComponent(entity);
        if (fluidComponent != null) {
            return fluidComponent.fluidType;
        } else {
            return null;
        }
    }

    public static float getTankFluidVolume(EntityRef entity) {
        FluidComponent fluidComponent = getFirstFluidSlotFluidComponent(entity);
        if (fluidComponent != null) {
            return fluidComponent.volume;
        } else {
            return 0;
        }
    }

    private static FluidComponent getFirstFluidSlotFluidComponent(EntityRef entity) {
        Integer mainFluidSlot = getFirstFluidSlot(entity);
        if (mainFluidSlot != null) {
            FluidInventoryComponent fluidInventoryComponent = entity.getComponent(FluidInventoryComponent.class);
            return fluidInventoryComponent.fluidSlots.get(mainFluidSlot).getComponent(FluidComponent.class);
        } else {
            return null;
        }
    }

    private static float getFirstFluidSlotMaximumVolume(EntityRef entity) {
        FluidInventoryComponent fluidInventoryComponent = entity.getComponent(FluidInventoryComponent.class);
        Integer mainFluidSlot = getFirstFluidSlot(entity);

        if (mainFluidSlot != null) {
            return fluidInventoryComponent.maximumVolumes.get(mainFluidSlot);
        } else {
            return fluidInventoryComponent.maximumVolumes.get(0);
        }
    }

    private static Integer getFirstFluidSlot(EntityRef entity) {
        FluidInventoryComponent fluidInventoryComponent = entity.getComponent(FluidInventoryComponent.class);
        if (fluidInventoryComponent != null) {
            for (int i = 0; i < fluidInventoryComponent.fluidSlots.size(); i++) {
                if (fluidInventoryComponent.fluidSlots.get(i).exists()) {
                    return i;
                }
            }
        }
        return 0;
    }

    public static boolean isTank(EntityRef entity) {
        return entity.hasComponent(FluidInventoryComponent.class);
    }

    public static float getTankEmptyVolume(EntityRef entity) {
        return getTankTotalVolume(entity) - getTankFluidVolume(entity);

    }
}
