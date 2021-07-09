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

    @Replicate
    public List<String> requirements = Lists.newArrayList();

    public ProcessRequirementsProviderFromWorkstationComponent() {
    }

    public ProcessRequirementsProviderFromWorkstationComponent(String... requirements) {
        for (String requirement : requirements) {
            this.requirements.add(requirement);
        }
    }

}
