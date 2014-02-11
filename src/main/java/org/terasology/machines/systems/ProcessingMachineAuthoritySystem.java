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

import com.google.common.collect.Lists;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.delay.AddDelayedActionEvent;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.health.DoDestroyEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.PickupBuilder;
import org.terasology.logic.location.LocationComponent;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.ProcessingManager;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.machines.components.DelayedProcessOutputComponent;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.machines.components.ProcessRequirementsProviderComponent;
import org.terasology.machines.components.ProcessingMachineComponent;
import org.terasology.machines.events.ProcessingMachineChanged;
import org.terasology.machines.events.RequestProcessingEvent;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.block.BlockComponent;

import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ProcessingMachineAuthoritySystem implements ComponentSystem {
    public static final String DELAYEDPROCESSOUTPUTACTION = "DelayedProcessOutputAction";
    public static final String CHECKAUTOMATICPROCESSINGACTION = "CheckAutomaticProcessingAction";


    @In
    EntityManager entityManager;
    @In
    Time time;
    @In
    ProcessingManager processingManager;
    @In
    InventoryManager inventoryManager;

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

    @ReceiveEvent
    public void onDelayedActionComplete(DelayedActionTriggeredEvent event, EntityRef entity, DelayedProcessOutputComponent delayedProcessOutputComponent) {
        if (event.getActionId().equals(DELAYEDPROCESSOUTPUTACTION)) {
            entity.removeComponent(DelayedProcessOutputComponent.class);
            processingManager.resolveParts(entity, delayedProcessOutputComponent.process, true, true);
        }
    }

    @ReceiveEvent(components = {LocationComponent.class, BlockComponent.class})
    public void onDelayedProcessOutputAdded(OnAddedComponent event, EntityRef entity, DelayedProcessOutputComponent delayedProcessOutputComponent) {
        entity.send(new AddDelayedActionEvent(DELAYEDPROCESSOUTPUTACTION, delayedProcessOutputComponent.endTime - delayedProcessOutputComponent.startTime));
    }


    @ReceiveEvent
    public void onProcessingMachineChanged(ProcessingMachineChanged event, EntityRef processingMachine, ProcessingMachineComponent processingMachineComponent) {
        if (processingMachineComponent.automaticProcessing
                && !processingMachineComponent.isCurrentlyProcessing
                && !processingMachine.hasComponent(DelayedProcessOutputComponent.class)) {
            // do automatic processing
            processingMachineComponent.isCurrentlyProcessing = true;
            processingManager.performProcessing(processingMachine, processingMachine);
            processingMachineComponent.isCurrentlyProcessing = false;
        }
    }


    @ReceiveEvent(components = {LocationComponent.class, BlockComponent.class})
    public void onMachineDefinitionAdded(OnAddedComponent event, EntityRef entity, MachineDefinitionComponent machineDefinition) {
        addProcessingMachine(entity, machineDefinition);
    }

    private void addProcessingMachine(EntityRef entity, MachineDefinitionComponent machineDefinition) {
        ProcessingMachineComponent processingMachineComponent = new ProcessingMachineComponent();

        // configure the input/output inventories
        if (!entity.hasComponent(InventoryComponent.class)) {
            int totalSlots = machineDefinition.blockInputSlots + machineDefinition.requirementInputSlots + machineDefinition.blockOutputSlots;
            InventoryComponent inventoryComponent = new InventoryComponent(totalSlots);
            inventoryComponent.privateToOwner = false;
            entity.addComponent(inventoryComponent);
        }

        // configure the categorized inventory
        if (!entity.hasComponent(CategorizedInventoryComponent.class)) {
            CategorizedInventoryComponent categorizedInventory = new CategorizedInventoryComponent();
            int totalInputSlots = machineDefinition.blockInputSlots + machineDefinition.requirementInputSlots;
            categorizedInventory.slotMapping.put(CategorizedInventoryComponent.INPUT, createSlotRange(0, machineDefinition.blockInputSlots));
            categorizedInventory.slotMapping.put(CategorizedInventoryComponent.REQUIREMENTS, createSlotRange(machineDefinition.blockInputSlots, machineDefinition.requirementInputSlots));
            categorizedInventory.slotMapping.put(CategorizedInventoryComponent.OUTPUT, createSlotRange(totalInputSlots, machineDefinition.blockOutputSlots));
            entity.addComponent(categorizedInventory);
        }

        // add the requirements provider to the input entity
        entity.addComponent(new ProcessRequirementsProviderComponent(machineDefinition.requirementsProvided.toArray(new String[0])));

        processingMachineComponent.automaticProcessing = machineDefinition.automaticProcessing;
        // add this at the end so we can do some validation eventually
        entity.addComponent(processingMachineComponent);
    }

    List<Integer> createSlotRange(int startIndex, int length) {
        List<Integer> slots = Lists.newArrayList();
        for (int i = 0; i < length; i++) {
            slots.add(startIndex + i);
        }

        return slots;
    }

    @ReceiveEvent
    public void onMachineDestroyed(DoDestroyEvent event, EntityRef entity, ProcessingMachineComponent processingMachineComponent, LocationComponent locationComponent) {
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
            ExtendedInventoryManager.dropItem(item, locationComponent.getWorldPosition());
        }
    }

    @ReceiveEvent
    public void onRequestProcessing(RequestProcessingEvent event, EntityRef processingMachine, ProcessingMachineComponent processingMachineComponent) {
        processingManager.performProcessing(processingMachine, processingMachine);
    }
}
