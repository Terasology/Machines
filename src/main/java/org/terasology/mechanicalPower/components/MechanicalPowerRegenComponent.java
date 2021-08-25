// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.mechanicalPower.components;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

public class MechanicalPowerRegenComponent implements Component<MechanicalPowerRegenComponent> {
    @Replicate
    public float power;

    @Override
    public void copyFrom(MechanicalPowerRegenComponent other) {
        this.power = other.power;
    }
}
