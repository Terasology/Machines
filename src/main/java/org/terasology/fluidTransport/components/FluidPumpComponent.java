// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;

@ForceBlockActive
public class FluidPumpComponent implements Component {
    @Replicate
    public float pressure;
    public float maximumFlowRate;
}
