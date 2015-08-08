package org.terasology.entityNetwork.systems;

import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;

interface NetworkTopologyListener {
    public void networkAdded(Network network);

    public void networkingNodeAdded(Network network, NetworkNode networkingNode);

    public void networkingNodeRemoved(Network network, NetworkNode networkingNode);

    public void networkRemoved(Network network);
}
