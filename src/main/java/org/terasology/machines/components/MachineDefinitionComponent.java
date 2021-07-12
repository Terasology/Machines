// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.List;
import java.util.Set;

@ForceBlockActive
public class MachineDefinitionComponent implements Component<MachineDefinitionComponent> {
    public int inputSlots;
    public String inputSlotsTitle = "Input";
    public int requirementSlots;
    public String requirementSlotsTitle = "Requirements";
    public int outputSlots;
    public String outputSlotsTitle = "Output";
    public String actionTitle = "Execute";
    public Set<String> outputWidgets = Sets.newHashSet();
    public Set<String> inputWidgets = Sets.newHashSet();
    public List<Float> fluidInputSlotVolumes = Lists.newLinkedList();
    public List<Float> fluidOutputSlotVolumes = Lists.newLinkedList();

    @Override
    public void copy(MachineDefinitionComponent other) {
            this.inputSlots = other.inputSlots;
            this.inputSlotsTitle = other.inputSlotsTitle;
            this.requirementSlots = other.requirementSlots;
            this.requirementSlotsTitle = other.requirementSlotsTitle;
            this.outputSlots = other.outputSlots;
            this.outputSlotsTitle = other.outputSlotsTitle;
            this.actionTitle = other.actionTitle;
            this.outputWidgets = Sets.newHashSet(other.outputWidgets);
            this.inputWidgets = Sets.newHashSet(other.inputWidgets);
            this.fluidInputSlotVolumes = Lists.newLinkedList(other.fluidInputSlotVolumes);
            this.fluidOutputSlotVolumes = Lists.newLinkedList(other.fluidOutputSlotVolumes);
    }
}
