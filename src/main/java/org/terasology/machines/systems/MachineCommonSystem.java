// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.RetainComponentsComponent;
import org.terasology.engine.math.IntegerRange;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.inventory.logic.InventoryAccessComponent;
import org.terasology.inventory.logic.InventoryComponent;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.machines.components.NestedMachineComponent;
import org.terasology.machines.processParts.RequirementInputProcessPartCommonSystem;
import org.terasology.workstation.process.fluid.FluidInputProcessPartCommonSystem;
import org.terasology.workstation.process.fluid.FluidOutputProcessPartCommonSystem;
import org.terasology.workstation.process.inventory.InventoryInputProcessPartCommonSystem;
import org.terasology.workstation.process.inventory.InventoryOutputProcessPartCommonSystem;

import java.util.HashMap;

@RegisterSystem
public class MachineCommonSystem extends BaseComponentSystem {

    @ReceiveEvent(components = {BlockComponent.class})
    public void onMachineDefinitionAdded(OnAddedComponent event, EntityRef entity,
                                         MachineDefinitionComponent machineDefinition) {
        addProcessingMachine(entity, machineDefinition);
    }

    @ReceiveEvent(components = {NestedMachineComponent.class})
    public void onNestedMachineDefinitionAdded(OnAddedComponent event, EntityRef entity,
                                               MachineDefinitionComponent machineDefinition) {
        addProcessingMachine(entity, machineDefinition);
    }

    private void addProcessingMachine(EntityRef entity, MachineDefinitionComponent machineDefinition) {
        RetainComponentsComponent retainComponents = new RetainComponentsComponent();

        // configure the input/output inventories
        if (!entity.hasComponent(InventoryComponent.class) && machineDefinition.inputSlots + machineDefinition.requirementSlots + machineDefinition.outputSlots > 0) {
            retainComponents.components.add(InventoryComponent.class);
            int totalSlots =
                    machineDefinition.inputSlots + machineDefinition.requirementSlots + machineDefinition.outputSlots;
            InventoryComponent inventoryComponent = new InventoryComponent(totalSlots);
            inventoryComponent.privateToOwner = false;
            entity.addComponent(inventoryComponent);
        }

        // configure the fluid inventories
        if (!entity.hasComponent(FluidInventoryComponent.class) && machineDefinition.fluidInputSlotVolumes.size() + machineDefinition.fluidOutputSlotVolumes.size() > 0) {
            retainComponents.components.add(FluidInventoryComponent.class);
            FluidInventoryComponent fluidInventoryComponent = new FluidInventoryComponent();
            for (float volume : machineDefinition.fluidInputSlotVolumes) {
                fluidInventoryComponent.fluidSlots.add(EntityRef.NULL);
                fluidInventoryComponent.maximumVolumes.add(volume);
            }
            for (float volume : machineDefinition.fluidOutputSlotVolumes) {
                fluidInventoryComponent.fluidSlots.add(EntityRef.NULL);
                fluidInventoryComponent.maximumVolumes.add(volume);
            }
            entity.addComponent(fluidInventoryComponent);
        }

        // configure the categorized inventory
        if (!entity.hasComponent(InventoryAccessComponent.class) && (entity.hasComponent(InventoryComponent.class) || entity.hasComponent(FluidInventoryComponent.class))) {
            retainComponents.components.add(InventoryAccessComponent.class);
            InventoryAccessComponent categorizedInventory = new InventoryAccessComponent();
            categorizedInventory.input = new HashMap();
            categorizedInventory.output = new HashMap();
            if (entity.hasComponent(InventoryComponent.class)) {
                int totalInputSlots = machineDefinition.inputSlots + machineDefinition.requirementSlots;
                categorizedInventory.input.put(InventoryInputProcessPartCommonSystem.WORKSTATIONINPUTCATEGORY,
                        createSlotRange(0, machineDefinition.inputSlots));
                categorizedInventory.input.put(RequirementInputProcessPartCommonSystem.REQUIREMENTSINVENTORYCATEGORY,
                        createSlotRange(machineDefinition.inputSlots, machineDefinition.requirementSlots));
                categorizedInventory.output.put(InventoryOutputProcessPartCommonSystem.WORKSTATIONOUTPUTCATEGORY,
                        createSlotRange(totalInputSlots, machineDefinition.outputSlots));
            }

            // add fluid slot assignments
            if (entity.hasComponent(FluidInventoryComponent.class)) {
                if (machineDefinition.fluidInputSlotVolumes.size() > 0) {
                    categorizedInventory.input.put(FluidInputProcessPartCommonSystem.FLUIDINPUTCATEGORY,
                            createSlotRange(0, machineDefinition.fluidInputSlotVolumes.size()));
                }
                if (machineDefinition.fluidOutputSlotVolumes.size() > 0) {
                    categorizedInventory.output.put(FluidOutputProcessPartCommonSystem.FLUIDOUTPUTCATEGORY,
                            createSlotRange(machineDefinition.fluidInputSlotVolumes.size(),
                                    machineDefinition.fluidOutputSlotVolumes.size()));
                }
            }

            entity.addComponent(categorizedInventory);
        }
        entity.addOrSaveComponent(retainComponents);
    }

    IntegerRange createSlotRange(int startIndex, int length) {
        IntegerRange slots = new IntegerRange();
        if (length > 0) {
            slots.addNumbers(startIndex, length + startIndex - 1);
        }
        return slots;
    }
}
