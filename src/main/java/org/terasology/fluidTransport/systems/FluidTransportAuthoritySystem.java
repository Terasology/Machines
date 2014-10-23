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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.blockNetwork.Network;
import org.terasology.blockNetwork.NetworkNode;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.fluid.component.FluidComponent;
import org.terasology.fluid.system.FluidRegistry;
import org.terasology.fluidTransport.components.FluidPipeComponent;
import org.terasology.fluidTransport.components.FluidPumpComponent;
import org.terasology.fluidTransport.components.FluidTankComponent;
import org.terasology.math.Side;
import org.terasology.math.Vector3i;
import org.terasology.registry.In;
import org.terasology.workstation.component.WorkstationComponent;
import org.terasology.workstation.event.WorkstationStateChanged;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.liquid.LiquidData;

import java.util.Comparator;
import java.util.SortedMap;

@RegisterSystem(RegisterMode.AUTHORITY)
public class FluidTransportAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    public static final long UPDATE_INTERVAL = 1000;
    private static final Logger logger = LoggerFactory.getLogger(FluidTransportAuthoritySystem.class);

    @In
    WorldProvider worldProvider;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    FluidTransportBlockNetwork fluidTransportBlockNetwork;
    @In
    Time time;
    @In
    EntityManager entityManager;
    @In
    FluidRegistry fluidRegistry;

    long nextUpdateTime;

    @Override
    public void initialise() {
    }


    @ReceiveEvent
    public void fluidChangedInMachine(OnChangedComponent event, EntityRef workstation, WorkstationComponent workstationComponent, FluidComponent consumerComponent) {
        workstation.send(new WorkstationStateChanged());
    }

    @ReceiveEvent
    public void pressureChangedInMachine(OnChangedComponent event, EntityRef workstation, WorkstationComponent workstationComponent, FluidTankComponent consumerComponent) {
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
            for (Network network : fluidTransportBlockNetwork.getNetworks()) {

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
                for (NetworkNode leafNode : fluidTransportBlockNetwork.getNetworkNodes(network)) {
                    EntityRef entity = blockEntityRegistry.getBlockEntityAt(leafNode.location.toVector3i());
                    if (entity.hasComponent(FluidTankComponent.class)) {
                        tanksFromBottomUp.put(leafNode.location.y, entity);
                        tanksFromTopDown.put(leafNode.location.y, entity);
                    } else if (entity.hasComponent(FluidPumpComponent.class)) {
                        pumpsFromBottomUp.put(leafNode.location.y, entity);
                    }
                }

                // distribute pump flow starting from the bottom
                for (EntityRef pump : pumpsFromBottomUp.values()) {
                    float pumpWorldPressure = getPumpWorldPressure(pump);
                    float remainingFlow = getPumpFlowAvailable(pump);

                    EntityRef sourceTank = null;
                    String fluidType = null;
                    // check each side for a valid source of liquid
                    for (Side side : Side.values()) {
                        Vector3i sidePosition = side.getAdjacentPos(getLocation(pump));

                        // check for world liquid blocks
                        LiquidData liquidData = worldProvider.getLiquid(sidePosition);
                        if (liquidData.getDepth() > 0) {
                            fluidType = fluidRegistry.getFluidType(liquidData.getType());
                            break;
                        }

                        // check for a tank block
                        EntityRef sideEntity = blockEntityRegistry.getBlockEntityAt(sidePosition);
                        FluidComponent fluidComponent = sideEntity.getComponent(FluidComponent.class);
                        if (fluidComponent != null) {
                            sourceTank = sideEntity;
                            fluidType = fluidComponent.fluidType;
                            break;
                        }
                    }

                    // distribute this fluid
                    for (EntityRef tank : tanksFromBottomUp.values()) {
                        if (getTankElevation(tank) < pumpWorldPressure && remainingFlow > 0) {
                            remainingFlow -= giveFluid(tank, remainingFlow, fluidType);
                        }
                    }
                }

                // distribute gravity flow
                for (EntityRef tank : tanksFromTopDown.values()) {
                    float tankElevation = getTankElevation(tank);
                    float remainingFlow = getTankFlowAvailable(tank);
                    float volumeTransfered = 0;
                    String fluidType = getFluidType(tank);

                    for (EntityRef downstreamTank : tanksFromTopDown.values()) {
                        if (getTankElevation(downstreamTank) < tankElevation && remainingFlow > 0) {
                            volumeTransfered += giveFluid(downstreamTank, remainingFlow, fluidType);
                        }
                    }

                    takeFluid(tank, volumeTransfered);
                }
            }
        }
    }

    private float getTankFlowAvailable(EntityRef tank) {
        float totalFlow = 0;
        for (Side side : Side.values()) {
            Vector3i sidePosition = side.getAdjacentPos(getLocation(tank));

            // check for a pipe block
            EntityRef sideEntity = blockEntityRegistry.getBlockEntityAt(sidePosition);
            FluidPipeComponent fluidPipeComponent = sideEntity.getComponent(FluidPipeComponent.class);
            if (fluidPipeComponent != null) {
                totalFlow += fluidPipeComponent.maximumFlowRate;
            }
        }

        FluidComponent fluidComponent = tank.getComponent(FluidComponent.class);
        return Math.min(totalFlow, fluidComponent.volume);
    }

    private Vector3i getLocation(EntityRef entity) {
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        return blockComponent.getPosition();
    }

    private void takeFluid(EntityRef entity, float volume) {
        FluidTankComponent fluidTankComponent = entity.getComponent(FluidTankComponent.class);
        FluidComponent fluidComponent = entity.getComponent(FluidComponent.class);
        if (volume > 0) {
            fluidComponent.volume -= volume;
            entity.saveComponent(fluidComponent);
        }
    }


    private float giveFluid(EntityRef entity, float volume, String fluidType) {
        FluidTankComponent fluidTankComponent = entity.getComponent(FluidTankComponent.class);
        FluidComponent fluidComponent = entity.getComponent(FluidComponent.class);

        if (fluidComponent.fluidType == null || fluidComponent.fluidType.equals(fluidType)) {
            float amountToGive = Math.min(volume, fluidTankComponent.maximumVolume - fluidComponent.volume);
            fluidComponent.fluidType = fluidType;
            fluidComponent.volume += amountToGive;
            entity.saveComponent(fluidComponent);
            return amountToGive;
        } else {
            return 0;
        }
    }

    private String getFluidType(EntityRef entity) {
        FluidComponent fluidComponent = entity.getComponent(FluidComponent.class);
        return fluidComponent.fluidType;
    }


    private float getTankElevation(EntityRef entity) {
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        return blockComponent.getPosition().y;
    }

    private float getPumpWorldPressure(EntityRef entity) {
        FluidPumpComponent fluidPumpComponent = entity.getComponent(FluidPumpComponent.class);
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        return (float) blockComponent.getPosition().y + fluidPumpComponent.pressure;
    }

    private float getPumpFlowAvailable(EntityRef entity) {
        FluidPumpComponent fluidPumpComponent = entity.getComponent(FluidPumpComponent.class);
        return fluidPumpComponent.maximumFlowRate;
    }

}
