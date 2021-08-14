// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itemTransport.components;

import org.terasology.engine.math.Direction;
import org.terasology.gestalt.entitysystem.component.Component;

public class PushInventoryInDirectionComponent implements Component<PushInventoryInDirectionComponent> {
    public Direction direction = Direction.BACKWARD;
    public boolean animateMovingItem;
    public long timeToDestination = 2000;
    public long pushFinishTime;

    public PushInventoryInDirectionComponent() {

    }

    @Override
    public void copyFrom(PushInventoryInDirectionComponent other) {
        this.direction = other.direction;
        this.animateMovingItem = other.animateMovingItem;
        this.timeToDestination = other.timeToDestination;
        this.pushFinishTime = other.pushFinishTime;
    }
}
