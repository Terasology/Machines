// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.Replicate;

import java.util.List;

public class ProcessRequirementsProviderComponent implements Component {

    @Replicate
    public List<String> requirements = Lists.newArrayList();

    public ProcessRequirementsProviderComponent() {
    }

    public ProcessRequirementsProviderComponent(String... requirements) {
        for (String requirement : requirements) {
            this.requirements.add(requirement);
        }
    }
}
