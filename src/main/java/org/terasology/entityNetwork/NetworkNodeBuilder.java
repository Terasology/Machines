// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.entityNetwork;

import org.terasology.engine.entitySystem.entity.EntityRef;

public interface NetworkNodeBuilder {
    NetworkNode build(EntityRef entityRef);
}
