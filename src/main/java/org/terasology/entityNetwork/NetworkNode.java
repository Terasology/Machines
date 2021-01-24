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
package org.terasology.entityNetwork;

import java.util.Objects;

public class NetworkNode {
    public String networkId;
    public boolean isLeaf;

    public NetworkNode(String networkId, boolean isLeaf) {
        this.networkId = networkId;
        this.isLeaf = isLeaf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NetworkNode)) {
            return false;
        }

        NetworkNode that = (NetworkNode) o;

        if (isLeaf != that.isLeaf) {
            return false;
        }
        return Objects.equals(networkId, that.networkId);
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
