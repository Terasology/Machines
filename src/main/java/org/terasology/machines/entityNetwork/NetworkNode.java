// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.entityNetwork;

public class NetworkNode {
    public String networkId;
    public boolean isLeaf;

    public NetworkNode(String networkId, boolean isLeaf) {
        this.networkId = networkId;
        this.isLeaf = isLeaf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkNode)) return false;

        NetworkNode that = (NetworkNode) o;

        if (isLeaf != that.isLeaf) return false;
        return networkId != null ? networkId.equals(that.networkId) : that.networkId == null;
    }

    @Override
    public int hashCode() {
        int result = networkId != null ? networkId.hashCode() : 0;
        result = 31 * result + (isLeaf ? 1 : 0);
        return result;
    }

    public boolean isConnectedTo(NetworkNode networkNode) {
        return networkId.equals(networkNode.networkId);
    }

    public String getNetworkId() {
        return networkId;
    }

    public boolean isLeaf() {
        return isLeaf;
    }
}
