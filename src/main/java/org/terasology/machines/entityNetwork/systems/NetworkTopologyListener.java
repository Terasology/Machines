// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.machines.entityNetwork.systems;

import org.terasology.machines.entityNetwork.Network;
import org.terasology.machines.entityNetwork.NetworkNode;

interface NetworkTopologyListener {
    void networkAdded(Network network);

    void networkingNodeAdded(Network network, NetworkNode networkingNode);

    void networkingNodeRemoved(Network network, NetworkNode networkingNode);

    void networkRemoved(Network network);
}
