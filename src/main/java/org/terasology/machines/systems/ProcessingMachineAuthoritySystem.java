/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.machines.systems;

import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.health.DoDestroyEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.PickupBuilder;
import org.terasology.logic.inventory.SlotBasedInventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.ProcessingManager;
import org.terasology.machines.components.DelayedProcessOutputComponent;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.machines.components.ProcessRequirementsProviderComponent;
import org.terasology.machines.components.ProcessingMachineComponent;
import org.terasology.machines.events.RequestProcessingEvent;
import org.terasology.network.NetworkComponent;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.block.BlockComponent;

import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ProcessingMachineAuthoritySystem implements UpdateSubscriberSystem {
    public static final long UPDATE_INTERVAL = 500;

    @In
    EntityManager entityManager;
    @In
    Time time;
    @In
    ProcessingManager processingManager;
    @In
    SlotBasedInventoryManager inventoryManager;

    long nextUpdateTime;
    Random random;
    PickupBuilder pickupBuilder;

    @Override
    public void initialise() {
        random = new FastRandom();
        pickupBuilder = new PickupBuilder();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void update(float delta) {
        long currentTime = time.getGameTimeInMs();
        if (currentTime > nextUpdateTime) {
            nextUpdateTime = currentTime + UPDATE_INTERVAL;

            // finish the processing if it has had sufficient time
            for (EntityRef entity : entityManager.getEntitiesWith(DelayedProcessOutputComponent.class)) {
                DelayedProcessOutputComponent delayedOutput = entity.getComponent(DelayedProcessOutputComponent.class);

                long gameTime = time.getGameTimeInMs();
                if (gameTime > delayedOutput.endTime) {
                    entity.removeComponent(DelayedProcessOutputComponent.class);
                    processingManager.resolveParts(entity, delayedOutput.process, true, true);
                }
            }

            // check for automatic machines
            for (EntityRef entity : entityManager.getEntitiesWith(ProcessingMachineComponent.class, BlockComponent.class, LocationComponent.class)) {
                ProcessingMachineComponent processingMachineComponent = entity.getComponent(ProcessingMachineComponent.class);
                // only automatically process if it does not have a delayed output
                if (processingMachineComponent.automaticProcessing && !processingMachineComponent.outputEntity.hasComponent(DelayedProcessOutputComponent.class)) {
                    // try and execute processing
                    processingManager.performProcessing(processingMachineComponent.inputEntity, processingMachineComponent.outputEntity);
                }
            }
        }
    }


    @ReceiveEvent(components = {LocationComponent.class, BlockComponent.class})
    public void onMachineDefinitionAdded(OnAddedComponent event, EntityRef entity, MachineDefinitionComponent machineDefinition) {
        addProcessingMachine(entity, machineDefinition);
    }

    private void addProcessingMachine(EntityRef entity, MachineDefinitionComponent machineDefinition) {
        ProcessingMachineComponent processingMachineComponent = new ProcessingMachineComponent();

        // create/link the input
        if (machineDefinition.inputEntityType.equalsIgnoreCase("STANDARD")) {
            EntityRef newEntity = entityManager.create(new NetworkComponent());
            processingMachineComponent.inputEntity = newEntity;
            processingMachineComponent.ownedInputEntity = newEntity;
        } else if (machineDefinition.inputEntityType.equalsIgnoreCase("SELF")) {
            processingMachineComponent.inputEntity = entity;
        }
        processingMachineComponent.inputEntity.addComponent(new InventoryComponent(machineDefinition.blockInputSlots + machineDefinition.requirementInputSlots));


        // add the requirements provider to the input entity
        processingMachineComponent.inputEntity.addComponent(new ProcessRequirementsProviderComponent(machineDefinition.requirementsProvided.toArray(new String[0])));

        // create/link the output
        if (machineDefinition.outputEntityType.equalsIgnoreCase("STANDARD")) {
            EntityRef newEntity = entityManager.create(new NetworkComponent());
            processingMachineComponent.outputEntity = newEntity;
            processingMachineComponent.ownedOutputEntity = newEntity;
        } else if (machineDefinition.outputEntityType.equalsIgnoreCase("SELF")) {
            processingMachineComponent.outputEntity = entity;
        }
        processingMachineComponent.outputEntity.addComponent(new InventoryComponent(machineDefinition.blockOutputSlots));
        processingMachineComponent.automaticProcessing = machineDefinition.automaticProcessing;

        entity.addComponent(processingMachineComponent);
    }

    @ReceiveEvent
    public void onMachineDestroyed(DoDestroyEvent event, EntityRef entity, ProcessingMachineComponent processingMachineComponent, LocationComponent locationComponent) {
        dropInventory(processingMachineComponent.inputEntity, locationComponent.getWorldPosition());
        dropInventory(processingMachineComponent.outputEntity, locationComponent.getWorldPosition());
    }

    private void dropInventory(EntityRef entity, Vector3f location) {
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
            EntityRef pickup = pickupBuilder.createPickupFor(item, location, 200, true);
            pickup.send(new ImpulseEvent(random.nextVector3f(30.0f)));
        }
    }

    @ReceiveEvent
    public void onRequestProcessing(RequestProcessingEvent event, EntityRef entity, ProcessingMachineComponent processingMachine) {
        processingManager.performProcessing(processingMachine.inputEntity, processingMachine.outputEntity);
    }
}
