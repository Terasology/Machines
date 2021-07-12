// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.mechanicalPower.processParts;

import org.terasology.gestalt.entitysystem.component.Component;

public class MechanicalPowerInputComponent implements Component<MechanicalPowerInputComponent> {
    public float power;

    @Override
    public void copy(MechanicalPowerInputComponent other) {
        this.power = other.power;
    }
}
