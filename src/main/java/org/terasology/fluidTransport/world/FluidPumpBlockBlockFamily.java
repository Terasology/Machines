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
package org.terasology.fluidTransport.world;


import org.joml.Vector3ic;
import org.terasology.engine.math.Side;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockBuilderHelper;
import org.terasology.engine.world.block.family.RegisterBlockFamily;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.fluidTransport.systems.FluidTransportAuthoritySystem;
import org.terasology.machines.world.SameNetworkByBlockBlockFamily;

@RegisterBlockFamily("FluidTransport:FluidPumpBlock")
public class FluidPumpBlockBlockFamily extends SameNetworkByBlockBlockFamily {
    @In
    WorldProvider worldProvider;

    public FluidPumpBlockBlockFamily(BlockFamilyDefinition family, BlockBuilderHelper builderHelper) {
        super(family, builderHelper, x -> x.getNetworkId().equals(FluidTransportAuthoritySystem.NETWORK_ID));
    }

    @Override
    protected boolean connectionCondition(Vector3ic blockLocation, Side connectSide) {
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
