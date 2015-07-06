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
package org.terasology.machines.systems;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.inventory.InventoryAccessComponent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.machines.components.NestedMachineComponent;
import org.terasology.machines.processParts.RequirementInputComponent;
import org.terasology.math.IntegerRange;
import org.terasology.workstation.process.fluid.FluidInputComponent;
import org.terasology.workstation.process.fluid.FluidOutputComponent;
import org.terasology.workstation.process.inventory.InventoryInputComponent;
import org.terasology.workstation.process.inventory.InventoryOutputComponent;
import org.terasology.world.block.BlockComponent;

import java.util.HashMap;

@RegisterSystem
public class MachineCommonSystem extends BaseComponentSystem {

    @ReceiveEvent(components = {BlockComponent.class})
    public void onMachineDefinitionAdded(OnAddedComponent event, EntityRef entity, MachineDefinitionComponent machineDefinition) {
        addProcessingMachine(entity, machineDefinition);
    }

    @ReceiveEvent(components = {NestedMachineComponent.class})
    public void onNestedMachineDefinitionAdded(OnAddedComponent event, EntityRef entity, MachineDefinitionComponent machineDefinition) {
        addProcessingMachine(entity, machineDefinition);
    }

    private void addProcessingMachine(EntityRef entity, MachineDefinitionComponent machineDefinition) {

        // configure the input/output inventories
        if (!entity.hasComponent(InventoryComponent.class)) {
            int totalSlots = machineDefinition.inputSlots + machineDefinition.requirementSlots + machineDefinition.outputSlots;
            InventoryComponent inventoryComponent = new InventoryComponent(totalSlots);
            inventoryComponent.privateToOwner = false;
            entity.addComponent(inventoryComponent);
        }

        // configure the categorized inventory
        if (!entity.hasComponent(InventoryAccessComponent.class)) {
            InventoryAccessComponent categorizedInventory = new InventoryAccessComponent();
            categorizedInventory.input = new HashMap();
            categorizedInventory.output = new HashMap();
            int totalInputSlots = machineDefinition.inputSlots + machineDefinition.requirementSlots;
            categorizedInventory.input.put(InventoryInputComponent.WORKSTATIONINPUTCATEGORY,
                    createSlotRange(0, machineDefinition.inputSlots));
            categorizedInventory.input.put(RequirementInputComponent.REQUIREMENTSINVENTORYCATEGORY,
                    createSlotRange(machineDefinition.inputSlots, machineDefinition.requirementSlots));
            categorizedInventory.output.put(InventoryOutputComponent.WORKSTATIONOUTPUTCATEGORY,
                    createSlotRange(totalInputSlots, machineDefinition.outputSlots));

            // add fluid slot assignments
            categorizedInventory.input.put(FluidInputComponent.FLUIDINPUTCATEGORY,
                    createSlotRange(0, 1));
            categorizedInventory.output.put(FluidOutputComponent.FLUIDOUTPUTCATEGORY,
                    createSlotRange(1, 1));
            entity.addComponent(categorizedInventory);
        }
    }

    IntegerRange createSlotRange(int startIndex, int length) {
        IntegerRange slots = new IntegerRange();
        if (length > 0) {
            slots.addNumbers(startIndex, length + startIndex - 1);
        }
        return slots;
    }
}
