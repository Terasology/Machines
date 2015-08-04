package org.terasology.entityNetwork;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockNetworkTest {
    static final String NETWORK_ID = "test";
    private BlockNetwork blockNetwork;
    private TestListener listener;
    private byte allDirections;

    @Before
    public void setup() {
        blockNetwork = new BlockNetwork();
        listener = new TestListener();
        blockNetwork.addTopologyListener(listener);
        blockNetwork.addTopologyListener(new ValidatingListener());
        allDirections = 63;
    }

    private SidedLocationNetworkNode toNode(Vector3i location, byte directions) {
        return new SidedLocationNetworkNode(NETWORK_ID, false, location, directions);
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
        blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(NETWORK_ID, false, location, Side.RIGHT, Side.LEFT));
        blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(NETWORK_ID, false, location, Side.FRONT, Side.BACK));

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
        final SidedLocationNetworkNode leftRight = new SidedLocationNetworkNode(NETWORK_ID, false, location, Side.RIGHT, Side.LEFT);
        blockNetwork.addNetworkingBlock(leftRight);
        final SidedLocationNetworkNode frontBack = new SidedLocationNetworkNode(NETWORK_ID, false, location, Side.FRONT, Side.BACK);
        blockNetwork.addNetworkingBlock(frontBack);

        blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(1, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(1, 0, 0), allDirections));

        assertEquals(1, blockNetwork.getNetworks().size());

        assertEquals(4, blockNetwork.getDistance(leftRight, frontBack));
    }

    @Test
    public void nodesAgreeAboutConnectivity() {
        blockNetwork.addNetworkingBlock(new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0), Side.TOP));
        blockNetwork.addNetworkingBlock(new LocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0)));

        assertEquals(1, blockNetwork.getNetworks().size());

        blockNetwork.addNetworkingBlock(new LocationNetworkNode(NETWORK_ID, false, new Vector3i(0, -1, 0)));

        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void leafNodesConnectToNormalNodes() {
        blockNetwork.addNetworkingBlock(new LocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0)));
        blockNetwork.addNetworkingBlock(new LocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 1, 0)));

        assertEquals(1, blockNetwork.getNetworks().size());
    }

    @Test
    public void leafNodesSplitTheNetworkBetweenNormalNodes() {
        blockNetwork.addNetworkingBlock(new LocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0)));
        blockNetwork.addNetworkingBlock(new LocationNetworkNode(NETWORK_ID, true, new Vector3i(0, 1, 0)));
        blockNetwork.addNetworkingBlock(new LocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 2, 0)));

        assertEquals(2, blockNetwork.getNetworks().size());
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
