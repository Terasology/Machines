// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.transport.fluid.systems;

import com.google.common.collect.Iterables;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.IntegerRange;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.fluid.component.FluidComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluid.system.FluidManager;
import org.terasology.inventory.logic.InventoryAccessComponent;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.fluid.FluidInputProcessPartCommonSystem;
import org.terasology.workstation.process.fluid.FluidOutputProcessPartCommonSystem;

public final class ExtendedFluidManager {

    private ExtendedFluidManager() {
    }

    public static void removeFluid(EntityRef entity, float volume, String fluidType) {
        FluidManager fluidManager = CoreRegistry.get(FluidManager.class);
        if (volume > 0 && fluidType != null) {
            fluidManager.removeFluid(entity, entity, fluidType, volume);
        }
    }


    public static float giveFluid(EntityRef entity, float volume, String fluidType, boolean forInput) {
        FluidManager fluidManager = CoreRegistry.get(FluidManager.class);
        float amountToGive = Math.min(volume,
                getFirstFluidSlotMaximumVolume(entity, forInput) - getTankFluidVolume(entity, forInput));
        if (volume > 0 && fluidType != null && fluidManager.addFluid(entity, entity, fluidType, amountToGive)) {
            return amountToGive;
        } else {
            return 0;
        }
    }

    public static float getTankTotalVolume(EntityRef entity, boolean forInput) {
        return getFirstFluidSlotMaximumVolume(entity, forInput);
    }

    public static String getTankFluidType(EntityRef entity, boolean forInput) {
        FluidComponent fluidComponent = getFirstFluidSlotFluidComponent(entity, forInput);
        if (fluidComponent != null) {
            return fluidComponent.fluidType;
        } else {
            return null;
        }
    }

    public static float getTankFluidVolume(EntityRef entity, boolean forInput) {
        FluidComponent fluidComponent = getFirstFluidSlotFluidComponent(entity, forInput);
        if (fluidComponent != null) {
            return fluidComponent.volume;
        } else {
            return 0;
        }
    }

    private static FluidComponent getFirstFluidSlotFluidComponent(EntityRef entity, boolean forInput) {
        Integer mainFluidSlot = getFirstFluidSlot(entity, forInput);
        if (mainFluidSlot != null) {
            FluidInventoryComponent fluidInventoryComponent = entity.getComponent(FluidInventoryComponent.class);
            return fluidInventoryComponent.fluidSlots.get(mainFluidSlot).getComponent(FluidComponent.class);
        } else {
            return null;
        }
    }

    private static float getFirstFluidSlotMaximumVolume(EntityRef entity, boolean forInput) {
        FluidInventoryComponent fluidInventoryComponent = entity.getComponent(FluidInventoryComponent.class);
        Integer mainFluidSlot = getFirstFluidSlot(entity, forInput);

        if (mainFluidSlot != null) {
            return fluidInventoryComponent.maximumVolumes.get(mainFluidSlot);
        } else {
            return Iterables.getFirst(fluidInventoryComponent.maximumVolumes, 0f);
        }
    }

    private static Integer getFirstFluidSlot(EntityRef entity, boolean forInput) {
        FluidInventoryComponent fluidInventoryComponent = entity.getComponent(FluidInventoryComponent.class);
        if (fluidInventoryComponent != null) {
            Iterable<Integer> slotRange;
            InventoryAccessComponent inventoryAccessComponent = entity.getComponent(InventoryAccessComponent.class);
            if (inventoryAccessComponent != null) {
                if (forInput) {
                    slotRange = WorkstationInventoryUtils.getAssignedInputSlots(entity,
                            FluidInputProcessPartCommonSystem.FLUIDINPUTCATEGORY);
                } else {
                    slotRange = WorkstationInventoryUtils.getAssignedOutputSlots(entity,
                            FluidOutputProcessPartCommonSystem.FLUIDOUTPUTCATEGORY);
                }
            } else {
                // use all slots
                IntegerRange intRange = new IntegerRange();
                intRange.addNumbers(0, fluidInventoryComponent.fluidSlots.size() - 1);
                slotRange = intRange;
            }

            for (Integer slotIndex : slotRange) {
                if (fluidInventoryComponent.fluidSlots.get(slotIndex).exists()) {
                    return slotIndex;
                }
            }

            return Iterables.getFirst(slotRange, null);
        }
        return null;
    }

    public static boolean isTank(EntityRef entity) {
        return entity.hasComponent(FluidInventoryComponent.class);
    }

    public static float getTankEmptyVolume(EntityRef entity, boolean forInput) {
        return getTankTotalVolume(entity, forInput) - getTankFluidVolume(entity, forInput);

    }
}
