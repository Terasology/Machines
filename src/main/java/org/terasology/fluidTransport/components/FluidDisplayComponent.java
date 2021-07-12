// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.components;

import org.terasology.engine.entitySystem.Owns;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.component.Component;

public class FluidDisplayComponent implements Component<FluidDisplayComponent> {
    @Owns
    public EntityRef renderedEntity;

    @Override
    public void copy(FluidDisplayComponent other) {

    }
}
