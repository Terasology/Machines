// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.world;


import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockBuilderHelper;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.BlockSections;
import org.terasology.engine.world.block.family.MultiConnectFamily;
import org.terasology.engine.world.block.family.RegisterBlockFamily;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.gestalt.naming.Name;
import org.terasology.machines.entityNetwork.Network;
import org.terasology.machines.entityNetwork.NetworkNode;
import org.terasology.machines.entityNetwork.systems.EntityNetworkManager;
import org.terasology.math.geom.Vector3i;

import java.util.List;
import java.util.function.Predicate;

@RegisterBlockFamily("Machines:SameNetworkByBlock")
@BlockSections({"no_connections", "one_connection", "line_connection", "2d_corner", "3d_corner", "2d_t", "cross", 
        "3d_side", "five_connections", "all"})
public class SameNetworkByBlockBlockFamily extends MultiConnectFamily {
    private final Predicate<NetworkNode> nodeFilter;
    @In
    private EntityNetworkManager entityNetworkManager;

    public SameNetworkByBlockBlockFamily(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder) {
        this(family, blockBuilder, x -> true);
    }

    protected SameNetworkByBlockBlockFamily(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder,
                                            Predicate<NetworkNode> nodeFilter) {
        super(definition, blockBuilder);
        this.nodeFilter = nodeFilter;

        BlockUri blockUri = new BlockUri(definition.getUrn());
        Block block = blockBuilder.constructSimpleBlock(definition, blockUri, this);

        block.setBlockFamily(this);
        block.setUri(new BlockUri(blockUri, new Name(String.valueOf(0))));
        this.blocks.put((byte) 0, block);

        this.registerBlock(blockUri, definition, blockBuilder, "no_connections", (byte) 0, Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "one_connection", SideBitFlag.getSides(Side.BACK),
                Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "line_connection", SideBitFlag.getSides(Side.BACK,
                Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "2d_corner", SideBitFlag.getSides(Side.LEFT,
                Side.BACK), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "3d_corner", SideBitFlag.getSides(Side.LEFT, Side.BACK
                , Side.TOP), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "2d_t", SideBitFlag.getSides(Side.LEFT, Side.BACK,
                Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "cross", SideBitFlag.getSides(Side.RIGHT, Side.LEFT,
                Side.BACK, Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "3d_side", SideBitFlag.getSides(Side.LEFT, Side.BACK,
                Side.FRONT, Side.TOP), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, "five_connections", SideBitFlag.getSides(Side.LEFT,
                Side.BACK, Side.FRONT, Side.TOP, Side.BOTTOM), Rotation.allValues());
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
        for (NetworkNode networkNode : Iterables.filter(entityNetworkManager.getNodesForEntity(thisEntity),
                x -> nodeFilter.test(x))) {
            thisNetworks.addAll(entityNetworkManager.getNetworks(networkNode));
        }

        Vector3i neighborLocation = new Vector3i(blockLocation);
        neighborLocation.add(connectSide.getVector3i());
        EntityRef neighborEntity = blockEntityRegistry.getBlockEntityAt(neighborLocation);

        for (NetworkNode neighborNetworkNode :
                Iterables.filter(entityNetworkManager.getNodesForEntity(neighborEntity), x -> nodeFilter.test(x))) {
            for (Network neighborNetwork : entityNetworkManager.getNetworks(neighborNetworkNode)) {
                if (thisNetworks.contains(neighborNetwork)) {
                    return true;
                }
            }
        }

        return false;
    }
}
