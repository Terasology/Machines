// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.systems;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.flowingliquids.world.block.LiquidData;
import org.terasology.fluid.component.FluidComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluid.system.FluidManager;
import org.terasology.fluid.system.FluidRegistry;
import org.terasology.fluidTransport.components.FluidInletOutletComponent;
import org.terasology.math.Side;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.blockdata.ExtraBlockDataManager;

@RegisterSystem(value = RegisterMode.AUTHORITY, requiresOptional = {"FlowingLiquids"})
public class FluidInletOutletSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    // This should really be a reference to the value from FluidAuthoritySystem, but that's private and changing it would conflict with pending PRs.
    private static final float FLUID_PER_BLOCK = 1000;
    @In
    EntityManager entityManager;
    @In
    FluidRegistry fluidRegistry;
    @In
    FluidManager fluidManager;
    @In
    WorldProvider worldProvider;

    @In
    private BlockManager blockManager;
    private Block air;

    @In
    private ExtraBlockDataManager extraDataManager;
    private int flowIndex;

    @Override
    public void initialise() {
        air = blockManager.getBlock(BlockManager.AIR_ID);
        flowIndex = extraDataManager.getSlotNumber(LiquidData.EXTRA_DATA_NAME);
    }

    @Override
    public void update(float delta) {
        for (EntityRef machine : entityManager.getEntitiesWith(FluidInletOutletComponent.class, BlockComponent.class, FluidInventoryComponent.class)) {
            Vector3ic pos = machine.getComponent(BlockComponent.class).getPosition();
            FluidInletOutletComponent inletOutlet = machine.getComponent(FluidInletOutletComponent.class);
            FluidInventoryComponent tank = machine.getComponent(FluidInventoryComponent.class);
            inletOutlet.inletVolume = Math.min(FLUID_PER_BLOCK, inletOutlet.inletVolume + delta * inletOutlet.inletRate);
            inletOutlet.outletVolume = Math.min(FLUID_PER_BLOCK, inletOutlet.outletVolume + delta * inletOutlet.outletRate);
            for (int i = 0; i < tank.fluidSlots.size(); i++) {
                float maximumVolume = tank.maximumVolumes.get(i);
                EntityRef fluidSlot = tank.fluidSlots.get(i);
                FluidComponent fluid = fluidSlot.getComponent(FluidComponent.class);
                Block liquid = fluid == null ? null : fluidRegistry.getCorrespondingLiquid(fluid.fluidType);
                float fullness = fluid == null ? 0 : fluid.volume / maximumVolume;
                int internalHeight = (int) (fullness * LiquidData.MAX_HEIGHT);

                if ((fluid == null || fluid.volume < maximumVolume && liquid != null) && inletOutlet.inletVolume > 0) {
                    for (Side side : Side.values()) {
                        if (side == Side.BOTTOM) {
                            continue;
                        }
                        Vector3ic adjPos = side.getAdjacentPos(pos, new Vector3i());
                        Block adjBlock = worldProvider.getBlock(adjPos);
                        if (liquid == null ? adjBlock.isLiquid() : adjBlock == liquid) {
                            byte status = (byte) worldProvider.getExtraData(flowIndex, adjPos);
                            int externalHeight = LiquidData.getHeight(status) - LiquidData.getRate(status);
                            int flowRate = (side == Side.TOP) ? Math.min(externalHeight, LiquidData.MAX_HEIGHT - internalHeight - 1) : (externalHeight - internalHeight) / 2;
                            if (flowRate > 0) {
                                if (flowRate == externalHeight) {
                                    worldProvider.setBlock(adjPos, air);
                                } else {
                                    worldProvider.setExtraData(flowIndex, adjPos, LiquidData.setHeight(status, LiquidData.getHeight(status) - flowRate));
                                }
                                float volume = flowRate * FLUID_PER_BLOCK / LiquidData.MAX_HEIGHT;
                                fluidManager.addFluid(EntityRef.NULL, machine, i, fluidRegistry.getCorrespondingFluid(adjBlock), volume);
                                inletOutlet.inletVolume -= volume;
                                break; // Don't accept liquid from multiple directions at once, for simplicity.
                            }
                        }
                    }
                }

                if (liquid != null && inletOutlet.outletVolume > 0) {
                    for (Side side : Side.values()) {
                        if (side == Side.TOP) {
                            continue;
                        }
                        Vector3ic adjPos = side.getAdjacentPos(pos, new Vector3i());
                        Block adjBlock = worldProvider.getBlock(adjPos);
                        if (adjBlock == air || adjBlock == liquid) {
                            byte status = adjBlock == air ? LiquidData.FULL : (byte) worldProvider.getExtraData(flowIndex, adjPos);
                            int externalHeight = adjBlock == air ? 0 : LiquidData.getHeight(status);
                            int flowRate = (side == Side.BOTTOM) ? Math.min(LiquidData.MAX_HEIGHT - externalHeight, internalHeight - 1) : (internalHeight - externalHeight) / 2;
                            if (flowRate > 0) {
                                if (adjBlock == air) {
                                    worldProvider.setBlock(adjPos, liquid);
                                }
                                worldProvider.setExtraData(flowIndex, adjPos, LiquidData.setHeight(status, externalHeight + flowRate));
                                float volume = flowRate * FLUID_PER_BLOCK / LiquidData.MAX_HEIGHT;
                                fluidManager.removeFluid(EntityRef.NULL, machine, i, fluid.fluidType, volume);
                                inletOutlet.outletVolume -= volume;
                                break; // Don't put liquid in multiple directions at once, for simplicity.
                            }
                        }
                    }
                }
            }
            machine.saveComponent(inletOutlet);
        }
    }
}
