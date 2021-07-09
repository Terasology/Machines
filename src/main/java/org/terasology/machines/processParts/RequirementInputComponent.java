// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.processParts;

import com.google.common.collect.Lists;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.List;

public class RequirementInputComponent implements Component<RequirementInputComponent> {
    public List<String> requirements = Lists.newArrayList();
}
