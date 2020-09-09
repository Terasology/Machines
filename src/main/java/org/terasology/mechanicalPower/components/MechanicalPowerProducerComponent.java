// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.mechanicalPower.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;

@ForceBlockActive
public class MechanicalPowerProducerComponent implements Component {
    @Replicate
    public float power;
    @Replicate
    public boolean active;
}
