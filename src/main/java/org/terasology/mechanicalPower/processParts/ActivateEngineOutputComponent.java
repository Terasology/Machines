// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.mechanicalPower.processParts;

import org.terasology.gestalt.entitysystem.component.Component;

public class ActivateEngineOutputComponent implements Component<ActivateEngineOutputComponent> {
    public long activateTime;

    @Override
    public void copyFrom(ActivateEngineOutputComponent other) {
        this.activateTime = other.activateTime;
    }
}
