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
import org.terasology.blockNetwork.Network;
import org.terasology.blockNetwork.NetworkNode;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.registry.In;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.mechanicalPower.components.MechanicalPowerProducerComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;

import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MechanicalPowerAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final long UPDATE_INTERVAL = 1000;
    private static final Logger logger = LoggerFactory.getLogger(MechanicalPowerAuthoritySystem.class);

    @In
    WorldProvider worldProvider;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    MechanicalPowerBlockNetwork mechanicalPowerBlockNetwork;
    @In
    Time time;

    long nextUpdateTime;

    @Override
    public void initialise() {
    }

    @Override
    public void update(float delta) {
        long currentTime = time.getGameTimeInMs();
        if (currentTime > nextUpdateTime) {
            nextUpdateTime = currentTime + UPDATE_INTERVAL;

            for (Network network : mechanicalPowerBlockNetwork.getNetworks()) {

                Set<EntityRef> consumers = Sets.newHashSet();
                Set<EntityRef> producers = Sets.newHashSet();
                // gather the consumers and producers for this network
                for (NetworkNode leafNode : mechanicalPowerBlockNetwork.getNetworkNodes(network)) {
                    EntityRef entity = blockEntityRegistry.getBlockEntityAt(leafNode.location.toVector3i());
                    if (entity.hasComponent(MechanicalPowerConsumerComponent.class)) {
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
                        MechanicalPowerConsumerComponent consumer = consumerEntity.getComponent(MechanicalPowerConsumerComponent.class);
                        if (consumer.currentStoredPower < consumer.maximumStoredPower) {
                            consumer.currentStoredPower = Math.min(consumer.currentStoredPower + powerToEachConsumer, consumer.maximumStoredPower);
                            consumerEntity.saveComponent(consumer);
                        }
                    }
                }
            }
        }
    }
}
