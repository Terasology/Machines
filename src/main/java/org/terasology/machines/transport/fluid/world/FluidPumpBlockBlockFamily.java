// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.transport.fluid.world;


import org.terasology.engine.math.Side;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockBuilderHelper;
import org.terasology.engine.world.block.family.RegisterBlockFamily;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.machines.transport.fluid.systems.FluidTransportAuthoritySystem;
import org.terasology.machines.world.SameNetworkByBlockBlockFamily;
import org.terasology.math.geom.Vector3i;

@RegisterBlockFamily("FluidTransport:FluidPumpBlock")
public class FluidPumpBlockBlockFamily extends SameNetworkByBlockBlockFamily {
    @In
    WorldProvider worldProvider;

    public FluidPumpBlockBlockFamily(BlockFamilyDefinition family, BlockBuilderHelper builderHelper) {
        super(family, builderHelper, x -> x.getNetworkId().equals(FluidTransportAuthoritySystem.NETWORK_ID));
    }

    @Override
    protected boolean connectionCondition(Vector3i blockLocation, Side connectSide) {
        boolean result = super.connectionCondition(blockLocation, connectSide);
        /* Removed as a part of PR MovingBlocks/Terasology
         * TODO: Re-implement connections when the side is a liquid
         */
//        if( !result) {
//            Vector3i targetLocation = new Vector3i(blockLocation);
//            targetLocation.add(connectSide.getVector3i());
//            result = worldProvider.getLiquid(targetLocation).getDepth() > 0;
//        }
        return result;
    }
}
