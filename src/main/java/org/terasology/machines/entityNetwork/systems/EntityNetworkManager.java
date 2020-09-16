// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.entityNetwork.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.machines.entityNetwork.Network;
import org.terasology.machines.entityNetwork.NetworkNode;

import java.util.Collection;

public interface EntityNetworkManager {
    Collection<NetworkNode> getNetworkNodes(Network network);

    Collection<Network> getNetworks(String networkId);

    EntityRef getEntityForNode(NetworkNode node);

    Collection<NetworkNode> getNodesForEntity(EntityRef entity);

    Collection<Network> getNetworks(NetworkNode node);
}
