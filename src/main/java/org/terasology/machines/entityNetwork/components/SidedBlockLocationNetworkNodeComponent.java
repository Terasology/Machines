// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.entityNetwork.components;

import com.google.common.collect.Sets;
import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.Direction;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.machines.BlockFamilyUtil;
import org.terasology.machines.entityNetwork.NetworkNode;
import org.terasology.machines.entityNetwork.NetworkNodeBuilder;
import org.terasology.machines.entityNetwork.SidedBlockLocationNetworkNode;
import org.terasology.math.geom.Vector3i;

import java.util.Set;

public class SidedBlockLocationNetworkNodeComponent implements Component, NetworkNodeBuilder {
    public String networkId;
    public boolean isLeaf;
    public Set<String> directions = Sets.newHashSet();

    @Override
    public NetworkNode build(EntityRef entityRef) {
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent != null) {
            return new SidedBlockLocationNetworkNode(networkId, isLeaf, new Vector3i(blockComponent.getPosition()),
                    calculateConnectionSides(blockComponent));
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
        byte connectionSides = SideBitFlag.getSides(sides);
        return connectionSides;
    }
}
