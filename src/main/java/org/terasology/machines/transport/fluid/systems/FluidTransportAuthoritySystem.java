// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.transport.fluid.systems;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Side;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluid.system.FluidManager;
import org.terasology.fluid.system.FluidRegistry;
import org.terasology.machines.entityNetwork.BlockLocationNetworkNode;
import org.terasology.machines.entityNetwork.Network;
import org.terasology.machines.entityNetwork.systems.EntityNetworkManager;
import org.terasology.machines.transport.fluid.components.FluidPipeComponent;
import org.terasology.machines.transport.fluid.components.FluidPumpComponent;
import org.terasology.machines.transport.fluid.components.FluidTankDropsFluidComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.workstation.component.WorkstationComponent;
import org.terasology.workstation.event.WorkstationStateChanged;

import java.util.Comparator;
import java.util.SortedMap;

@RegisterSystem(RegisterMode.AUTHORITY)
public class FluidTransportAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    public static final String NETWORK_ID = "FluidTransport:Fluids";
    public static final long UPDATE_INTERVAL = 1000;
    private static final Logger logger = LoggerFactory.getLogger(FluidTransportAuthoritySystem.class);

    @In
    WorldProvider worldProvider;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    EntityNetworkManager fluidTransportBlockNetwork;
    @In
    Time time;
    @In
    EntityManager entityManager;
    @In
    FluidRegistry fluidRegistry;
    @In
    FluidManager fluidManager;

    long nextUpdateTime;

    @Override
    public void initialise() {
    }

    @ReceiveEvent
    public void pressureChangedInMachine(OnChangedComponent event, EntityRef workstation,
                                         WorkstationComponent workstationComponent,
                                         FluidInventoryComponent fluidInventoryComponent) {
        workstation.send(new WorkstationStateChanged());
    }

    @Override
    public void update(float delta) {
        long currentTime = time.getGameTimeInMs();
        if (currentTime > nextUpdateTime) {
            nextUpdateTime = currentTime + UPDATE_INTERVAL;

            // fluid flows naturally downwards
            // tanks even themselves out if they are on equal elevation
            // pumps create pressure which can move fluid against gravity
            // flow in and out of a tank is restricted to a static rate

            // distribute all fluid through the network
            for (Network network : fluidTransportBlockNetwork.getNetworks(NETWORK_ID)) {

                SortedMap<Integer, EntityRef> tanksFromTopDown = Maps.newTreeMap(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer i1, Integer i2) {
                        // sort descending
                        return i1.compareTo(i2) * -1;
                    }
                });

                SortedMap<Integer, EntityRef> tanksFromBottomUp = Maps.newTreeMap();
                SortedMap<Integer, EntityRef> pumpsFromBottomUp = Maps.newTreeMap();


                // gather the tanks for this network
                for (BlockLocationNetworkNode node :
                        Iterables.filter(fluidTransportBlockNetwork.getNetworkNodes(network),
                                BlockLocationNetworkNode.class)) {
                    EntityRef entity = blockEntityRegistry.getExistingEntityAt(node.location);
                    if (ExtendedFluidManager.isTank(entity)) {
                        tanksFromBottomUp.put(node.location.y, entity);
                        tanksFromTopDown.put(node.location.y, entity);
                    } else if (entity.hasComponent(FluidPumpComponent.class)) {
                        pumpsFromBottomUp.put(node.location.y, entity);
                    }
                }


                // let tanks drop their fluid to a tank below
                for (EntityRef tank : tanksFromBottomUp.values()) {
                    if (tank.hasComponent(FluidTankDropsFluidComponent.class)) {
                        Vector3i tankBelowLocation = Side.BOTTOM.getAdjacentPos(getLocation(tank));
                        EntityRef tankBelow = blockEntityRegistry.getEntityAt(tankBelowLocation);
                        String fluidType = ExtendedFluidManager.getTankFluidType(tank, true);
                        if (tankBelow.hasComponent(FluidInventoryComponent.class)) {
                            float volumeGiven = ExtendedFluidManager.giveFluid(tankBelow,
                                    ExtendedFluidManager.getTankFluidVolume(tank, true), fluidType, true);
                            ExtendedFluidManager.removeFluid(tank, volumeGiven, fluidType);
                        }
                    }
                }

                // distribute gravity flow
                for (EntityRef tank : tanksFromTopDown.values()) {
                    float tankElevation = getTankElevation(tank);
                    float remainingFlow = getTankFlowAvailable(tank, false);
                    float totalVolumeTransfered = 0;
                    String fluidType = ExtendedFluidManager.getTankFluidType(tank, false);

                    for (EntityRef downstreamTank : tanksFromTopDown.values()) {
                        if (!downstreamTank.equals(tank) && getTankElevation(downstreamTank) < tankElevation && remainingFlow > 0) {
                            float volumeTransfered = ExtendedFluidManager.giveFluid(downstreamTank, remainingFlow,
                                    fluidType, true);
                            totalVolumeTransfered += volumeTransfered;
                            remainingFlow -= volumeTransfered;
                        }
                    }

                    ExtendedFluidManager.removeFluid(tank, totalVolumeTransfered, fluidType);
                }

                // distribute pump flow starting from the bottom
                for (EntityRef pump : pumpsFromBottomUp.values()) {
                    float pumpWorldPressure = getPumpWorldPressure(pump);
                    float remainingFlow = getPumpFlowAvailable(pump);
                    float totalVolumeTransfered = 0;

                    EntityRef sourceTank = null;
                    String fluidType = null;

                    // check each side for a valid source of liquid
                    for (Side side : Side.values()) {
                        Vector3i sidePosition = side.getAdjacentPos(getLocation(pump));

                        //TODO: Implement ability to pump from source blocks

                        // check for a tank block
                        EntityRef sideEntity = blockEntityRegistry.getEntityAt(sidePosition);
                        if (ExtendedFluidManager.isTank(sideEntity)) {
                            sourceTank = sideEntity;
                            fluidType = ExtendedFluidManager.getTankFluidType(sideEntity, false);
                            break;
                        }
                    }

                    if (fluidType != null) {
                        // distribute this fluid
                        for (EntityRef tank : tanksFromBottomUp.values()) {
                            if (!tank.equals(sourceTank) && getTankElevation(tank) <= pumpWorldPressure && remainingFlow > 0) {
                                float volumeTransfered = ExtendedFluidManager.giveFluid(tank, remainingFlow,
                                        fluidType, true);
                                remainingFlow -= volumeTransfered;
                                totalVolumeTransfered += volumeTransfered;
                            }
                        }

                        if (sourceTank != null) {
                            ExtendedFluidManager.removeFluid(sourceTank, totalVolumeTransfered, fluidType);
                        }

                        if (totalVolumeTransfered > 0) {
                            FluidPumpComponent fluidPumpComponent = pump.getComponent(FluidPumpComponent.class);
                            fluidPumpComponent.pressure = 0;
                            pump.saveComponent(fluidPumpComponent);
                        }
                    }
                }
            }
        }
    }

    private float getTankFlowAvailable(EntityRef tank, boolean forInput) {
        float totalFlow = 0;
        for (Side side : Side.values()) {
            Vector3i sidePosition = side.getAdjacentPos(getLocation(tank));

            // check for a pipe block
            EntityRef sideEntity = blockEntityRegistry.getEntityAt(sidePosition);
            FluidPipeComponent fluidPipeComponent = sideEntity.getComponent(FluidPipeComponent.class);
            if (fluidPipeComponent != null) {
                totalFlow += fluidPipeComponent.maximumFlowRate;
            }
        }

        return Math.min(totalFlow, ExtendedFluidManager.getTankFluidVolume(tank, forInput));
    }

    private Vector3i getLocation(EntityRef entity) {
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        if (blockComponent != null) {
            return blockComponent.getPosition();
        } else {
            LocationComponent locationComponent = entity.getComponent(LocationComponent.class);
            return new Vector3i(new Vector3f(locationComponent.getWorldPosition()).sub(0.5f, 0.5f, 0.5f));
        }

    }

    private float getTankElevation(EntityRef entity) {
        return getLocation(entity).y;
    }

    private float getPumpWorldPressure(EntityRef entity) {
        FluidPumpComponent fluidPumpComponent = entity.getComponent(FluidPumpComponent.class);
        return (float) getLocation(entity).y + fluidPumpComponent.pressure;
    }

    private float getPumpFlowAvailable(EntityRef entity) {
        FluidPumpComponent fluidPumpComponent = entity.getComponent(FluidPumpComponent.class);
        return fluidPumpComponent.maximumFlowRate;
    }

}
