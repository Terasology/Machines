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
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.family.ConnectionCondition;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;
import org.terasology.world.block.family.UpdatesWithNeighboursFamilyFactory;

import java.util.List;
import java.util.function.Predicate;

@RegisterBlockFamilyFactory("Machines:SameNetworkByBlock")
public class SameNetworkByBlockBlockFamilyFactory extends UpdatesWithNeighboursFamilyFactory {
    public SameNetworkByBlockBlockFamilyFactory() {
        this(x -> true);
    }

    protected SameNetworkByBlockBlockFamilyFactory(Predicate<NetworkNode> nodeFilter) {
        super(new SameNetworkByBlockConnectionCondition(nodeFilter), (byte) 63);
    }

    private static class SameNetworkByBlockConnectionCondition implements ConnectionCondition {
        Predicate<NetworkNode> nodeFilter;

        public SameNetworkByBlockConnectionCondition(Predicate<NetworkNode> nodeFilter) {
            this.nodeFilter = nodeFilter;
        }

        @Override
        public boolean isConnectingTo(Vector3i blockLocation, Side connectSide, WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry) {
            final EntityNetworkManager entityNetworkManager = CoreRegistry.get(EntityNetworkManager.class);
            EntityRef thisEntity = blockEntityRegistry.getBlockEntityAt(blockLocation);
            List<Network> thisNetworks = Lists.newArrayList(Iterables.transform(Iterables.filter(entityNetworkManager.getNodesForEntity(thisEntity), x -> nodeFilter.test(x)), x -> entityNetworkManager.getNetwork(x)));

            Vector3i neighborLocation = new Vector3i(blockLocation);
            neighborLocation.add(connectSide.getVector3i());
            EntityRef neighborEntity = blockEntityRegistry.getBlockEntityAt(neighborLocation);

            for (Network neighborNetwork : Iterables.transform(Iterables.filter(entityNetworkManager.getNodesForEntity(neighborEntity), x -> nodeFilter.test(x)), x -> entityNetworkManager.getNetwork(x))) {
                if (thisNetworks.contains(neighborNetwork)) {
                    return true;
                }
            }

            return false;
        }
    }
}
