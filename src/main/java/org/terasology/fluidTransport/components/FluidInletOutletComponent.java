/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.fluidTransport.components;

import org.terasology.entitySystem.Component;
import org.terasology.network.Replicate;
import org.terasology.world.block.ForceBlockActive;

/**
 * Marks a block that also has a FluidInventoryComponent as able to exchange fluid with adjacent liquid blocks.
 */
@ForceBlockActive
public class FluidInletOutletComponent implements Component {
    /** The maximum amount of liquid that can flow into the container, in litres (thousandths of a block) per second. */
    public float inletRate;
    /** The maximum amount of liquid that can flow from the container, in litres (thousandths of a block) per second. */
    public float outletRate;

    public float inletVolume;
    public float outletVolume;
}
