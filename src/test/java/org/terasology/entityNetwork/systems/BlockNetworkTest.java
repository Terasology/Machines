package org.terasology.entityNetwork.systems;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.terasology.entityNetwork.BlockLocationNetworkNode;
import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.SidedBlockLocationNetworkNode;
import org.terasology.math.Side;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class BlockNetworkTest {
    static final String NETWORK_ID = "test";
    private BlockNetwork blockNetwork;
    private TestListener listener;
    private byte allDirections;

    @BeforeAll
    public void setup() {
        blockNetwork = new BlockNetwork();
        listener = new TestListener();
        blockNetwork.addTopologyListener(listener);
        blockNetwork.addTopologyListener(new ValidatingListener());
        allDirections = 63;
    }

    private SidedBlockLocationNetworkNode toNode(Vector3ic location, byte directions) {
        return new SidedBlockLocationNetworkNode(NETWORK_ID, false, location, directions);
    }

    @Test
    public void addAndRemoveNetworkingBlock() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksAdded);

        blockNetwork.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(0, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksRemoved);
    }

    @Test
    public void addTwoNeighbouringNetworkingBlocks() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, listener.networksAdded);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());

        assertEquals(1, listener.networksAdded);
        assertEquals(2, listener.networkingNodesAdded);
    }

    @Test
    public void removingNetworkingNodeSplitsNetworkInTwo() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, -1), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksAdded);

        blockNetwork.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, listener.networksAdded);
        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void removingNetworkingNodeKeepsExistingNetwork() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksAdded);

        blockNetwork.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, listener.networksAdded);
        assertEquals(1, blockNetwork.getNetworks().size());
    }

    @Test
    public void addingNetworkingNodeJoinsExistingNetworks() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, -1), allDirections));
        assertEquals(2, blockNetwork.getNetworks().size());
        assertEquals(2, listener.networksAdded);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        Network network = blockNetwork.getNetworks().iterator().next();
        assertTrue(blockNetwork.hasNetworkingNode(network, toNode(new Vector3i(0, 0, -1), allDirections)));
        assertTrue(blockNetwork.hasNetworkingNode(network, toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(blockNetwork.hasNetworkingNode(network, toNode(new Vector3i(0, 0, 1), allDirections)));
        assertEquals(1, listener.networksRemoved);
    }

    @Test
    public void addTwoOverlappingCrossingNetworkingNodes() {
        Vector3i location = new Vector3i(0, 0, 0);
        blockNetwork.addNetworkingBlock(new SidedBlockLocationNetworkNode(NETWORK_ID, false, location, Side.RIGHT, Side.LEFT));
        blockNetwork.addNetworkingBlock(new SidedBlockLocationNetworkNode(NETWORK_ID, false, location, Side.FRONT, Side.BACK));

        assertEquals(2, blockNetwork.getNetworks().size());
    }

   /* @Test
    public void tryAddingOverlappingConnectionsNetworkingNodes() {
        Vector3i location = new Vector3i(0, 0, 0);
        blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(location, Side.RIGHT, Side.LEFT));
        try {
            blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(location, Side.FRONT, Side.BACK, Side.RIGHT));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException exp) {
            // expected
        }
    } */

    @Test
    public void cablesInTheSameBlockCanConnectAndHaveCorrectDistance() {
        Vector3i location = new Vector3i(0, 0, 0);
        final SidedBlockLocationNetworkNode leftRight = new SidedBlockLocationNetworkNode(NETWORK_ID, false, location, Side.RIGHT, Side.LEFT);
        blockNetwork.addNetworkingBlock(leftRight);
        final SidedBlockLocationNetworkNode frontBack = new SidedBlockLocationNetworkNode(NETWORK_ID, false, location, Side.FRONT, Side.BACK);
        blockNetwork.addNetworkingBlock(frontBack);

        blockNetwork.addNetworkingBlock(new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(1, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(1, 0, 0), allDirections));

        assertEquals(1, blockNetwork.getNetworks().size());

        assertEquals(4, blockNetwork.getDistance(leftRight, frontBack));
    }

    @Test
    public void nodesAgreeAboutConnectivity() {
        blockNetwork.addNetworkingBlock(new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0), Side.TOP));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0)));

        assertEquals(1, blockNetwork.getNetworks().size());

        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, -1, 0)));

        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void leafNodesConnectToNormalNodes() {
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 1, 0)));

        assertEquals(1, blockNetwork.getNetworks().size());
    }

    @Test
    public void leafNodesSplitTheNetworkBetweenNormalNodes() {
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 1, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 2, 0)));

        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void leafNodesConnectToLeafNodes() {
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 0, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 1, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 2, 0)));

        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void leafNodesConnectThroughNormalNodes() {
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 0, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 2, 0)));

        assertEquals(1, blockNetwork.getNetworks().size());
    }

    @Test
    public void normalNodesConnectTwoLeafNodes() {
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 0, 0)));
        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 2, 0)));

        blockNetwork.addNetworkingBlock(new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0)));

        assertEquals(1, blockNetwork.getNetworks().size());
    }

    private class TestListener implements NetworkTopologyListener {
        public int networksAdded;
        public int networksRemoved;
        public int networkingNodesAdded;
        public int networkingNodesRemoved;
        public int leafNodesAdded;
        public int leafNodesRemoved;

        public void reset() {
            networksAdded = 0;
            networksRemoved = 0;
            networkingNodesAdded = 0;
            networkingNodesRemoved = 0;
            leafNodesAdded = 0;
            leafNodesRemoved = 0;
        }

        @Override
        public void networkAdded(Network newNetwork) {
            networksAdded++;
        }

        @Override
        public void networkRemoved(Network network) {
            networksRemoved++;
        }

        @Override
        public void networkingNodeAdded(Network network, NetworkNode networkingNode) {
            networkingNodesAdded++;
        }

        @Override
        public void networkingNodeRemoved(Network network, NetworkNode networkingNode) {
            networkingNodesRemoved++;
        }
    }

    private class ValidatingListener implements NetworkTopologyListener {
        private Set<Network> networks = Sets.newHashSet();
        private Multimap<Network, NetworkNode> localNetworkingNodes = HashMultimap.create();

        @Override
        public void networkAdded(Network network) {
            assertTrue(networks.add(network));
        }

        @Override
        public void networkingNodeAdded(Network network, NetworkNode networkingNode) {
            assertTrue(networks.contains(network));
            assertTrue(localNetworkingNodes.put(network, networkingNode));
        }

        @Override
        public void networkingNodeRemoved(Network network, NetworkNode networkingNode) {
            assertTrue(networks.contains(network));
            assertTrue(localNetworkingNodes.remove(network, networkingNode));
        }

        @Override
        public void networkRemoved(Network network) {
            assertFalse(localNetworkingNodes.containsKey(network));
            assertTrue(networks.remove(network));
        }
    }
}
