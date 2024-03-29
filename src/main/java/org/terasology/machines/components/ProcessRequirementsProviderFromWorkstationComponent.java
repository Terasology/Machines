// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.List;

@ForceBlockActive
public class ProcessRequirementsProviderFromWorkstationComponent implements Component<ProcessRequirementsProviderFromWorkstationComponent> {

    public ProcessRequirementsProviderFromWorkstationComponent() {
    }

    @Replicate
    public List<String> requirements = Lists.newArrayList();



    @Override
    public void copyFrom(ProcessRequirementsProviderFromWorkstationComponent other) {
        this.requirements = Lists.newArrayList(other.requirements);
    }
}
