/*
 * Copyright 2015 MovingBlocks
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

import org.terasology.RotationUtils;
import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.systems.EntityNetworkManager;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.itemRendering.components.AnimateRotationComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.machines.BlockFamilyUtil;
import org.terasology.math.Roll;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.mechanicalPower.components.MechanicalPowerProducerComponent;
import org.terasology.mechanicalPower.components.RotatingAxleComponent;
import org.terasology.registry.In;
import org.terasology.rendering.nui.layers.ingame.inventory.GetItemTooltip;
import org.terasology.rendering.nui.widgets.TooltipLine;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.items.BlockItemComponent;

@RegisterSystem(RegisterMode.CLIENT)
public class MechanicalPowerClientSystem extends BaseComponentSystem {
    static final float POWERFOR1RPS = 6.0f;

    //@In
    //BlockEntityRegistry blockEntityRegistry;
    @In
    EntityManager entityManager;
    @In
    EntityNetworkManager mechanicalPowerBlockNetwork;

    @ReceiveEvent
    public void createRenderedAxle(OnAddedComponent event, EntityRef entity, RotatingAxleComponent rotatingAxle, LocationComponent location, BlockComponent block) {
        EntityBuilder renderedEntityBuilder = entityManager.newBuilder("RotatingAxle");
        renderedEntityBuilder.setOwner(entity);
        // set the look of the rendered entity
        BlockItemComponent blockItem = renderedEntityBuilder.getComponent(BlockItemComponent.class);
        blockItem.blockFamily = rotatingAxle.renderedBlockFamily;
        renderedEntityBuilder.saveComponent(blockItem);

        // rotate the block so that the rendered entity can be rotated independently while respecting the block placement rotation
        Side direction = BlockFamilyUtil.getSideDefinedDirection(block.getBlock());
        Rotation rotation = RotationUtils.getRotation(direction);
        location.setWorldRotation(rotation.getQuat4f());
        entity.saveComponent(location);

        rotatingAxle.renderedEntity = renderedEntityBuilder.build();
        entity.saveComponent(rotatingAxle);

        for (NetworkNode node : mechanicalPowerBlockNetwork.getNodesForEntity(entity)) {
            for (Network network : mechanicalPowerBlockNetwork.getNetworks(node)) {
                updateAxlesInNetwork(network);
            }
        }
    }

    @ReceiveEvent
    public void removeRenderedAxle(BeforeDeactivateComponent event, EntityRef entityRef, RotatingAxleComponent rotatingAxle) {
        if (rotatingAxle.renderedEntity != null) {
            rotatingAxle.renderedEntity.destroy();
        }
    }

    @ReceiveEvent
    public void updateAxlesInNetwork(OnChangedComponent event, EntityRef entity, MechanicalPowerProducerComponent powerProducer, BlockComponent block) {
        for (NetworkNode node : mechanicalPowerBlockNetwork.getNodesForEntity(entity)) {
            for (Network network : mechanicalPowerBlockNetwork.getNetworks(node)) {
                updateAxlesInNetwork(network);
            }
        }
    }

    private void updateAxlesInNetwork(Network network) {
        if (network != null) {
            float totalPower = 0f;
            int totalConsumers = 0;

            for (NetworkNode node : mechanicalPowerBlockNetwork.getNetworkNodes(network)) {
                EntityRef nodeEntity = mechanicalPowerBlockNetwork.getEntityForNode(node);
                MechanicalPowerProducerComponent producer = nodeEntity.getComponent(MechanicalPowerProducerComponent.class);
                if (producer != null) {
                    totalPower += producer.active ? producer.power : 0f;
                }
                MechanicalPowerConsumerComponent consumer = nodeEntity.getComponent(MechanicalPowerConsumerComponent.class);
                if (consumer != null) {
                    totalConsumers++;
                }

            }

            float speed = 1 / (totalPower / (totalConsumers + 1) / POWERFOR1RPS);
            for (NetworkNode node : mechanicalPowerBlockNetwork.getNetworkNodes(network)) {
                EntityRef nodeEntity = mechanicalPowerBlockNetwork.getEntityForNode(node);

                RotatingAxleComponent rotatingAxle = nodeEntity.getComponent(RotatingAxleComponent.class);
                if (rotatingAxle != null && rotatingAxle.renderedEntity != null) {
                    if (totalPower > 0) {
                        // ensure all axle rotation is turned on
                        turnAxleOn(rotatingAxle.renderedEntity, speed);
                    } else {
                        // ensure all axle rotation is turned off
                        turnAxleOff(rotatingAxle.renderedEntity);
                    }
                }
            }
        }
    }

    private void turnAxleOn(EntityRef renderedEntity, float speed) {
        AnimateRotationComponent animateRotation = renderedEntity.getComponent(AnimateRotationComponent.class);
        if (animateRotation != null) {
            // update the speed if we have already added this component
            if (animateRotation.rollSpeed != speed) {
                animateRotation.rollSpeed = speed;
                renderedEntity.saveComponent(animateRotation);
            }
        } else {

            Rotation targetRotation = Rotation.rotate(Roll.CLOCKWISE_90);

            animateRotation = new AnimateRotationComponent();
            animateRotation.isSynchronized = true;
            animateRotation.rollSpeed = speed;
            renderedEntity.addComponent(animateRotation);
        }
    }

    private void turnAxleOff(EntityRef renderedEntity) {
        renderedEntity.removeComponent(AnimateRotationComponent.class);
    }

    @ReceiveEvent
    public void getItemTooltip(GetItemTooltip event, EntityRef entityRef, MechanicalPowerConsumerComponent consumerComponent) {
        event.getTooltipLines().add(new TooltipLine(String.format("Power: %.0f/%.0f", consumerComponent.currentStoredPower, consumerComponent.maximumStoredPower)));
    }
}
