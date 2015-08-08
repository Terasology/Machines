/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.entityNetwork.components;

import com.google.common.collect.Sets;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.NetworkNodeBuilder;
import org.terasology.entityNetwork.SidedBlockLocationNetworkNode;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.machines.BlockFamilyUtil;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.block.BlockComponent;

import java.util.Set;

public class SidedBlockLocationNetworkNodeComponent implements NetworkNodeBuilder {
    public String networkId;
    public boolean isLeaf;
    public Set<String> directions = Sets.newHashSet();

    @Override
    public NetworkNode build(EntityRef entityRef) {
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent != null) {
            return new SidedBlockLocationNetworkNode(networkId, isLeaf, new Vector3i(blockComponent.getPosition()), calculateConnectionSides(blockComponent));
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
