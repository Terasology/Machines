// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.components;

import org.terasology.engine.entitySystem.Owns;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.component.EmptyComponent;

public class FluidDisplayComponent extends EmptyComponent<FluidDisplayComponent> {
    @Owns
    public EntityRef renderedEntity;
}
