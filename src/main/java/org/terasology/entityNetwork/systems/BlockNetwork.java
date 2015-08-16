package org.terasology.entityNetwork.systems;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiPredicate;

class BlockNetwork {
    private static final Logger logger = LoggerFactory.getLogger(BlockNetwork.class);

    private Multimap<Network, NetworkNode> allNetworks = HashMultimap.create();
    // an adjacency list of nodes connecting to each other
    private Multimap<NetworkNode, NetworkNode> adjacencyList = HashMultimap.create();
    private Set<NetworkTopologyListener> listeners = Sets.newLinkedHashSet();

    public void addTopologyListener(NetworkTopologyListener listener) {
        listeners.add(listener);
    }

    public void removeTopologyListener(NetworkTopologyListener listener) {
        listeners.remove(listener);
    }

    public void addNetworkingBlock(NetworkNode networkNode) {
        // loop through all the nodes and find connections
        for (NetworkNode existingNode : adjacencyList.keySet()) {
            // ensure that there is mutual agreement between the nodes about a positive connection
            if (networkNode != existingNode && networkNode.isConnectedTo(existingNode) && existingNode.isConnectedTo(networkNode)) {
                adjacencyList.put(existingNode, networkNode);
                adjacencyList.put(networkNode, existingNode);
            }
        }

        addToNetwork(networkNode);
    }

    private void addToNetwork(NetworkNode networkNode) {
        // check the nodes connecting to this one, see if they are all from the same network.  If they are not, merge the networks together
        Collection<NetworkNode> connectedNodes = adjacencyList.get(networkNode);
        Network network = null;
        for (NetworkNode connectedNode : Iterables.filter(connectedNodes, x -> !x.isLeaf())) {
            Network foundNetwork = getNetwork(connectedNode);
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

        if (network == null) {
            network = new BasicNetwork();
            notifyNetworkAdded(network);
        }

        allNetworks.put(network, networkNode);
        notifyNetworkingNodeAdded(network, networkNode);

        // ensure that all leaf nodes also are added to this network
        for (NetworkNode leafNode : Iterables.filter(connectedNodes, x -> x.isLeaf())) {
            // the special case where 2 leaf nodes are together, the first node will have its own network that links to nobody
            if (networkNode.isLeaf() && adjacencyList.get(leafNode).size() == 1) {
                allNetworks.removeAll(getNetwork(leafNode));
            }
            if (!allNetworks.containsEntry(network, leafNode)) {
                allNetworks.put(network, leafNode);
                notifyNetworkingNodeAdded(network, leafNode);
            }
        }

    }

    public Network getNetwork(NetworkNode networkNode) {
        for (Network network : allNetworks.keySet()) {
            Collection<NetworkNode> nodes = allNetworks.get(network);

            for (NetworkNode node : nodes) {
                if (node.equals(networkNode)) {
                    return network;
                }
            }
        }

        return null;
    }

    private void mergeNetworks(Network target, Network source) {
        Collection<NetworkNode> nodesInSource = allNetworks.get(source);
        for (NetworkNode node : nodesInSource) {
            notifyNetworkingNodeRemoved(source, node);
        }
        allNetworks.removeAll(source);
        notifyNetworkRemoved(source);

        allNetworks.get(target).addAll(nodesInSource);
        for (NetworkNode node : nodesInSource) {
            notifyNetworkingNodeAdded(target, node);
        }
    }

    public void removeNetworkingBlock(NetworkNode networkNode) {
        Collection<NetworkNode> connectedNodes = adjacencyList.get(networkNode);

        Network originalNetwork = getNetwork(networkNode);
        allNetworks.get(originalNetwork).remove(networkNode);
        notifyNetworkingNodeRemoved(originalNetwork, networkNode);

        adjacencyList.removeAll(networkNode);
        // remove all adjacent links
        for (NetworkNode connectedNode : connectedNodes) {
            adjacencyList.remove(connectedNode, networkNode);
        }

        Queue<NetworkNode> needsNewNetwork = Queues.newArrayDeque();
        needsNewNetwork.addAll(connectedNodes);

        // ensure that the network is still intact, if not,  split it up
        // touch all nodes in the starting from each of the connected nodes to the removed node
        // if a nodes is found in a previously touched list, it connects to that network
        Map<NetworkNode, NetworkNode> visitedNodes = Maps.newHashMap(); // where Key = a node in the network, Value = the connected node it originated from
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
            allNetworks.get(newNetwork).addAll(newNetworkNodes);
            notifyNetworkAdded(newNetwork);
            Collection<NetworkNode> originalNetworkNodes = allNetworks.get(originalNetwork);
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
            allNetworks.removeAll(originalNetwork);
            notifyNetworkRemoved(originalNetwork);
        }
    }

    public Collection<Network> getNetworks() {
        return Collections.unmodifiableCollection(allNetworks.keySet());
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

    public Iterable<NetworkNode> getNetworkNodes(Network network) {
        return allNetworks.get(network);
    }

    private class BasicNetwork implements Network {
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

    public boolean isInDistance(int distance, NetworkNode from, NetworkNode to, BiPredicate<NetworkNode, NetworkNode> edgeFilter) {
        return getDistance(from, to, edgeFilter) <= distance;
    }

    public List<NetworkNode> getPath(NetworkNode start, NetworkNode end) {
        return getPath(start, end, null);
    }

    /*
     * Further optimizations: there is no heuristic part of this to avoid searching through all nodes
     */
    public List<NetworkNode> getPath(NetworkNode start, NetworkNode end, BiPredicate<NetworkNode, NetworkNode> edgeFilter) {
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

}
