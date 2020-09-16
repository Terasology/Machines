// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.machines.entityNetwork.systems;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.machines.entityNetwork.Network;
import org.terasology.machines.entityNetwork.NetworkNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiPredicate;

class BlockNetwork {
    private static final Logger logger = LoggerFactory.getLogger(BlockNetwork.class);

    private final Map<Network, Set<NetworkNode>> allNetworks = Maps.newHashMap();
    // an adjacency list of nodes connecting to each other
    private final Map<NetworkNode, Set<NetworkNode>> adjacencyList = Maps.newHashMap();

    private final Set<NetworkTopologyListener> listeners = Sets.newLinkedHashSet();

    public void addTopologyListener(NetworkTopologyListener listener) {
        listeners.add(listener);
    }

    public void removeTopologyListener(NetworkTopologyListener listener) {
        listeners.remove(listener);
    }

    public void addNetworkingBlock(NetworkNode networkNode) {
        adjacencyList.put(networkNode, Sets.newHashSet());

        // loop through all the nodes and find connections
        for (NetworkNode existingNode : adjacencyList.keySet()) {
            // ensure that there is mutual agreement between the nodes about a positive connection
            if (!networkNode.equals(existingNode) && networkNode.isConnectedTo(existingNode) && existingNode.isConnectedTo(networkNode)) {
                adjacencyList.get(existingNode).add(networkNode);
                adjacencyList.get(networkNode).add(existingNode);
            }
        }

        addToNetwork(networkNode);
    }

    private void addToNetwork(NetworkNode networkNode) {
        // check the nodes connecting to this one, see if they are all from the same network.  If they are not, merge
        // the networks together
        Set<NetworkNode> connectedNodes = adjacencyList.get(networkNode);
        Network network = null;
        for (NetworkNode connectedNode : Iterables.filter(connectedNodes, x -> !x.isLeaf())) {
            for (Network foundNetwork : getNetworks(connectedNode)) {
                if (foundNetwork == null) {
                    // abort, this should not happen
                    return;
                } else if (network == null) {
                    // this is the first network we found
                    network = foundNetwork;
                } else if (foundNetwork != network) {
                    // connect networks that have now become connected because of this new node
                    mergeNetworks(network, foundNetwork);
                }
            }
        }

        if (network == null) {
            network = new BasicNetwork();
            allNetworks.put(network, Sets.newHashSet());
            notifyNetworkAdded(network);
        }

        allNetworks.get(network).add(networkNode);
        notifyNetworkingNodeAdded(network, networkNode);

        // ensure that all leaf nodes also are added to this network
        for (NetworkNode leafNode : Iterables.filter(connectedNodes, x -> x.isLeaf())) {
            // the special case where a leaf node is being merged into a normal network
            if (adjacencyList.get(leafNode).size() == 1) {
                for (Network leafNetwork : getNetworks(leafNode)) {
                    allNetworks.remove(leafNetwork);
                }
            }
            if (!allNetworks.get(network).contains(leafNode)) {
                allNetworks.get(network).add(leafNode);
                notifyNetworkingNodeAdded(network, leafNode);
            }
        }

    }

    public Collection<Network> getNetworks(NetworkNode networkNode) {
        Set<Network> networks = Sets.newHashSet();
        for (Network network : allNetworks.keySet()) {
            Set<NetworkNode> nodes = allNetworks.get(network);
            if (nodes.contains(networkNode)) {
                networks.add(network);
            }
        }

        return networks;
    }

    private void mergeNetworks(Network target, Network source) {
        Set<NetworkNode> nodesInSource = allNetworks.get(source);
        for (NetworkNode node : nodesInSource) {
            notifyNetworkingNodeRemoved(source, node);
        }
        allNetworks.remove(source);
        notifyNetworkRemoved(source);

        allNetworks.get(target).addAll(nodesInSource);
        for (NetworkNode node : nodesInSource) {
            notifyNetworkingNodeAdded(target, node);
        }
    }

