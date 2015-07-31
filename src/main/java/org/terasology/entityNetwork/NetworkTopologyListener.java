package org.terasology.entityNetwork;

public interface NetworkTopologyListener {
    public void networkAdded(Network network);

    public void networkingNodeAdded(Network network, NetworkNode networkingNode);

    public void networkingNodeRemoved(Network network, NetworkNode networkingNode);

    public void networkRemoved(Network network);
}
