// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.components;

import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Marks a block that also has a FluidInventoryComponent as able to exchange fluid with adjacent liquid blocks.
 */
@ForceBlockActive
public class FluidInletOutletComponent implements Component<FluidInletOutletComponent> {
    /** The maximum amount of liquid that can flow into the container, in litres (thousandths of a block) per second. */
    public float inletRate;
    /** The maximum amount of liquid that can flow from the container, in litres (thousandths of a block) per second. */
    public float outletRate;

    public float inletVolume;
    public float outletVolume;
}
