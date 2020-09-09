// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.world.block.ForceBlockActive;

import java.util.List;
import java.util.Set;

@ForceBlockActive
public class MachineDefinitionComponent implements Component {
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
}
