/*
 * Copyright 2013 MovingBlocks
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
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.entitySystem.Component;
import org.terasology.world.block.ForceBlockActive;

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
