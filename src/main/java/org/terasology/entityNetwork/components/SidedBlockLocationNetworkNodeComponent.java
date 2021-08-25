// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.entityNetwork.components;

import com.google.common.collect.Sets;
import org.joml.Vector3i;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.Direction;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.NetworkNodeBuilder;
import org.terasology.entityNetwork.SidedBlockLocationNetworkNode;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.machines.BlockFamilyUtil;

import java.util.Set;

public class SidedBlockLocationNetworkNodeComponent implements Component<SidedBlockLocationNetworkNodeComponent>, NetworkNodeBuilder {
    public String networkId;
    public boolean isLeaf;
    public Set<String> directions = Sets.newHashSet();

    @Override
    public NetworkNode build(EntityRef entityRef) {
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent != null) {
            return new SidedBlockLocationNetworkNode(networkId, isLeaf, blockComponent.getPosition(new Vector3i()), calculateConnectionSides(blockComponent));
        } else {
            return null;
        }
    }

    private byte calculateConnectionSides(BlockComponent block) {
        Side blockDirection = BlockFamilyUtil.getSideDefinedDirection(block.getBlock());
        // convert these directions to sides relative to the facing of the block
        Set<Side> sides = Sets.newHashSet();
        for (String directionString : directions) {
            Direction direction = Direction.valueOf(directionString);
            sides.add(blockDirection.getRelativeSide(direction));
        }
        return SideBitFlag.getSides(sides);
    }

    @Override
    public void copyFrom(SidedBlockLocationNetworkNodeComponent other) {
        this.networkId = other.networkId;
        this.isLeaf = other.isLeaf;
        this.directions.clear();
        this.directions.addAll(other.directions);
    }
}
