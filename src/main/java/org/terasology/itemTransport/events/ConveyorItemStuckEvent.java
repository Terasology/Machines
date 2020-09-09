// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itemTransport.events;

import org.terasology.engine.entitySystem.event.Event;
import org.terasology.math.geom.Vector3i;

public class ConveyorItemStuckEvent implements Event {
    Vector3i targetPosition;

    public ConveyorItemStuckEvent(Vector3i targetPosition) {
        this.targetPosition = targetPosition;
    }

    public Vector3i getTargetPosition() {
        return targetPosition;
    }
}
