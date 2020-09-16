// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.entityNetwork;

import org.terasology.math.geom.Vector3i;

public class BlockLocationNetworkNode extends NetworkNode {
    public final Vector3i location;
    int maximumGridDistance = 1;

    public BlockLocationNetworkNode(String networkId, boolean isLeaf, Vector3i location) {
        super(networkId, isLeaf);
        this.location = location;
    }

    public BlockLocationNetworkNode(String networkId, boolean isLeaf, int maximumGridDistance, Vector3i location) {
        this(networkId, isLeaf, location);
        this.maximumGridDistance = maximumGridDistance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockLocationNetworkNode)) return false;
        if (!super.equals(o)) return false;

        BlockLocationNetworkNode that = (BlockLocationNetworkNode) o;

        return location != null ? location.equals(that.location) : that.location == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (location != null ? location.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return location.toString();
    }

    @Override
    public boolean isConnectedTo(NetworkNode networkNode) {
        if (networkNode == null || !(networkNode instanceof BlockLocationNetworkNode)) return false;
        BlockLocationNetworkNode locationNetworkNode = (BlockLocationNetworkNode) networkNode;
        return locationNetworkNode.location.gridDistance(location) <= maximumGridDistance;
    }
}
