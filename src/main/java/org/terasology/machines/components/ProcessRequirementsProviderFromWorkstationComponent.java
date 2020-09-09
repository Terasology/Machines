// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;

import java.util.List;

@ForceBlockActive
public class ProcessRequirementsProviderFromWorkstationComponent implements Component {

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
