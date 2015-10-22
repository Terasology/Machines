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
package org.terasology.mechanicalPower.components;

import org.terasology.entitySystem.Component;
import org.terasology.logic.inventory.ItemDifferentiating;
import org.terasology.network.Replicate;
import org.terasology.world.block.ForceBlockActive;

@ForceBlockActive
@ItemDifferentiating
public class MechanicalPowerConsumerComponent implements Component {
    @Replicate
    public float currentStoredPower;
    @Replicate
    public float maximumStoredPower;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MechanicalPowerConsumerComponent that = (MechanicalPowerConsumerComponent) o;

        if (that.currentStoredPower > 0 || this.currentStoredPower > 0) {
            // never allow items with power to stack
            return false;
        }
        if (Float.compare(that.currentStoredPower, currentStoredPower) != 0) {
            return false;
        }
        if (Float.compare(that.maximumStoredPower, maximumStoredPower) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (currentStoredPower != +0.0f ? Float.floatToIntBits(currentStoredPower) : 0);
        result = 31 * result + (maximumStoredPower != +0.0f ? Float.floatToIntBits(maximumStoredPower) : 0);
        return result;
    }
}
