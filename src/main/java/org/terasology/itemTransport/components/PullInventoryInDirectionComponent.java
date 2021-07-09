// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itemTransport.components;

import org.terasology.engine.math.Direction;
import org.terasology.gestalt.entitysystem.component.Component;

public class PullInventoryInDirectionComponent implements Component<PullInventoryInDirectionComponent> {
    public Direction direction = Direction.FORWARD;
}
