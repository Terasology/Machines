// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.entityNetwork.components;

import org.joml.Vector3i;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.entityNetwork.BlockLocationNetworkNode;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.NetworkNodeBuilder;
import org.terasology.gestalt.entitysystem.component.Component;

public class BlockLocationNetworkNodeComponent implements Component<BlockLocationNetworkNodeComponent>, NetworkNodeBuilder {
    public String networkId;
    public boolean isLeaf;
    public int maximumGridDistance = 1;

    @Override
    public NetworkNode build(EntityRef entityRef) {
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent != null) {
            return new BlockLocationNetworkNode(networkId, isLeaf, maximumGridDistance, blockComponent.getPosition(new Vector3i()));
        } else {
            return null;
        }
    }

    @Override
    public void copy(BlockLocationNetworkNodeComponent other) {
        this.networkId = other.networkId;
        this.isLeaf = other.isLeaf;
        this.maximumGridDistance = other.maximumGridDistance;
    }
}
