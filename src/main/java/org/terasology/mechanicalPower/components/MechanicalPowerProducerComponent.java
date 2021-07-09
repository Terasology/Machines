// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.mechanicalPower.components;

import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

@ForceBlockActive
public class MechanicalPowerProducerComponent implements Component<MechanicalPowerProducerComponent> {
    @Replicate
    public float power;
    @Replicate
    public boolean active;
}
