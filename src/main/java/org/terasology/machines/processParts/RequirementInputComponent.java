// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.processParts;

import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.Component;

import java.util.List;

public class RequirementInputComponent implements Component {
    public List<String> requirements = Lists.newArrayList();
}
