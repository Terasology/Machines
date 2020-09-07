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
package org.terasology.machines.world;


import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.systems.EntityNetworkManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.gestalt.naming.Name;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockSections;
import org.terasology.world.block.family.MultiConnectFamily;
import org.terasology.world.block.family.RegisterBlockFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;

import java.util.List;
import java.util.function.Predicate;

@RegisterBlockFamily("Machines:SameNetworkByBlock")
@BlockSections({"no_connections", "one_connection", "line_connection", "2d_corner", "3d_corner", "2d_t", "cross", "3d_side", "five_connections", "all"})
public class SameNetworkByBlockBlockFamily extends MultiConnectFamily {
    @In
    private EntityNetworkManager entityNetworkManager;

    private Predicate<NetworkNode> nodeFilter;

    public SameNetworkByBlockBlockFamily(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder) {
        this(family, blockBuilder, x -> true);
    }

    protected SameNetworkByBlockBlockFamily(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder, Predicate<NetworkNode> nodeFilter) {
        super(definition, blockBuilder);
        this.nodeFilter = nodeFilter;

        BlockUri blockUri = new BlockUri(definition.getUrn());
        Block block = blockBuilder.constructSimpleBlock(definition, blockUri, this);

        block.setBlockFamily(this);
        block.setUri(new BlockUri(blockUri, new Name(String.valueOf(0))));
        this.blocks.put((byte) 0, block);

        this.registerBlock(blockUri, definition, blockBuilder, "no_connections", (byte) 0, Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "one_connection", SideBitFlag.getSides(Side.BACK), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "line_connection", SideBitFlag.getSides(Side.BACK, Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "2d_corner", SideBitFlag.getSides(Side.LEFT, Side.BACK), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "3d_corner", SideBitFlag.getSides(Side.LEFT, Side.BACK, Side.TOP), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "2d_t", SideBitFlag.getSides(Side.LEFT, Side.BACK, Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "cross", SideBitFlag.getSides(Side.RIGHT, Side.LEFT, Side.BACK, Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "3d_side", SideBitFlag.getSides(Side.LEFT, Side.BACK, Side.FRONT, Side.TOP), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "five_connections", SideBitFlag.getSides(Side.LEFT, Side.BACK, Side.FRONT, Side.TOP, Side.BOTTOM), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "all", (byte) 63, Rotation.allValues());
    }

    @Override
    public byte getConnectionSides() {
        return 63;
    }

    @Override
    public Block getBlockForNeighborUpdate(Vector3i location, Block oldBlock) {
        return super.getBlockForNeighborUpdate(location, oldBlock);
    }

    @Override
    public Block getArchetypeBlock() {
        return blocks.get((byte) 0);
    }

    @Override
    protected boolean connectionCondition(Vector3i blockLocation, Side connectSide) {
        EntityRef thisEntity = blockEntityRegistry.getBlockEntityAt(blockLocation);
        List<Network> thisNetworks = Lists.newArrayList();
        for (NetworkNode networkNode : Iterables.filter(entityNetworkManager.getNodesForEntity(thisEntity), x -> nodeFilter.test(x))) {
            thisNetworks.addAll(entityNetworkManager.getNetworks(networkNode));
        }

        Vector3i neighborLocation = new Vector3i(blockLocation);
        neighborLocation.add(connectSide.getVector3i());
        EntityRef neighborEntity = blockEntityRegistry.getBlockEntityAt(neighborLocation);

        for (NetworkNode neighborNetworkNode : Iterables.filter(entityNetworkManager.getNodesForEntity(neighborEntity), x -> nodeFilter.test(x))) {
            for (Network neighborNetwork : entityNetworkManager.getNetworks(neighborNetworkNode)) {
                if (thisNetworks.contains(neighborNetwork)) {
                    return true;
                }
            }
        }

        return false;
    }
}