    public void removeNetworkingBlock(NetworkNode networkNode) {
        Set<NetworkNode> connectedNodes = adjacencyList.get(networkNode);

        for (Network originalNetwork : getNetworks(networkNode)) {
            allNetworks.get(originalNetwork).remove(networkNode);
            notifyNetworkingNodeRemoved(originalNetwork, networkNode);

            adjacencyList.remove(networkNode);
            // remove all adjacent links
            for (NetworkNode connectedNode : connectedNodes) {
                adjacencyList.get(connectedNode).remove(networkNode);
            }

            Queue<NetworkNode> needsNewNetwork = Queues.newArrayDeque();
            needsNewNetwork.addAll(connectedNodes);

            // ensure that the network is still intact, if not,  split it up
            // touch all nodes in the starting from each of the connected nodes to the removed node
            // if a nodes is found in a previously touched list, it connects to that network
            Map<NetworkNode, NetworkNode> visitedNodes = Maps.newHashMap(); // where Key = a node in the network,
            // Value = the connected node it originated from
            for (NetworkNode connectedNode : connectedNodes) {
                visitedNodes.put(connectedNode, connectedNode);
            }

            Queue<NetworkNode> currentNodes = Queues.newArrayDeque();
            for (NetworkNode connectedNode : connectedNodes) {
                if (needsNewNetwork.size() == 0) {
                    // we have already verified that nothing needs a new network
                    return;
                }

                currentNodes.add(connectedNode);

                // search through the nodes connecting to this one
                while (currentNodes.size() > 0) {
                    NetworkNode currentNode = currentNodes.poll();
                    if (visitedNodes.containsKey(currentNode) && currentNode != connectedNode) {
                        // we have visited this node already
                        needsNewNetwork.remove(currentNode);
                    } else {
                        for (NetworkNode node : adjacencyList.get(currentNode)) {
                            // add all these adjacent nodes to the list of nodes to visit
                            currentNodes.add(node);
                        }
                    }
                    visitedNodes.put(currentNode, connectedNode);
                }
            }

            // reuse the existing network
            needsNewNetwork.poll();
            // create new networks
            for (NetworkNode node : needsNewNetwork) {
                Network newNetwork = new BasicNetwork();
                Set<NetworkNode> newNetworkNodes = Sets.newHashSet();
                allNetworks.put(newNetwork, newNetworkNodes);
                notifyNetworkAdded(newNetwork);
                Set<NetworkNode> originalNetworkNodes = allNetworks.get(originalNetwork);
                for (Map.Entry<NetworkNode, NetworkNode> item : visitedNodes.entrySet()) {
                    if (item.getValue() == node) {
                        originalNetworkNodes.remove(item.getKey());
                        notifyNetworkingNodeRemoved(originalNetwork, item.getKey());
                        newNetworkNodes.add(item.getKey());
                        notifyNetworkingNodeAdded(newNetwork, item.getKey());

                    }
                }

            }

            if (allNetworks.get(originalNetwork).size() == 0) {
                // this network is empty
                allNetworks.remove(originalNetwork);
                notifyNetworkRemoved(originalNetwork);
            }
        }
    }

    public Collection<Network> getNetworks() {
        return Collections.unmodifiableCollection(new ArrayList<>(allNetworks.keySet()));
    }

    private void notifyNetworkAdded(Network network) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkAdded(network);
        }
    }

    private void notifyNetworkRemoved(Network network) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkRemoved(network);
        }
    }

    private void notifyNetworkingNodeAdded(Network network, NetworkNode networkingNode) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkingNodeAdded(network, networkingNode);
        }
    }

    private void notifyNetworkingNodeRemoved(Network network, NetworkNode networkingNode) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkingNodeRemoved(network, networkingNode);
        }
    }

    public Collection<NetworkNode> getNetworkNodes(Network network) {
        return allNetworks.get(network);
    }

    public boolean hasNetworkingNode(Network network, NetworkNode networkNode) {
        return allNetworks.get(network).contains(networkNode);
    }

    public int getNetworkSize() {
        return adjacencyList.size();
    }

    public Iterable<NetworkNode> getAdjacentNodes(NetworkNode node) {
        return adjacencyList.get(node);

    }

    public int getDistance(NetworkNode from, NetworkNode to) {
        return getDistance(from, to, null);
    }

    public int getDistance(NetworkNode from, NetworkNode to, BiPredicate<NetworkNode, NetworkNode> edgeFilter) {
        return getPath(from, to, edgeFilter).size();
    }

    public boolean isInDistance(int distance, NetworkNode from, NetworkNode to) {
        return isInDistance(distance, from, to, null);
    }

    public boolean isInDistance(int distance, NetworkNode from, NetworkNode to,
                                BiPredicate<NetworkNode, NetworkNode> edgeFilter) {
        return getDistance(from, to, edgeFilter) <= distance;
    }

    public List<NetworkNode> getPath(NetworkNode start, NetworkNode end) {
        return getPath(start, end, null);
    }

    /*
     * Further optimizations: there is no heuristic part of this to avoid searching through all nodes
     */
    public List<NetworkNode> getPath(NetworkNode start, NetworkNode end,
                                     BiPredicate<NetworkNode, NetworkNode> edgeFilter) {
        if (start.equals(end)) {
            // we win already
            return Lists.newArrayList();
        }

        Queue<NetworkNode> currentNodes = Queues.newArrayDeque();
        Map<NetworkNode, NetworkNode> cameFrom = Maps.newHashMap();
        Map<NetworkNode, Integer> distances = Maps.newHashMap();
        Set<NetworkNode> visitedNodes = Sets.newHashSet();

        currentNodes.add(start);
        distances.put(start, 0);
        visitedNodes.add(start);
        while (currentNodes.size() > 0) {
            NetworkNode currentNode = currentNodes.poll();

            int currentConnectedDistance = distances.get(currentNode) + 1;
            for (NetworkNode connectedNode : adjacencyList.get(currentNode)) {
                // filter out any undesired edges
                if (edgeFilter != null) {
                    if (!edgeFilter.test(currentNode, connectedNode)) {
                        continue;
                    }
                }

                // update the distance and cameFrom
                if (!distances.containsKey(connectedNode) || distances.get(connectedNode) > currentConnectedDistance) {
                    distances.put(connectedNode, currentConnectedDistance);
                    cameFrom.put(connectedNode, currentNode);
                }
                if (!visitedNodes.contains(connectedNode)) {
                    visitedNodes.add(connectedNode);
                    currentNodes.add(connectedNode);
                }
            }
        }


        List<NetworkNode> path = Lists.newArrayList();
        NetworkNode currentNode = cameFrom.get(end);
        while (currentNode != null) {
            path.add(currentNode);
            currentNode = cameFrom.get(currentNode);
        }
        Collections.reverse(path);

        return path;
    }

    private class BasicNetwork implements Network {
    }

}
