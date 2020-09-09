// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itemTransport.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.math.Direction;

public class PullInventoryInDirectionComponent implements Component {
    public Direction direction = Direction.FORWARD;
}
