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
package org.terasology.entityNetwork.systems;

import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entitySystem.entity.EntityRef;

import java.util.Collection;

public interface EntityNetworkManager {
    Iterable<NetworkNode> getNetworkNodes(Network network);

    Iterable<Network> getNetworks(String networkId);

    EntityRef getEntityForNode(NetworkNode node);

    Collection<NetworkNode> getNodesForEntity(EntityRef entity);

    Network getNetwork(NetworkNode node);
}
