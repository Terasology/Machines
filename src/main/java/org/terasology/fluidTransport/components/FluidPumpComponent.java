// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.components;

import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

@ForceBlockActive
public class FluidPumpComponent implements Component<FluidPumpComponent> {
    @Replicate
    public float pressure;
    public float maximumFlowRate;

    @Override
    public void copyFrom(FluidPumpComponent other) {
        this.pressure = other.pressure;
        this.maximumFlowRate = other.maximumFlowRate;
    }
}
