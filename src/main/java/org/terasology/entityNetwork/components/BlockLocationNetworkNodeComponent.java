// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.entityNetwork.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.entityNetwork.BlockLocationNetworkNode;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.NetworkNodeBuilder;

public class BlockLocationNetworkNodeComponent implements Component, NetworkNodeBuilder {
    public String networkId;
    public boolean isLeaf;
    public int maximumGridDistance = 1;

    @Override
    public NetworkNode build(EntityRef entityRef) {
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent != null) {
            return new BlockLocationNetworkNode(networkId, isLeaf, maximumGridDistance, blockComponent.getPosition());
        } else {
            return null;
        }
    }
}
