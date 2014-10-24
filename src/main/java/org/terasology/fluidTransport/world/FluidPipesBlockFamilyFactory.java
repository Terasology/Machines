/*
 * Copyright 2014 MovingBlocks
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


import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.fluidTransport.components.FluidTransportBlockNetworkComponent;
import org.terasology.fluidTransport.systems.ExtendedFluidManager;
import org.terasology.math.Side;
import org.terasology.math.Vector3i;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.ConnectionCondition;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;
import org.terasology.world.block.family.UpdatesWithNeighboursFamilyFactory;

@RegisterBlockFamilyFactory("Machines:fluidPipe")
public class FluidPipesBlockFamilyFactory extends UpdatesWithNeighboursFamilyFactory {
    public FluidPipesBlockFamilyFactory() {
        super(new FluidPipesConnectionCondition(), (byte) 63);
    }

    private static class FluidPipesConnectionCondition implements ConnectionCondition {
        @Override
        public boolean isConnectingTo(Vector3i blockLocation, Side connectSide, WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry) {
            Vector3i neighborLocation = new Vector3i(blockLocation);
            neighborLocation.add(connectSide.getVector3i());

            EntityRef neighborEntity = blockEntityRegistry.getExistingEntityAt(neighborLocation);
            return neighborEntity != null && connectsToNeighbor(neighborEntity);
        }

        private boolean connectsToNeighbor(EntityRef neighborEntity) {
            BlockComponent blockComponent = neighborEntity.getComponent(BlockComponent.class);
            return neighborEntity.hasComponent(FluidTransportBlockNetworkComponent.class)
                    || (blockComponent != null && blockComponent.getBlock().isLiquid())
                    || ExtendedFluidManager.isTank(neighborEntity);
        }
    }
}