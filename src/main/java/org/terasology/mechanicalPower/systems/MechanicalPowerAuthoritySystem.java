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
package org.terasology.mechanicalPower.systems;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.WorldProvider;
import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.systems.EntityNetworkManager;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.math.TeraMath;
import org.terasology.mechanicalPower.components.MechanicalPowerProducerComponent;
import org.terasology.potentialEnergyDevices.components.PotentialEnergyDeviceComponent;
import org.terasology.workstation.component.WorkstationComponent;
import org.terasology.workstation.event.WorkstationStateChanged;

import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MechanicalPowerAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    public static final String NETWORK_ID = "MechanicalPower:Power";
    private static final long UPDATE_INTERVAL = 1000;
    private static final Logger logger = LoggerFactory.getLogger(MechanicalPowerAuthoritySystem.class);

    @In
    WorldProvider worldProvider;
    @In
    EntityNetworkManager mechanicalPowerBlockNetwork;
    @In
    Time time;
    @In
    EntityManager entityManager;

    long nextUpdateTime;

    @Override
    public void initialise() {
    }

    @Override
    public void update(float delta) {
        long currentTime = time.getGameTimeInMs();
        if (currentTime > nextUpdateTime) {
            nextUpdateTime = currentTime + UPDATE_INTERVAL;


            // add all power distributed through the network
            for (Network network : mechanicalPowerBlockNetwork.getNetworks(NETWORK_ID)) {

                Set<EntityRef> consumers = Sets.newHashSet();
                Set<EntityRef> producers = Sets.newHashSet();
                // gather the consumers and producers for this network
                for (NetworkNode node : mechanicalPowerBlockNetwork.getNetworkNodes(network)) {
                    EntityRef entity = mechanicalPowerBlockNetwork.getEntityForNode(node);
                    if (entity.hasComponent(PotentialEnergyDeviceComponent.class)) {
                        consumers.add(entity);
                    }
                    if (entity.hasComponent(MechanicalPowerProducerComponent.class)) {
                        producers.add(entity);
                    }
                }

                float totalPower = 0;
                for (EntityRef producerEntity : producers) {
                    MechanicalPowerProducerComponent producer = producerEntity.getComponent(MechanicalPowerProducerComponent.class);
                    if (producer.active) {
                        totalPower += producer.power;
                    }
                }

                if (totalPower > 0 && consumers.size() > 0) {
                    float powerToEachConsumer = totalPower / consumers.size();
                    for (EntityRef consumerEntity : consumers) {
                        PotentialEnergyDeviceComponent deviceComponent = consumerEntity.getComponent(PotentialEnergyDeviceComponent.class);
                        if (deviceComponent.currentStoredEnergy < deviceComponent.maximumStoredEnergy) {
                            deviceComponent.currentStoredEnergy = TeraMath.clamp(deviceComponent.currentStoredEnergy + powerToEachConsumer, 0, deviceComponent.maximumStoredEnergy);
                            consumerEntity.saveComponent(deviceComponent);
                        }
                    }
                }
            }
        }
    }

    @ReceiveEvent
    public void powerChangedInMachine(OnChangedComponent event, EntityRef workstation,
                                      WorkstationComponent workstationComponent,
                                      PotentialEnergyDeviceComponent potentialEnergyDeviceComponent) {
        workstation.send(new WorkstationStateChanged());
    }

}
