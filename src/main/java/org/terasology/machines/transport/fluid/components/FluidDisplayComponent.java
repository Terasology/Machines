// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.transport.fluid.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.Owns;
import org.terasology.engine.entitySystem.entity.EntityRef;

public class FluidDisplayComponent implements Component {
    @Owns
    public EntityRef renderedEntity;
}
