package org.terasology.entityNetwork.systems;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.SidedBlockLocationNetworkNode;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleNetworkTest {
    static final String NETWORK_ID = "test";
    private BlockNetwork network;
    private byte allDirections;
    private byte upOnly;

    @BeforeAll
    public void setup() {
        network = new BlockNetwork();
        allDirections = 63;
        upOnly = SideBitFlag.addSide((byte) 0, Side.TOP);
    }

    private SidedBlockLocationNetworkNode toNode(Vector3ic location, byte sides) {
        return new SidedBlockLocationNetworkNode(NETWORK_ID, false, location, sides);
    }

    @Test
    public void addNetworkingBlockToEmptyNetwork() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, network.getNetworkSize());
    }

    public static BlockNetwork createDegenerateNetwork(
            NetworkNode networkNode1,
            NetworkNode networkNode2) {
        if (!networkNode1.isConnectedTo(networkNode2)) {
            throw new IllegalArgumentException("These two nodes are not connected");
        }

        BlockNetwork network = new BlockNetwork();
        network.addNetworkingBlock(networkNode1);
        network.addNetworkingBlock(networkNode2);
        return network;
    }


    @Test
    public void creatingDegenerateNetwork() {
        network = createDegenerateNetwork(toNode(new Vector3i(0, 0, 1), allDirections), toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, network.getNetworkSize());
    }

    @Test
    public void cantAddLeafNodeToDegeneratedNetwork() {
        network = createDegenerateNetwork(toNode(new Vector3i(0, 0, 1), allDirections), toNode(new Vector3i(0, 0, 0), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 2), allDirections));
        assertEquals(3, network.getNetworkSize());
        assertEquals(1, network.getNetworks().size());

    }

    @Test
    public void addingNetworkingNodeToNetworkingNode() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));

        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, network.getNetworkSize());
        assertEquals(1, network.getNetworks().size());
    }

    @Test
    public void cantAddNodeToNetworkingNodeTooFar() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 2), allDirections));

        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, network.getNetworks().size());

    }

    @Test
    public void cantAddNodeToNetworkingNodeWrongDirection() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), upOnly));

        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));

        assertEquals(2, network.getNetworks().size());
    }

    @Test
    public void cantAddNodeToNetworkOnTheSideOfConnectedLeaf() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 2), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));

        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(3, network.getNetworkSize());
    }

    @Test
    public void canAddLeafNodeOnTheSideOfConnectedNetworkingNode() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 2), allDirections));

        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(3, network.getNetworkSize());
    }

    @Test
    public void canaddNetworkingBlockOnTheSideOfConnectedNetworkingNode() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 2), allDirections));

        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(3, network.getNetworkSize());
    }

    @Test
    public void removeLeafNodeFromConnectedNetworkWithNetworkingNode() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));

        network.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, network.getNetworkSize());
    }

    @Test
    public void removeLeafNodeFromConnectedNetworkWithOnlyLeafNodes() {
        network = createDegenerateNetwork(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 1), allDirections));

        network.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, network.getNetworkSize());
    }

    @Test
    public void distanceForSameLeafNode() {
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));

        assertTrue(network.isInDistance(0, toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 0), allDirections)));
        assertEquals(0, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 0), allDirections)));
    }

    @Test
    public void distanceForDegeneratedNetwork() {
        network = createDegenerateNetwork(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 1), allDirections));

        assertTrue(network.isInDistance(1, toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 1), allDirections)));
        assertEquals(1, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 1), allDirections)));
    }

    @Test
    public void distanceForTwoLeafNodesOnNetwork() {
        SidedBlockLocationNetworkNode firstLeaf = toNode(new Vector3i(0, 0, 0), allDirections);
        SidedBlockLocationNetworkNode secondLeaf = toNode(new Vector3i(0, 0, 2), allDirections);
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addNetworkingBlock(secondLeaf);
        network.addNetworkingBlock(firstLeaf);

        assertTrue(network.isInDistance(2, firstLeaf, secondLeaf));
        assertFalse(network.isInDistance(1, firstLeaf, secondLeaf));
        assertEquals(2, network.getDistance(firstLeaf, secondLeaf));
    }

    @Test
    public void distanceFromDifferentSides() {
        SidedBlockLocationNetworkNode firstLeaf = toNode(new Vector3i(0, 0, 0), allDirections);
        SidedBlockLocationNetworkNode secondLeaf = toNode(new Vector3i(0, 0, 2), allDirections);
        network.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 1, 1), allDirections));
        network.addNetworkingBlock(toNode(new Vector3i(0, 1, 2), allDirections));
        network.addNetworkingBlock(secondLeaf);
        network.addNetworkingBlock(firstLeaf);

        assertTrue(network.isInDistance(2, firstLeaf, secondLeaf, SidedBlockLocationNetworkNode.createSideConnectivityFilter(Side.FRONT, secondLeaf.location)));
        assertFalse(network.isInDistance(2, firstLeaf, secondLeaf, SidedBlockLocationNetworkNode.createSideConnectivityFilter(Side.TOP, secondLeaf.location)));
        assertFalse(network.isInDistance(3, firstLeaf, secondLeaf, SidedBlockLocationNetworkNode.createSideConnectivityFilter(Side.TOP, secondLeaf.location)));
        assertTrue(network.isInDistance(4, firstLeaf, secondLeaf, SidedBlockLocationNetworkNode.createSideConnectivityFilter(Side.TOP, secondLeaf.location)));
    }

    @Test
    public void distanceForLongNetwork() {
        for (int i = 0; i < 10; i++) {
            network.addNetworkingBlock(toNode(new Vector3i(0, 0, i), allDirections));
        }

        assertTrue(network.isInDistance(9, toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 9), allDirections)));
        assertFalse(network.isInDistance(8, toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 9), allDirections)));
        assertEquals(9, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0, 9), allDirections)));
    }

    @Test
    public void distanceForBranchedNetwork() {
        for (int i = 0; i < 10; i++) {
            network.addNetworkingBlock(toNode(new Vector3i(0, 0, i), allDirections));
        }

        for (int i = 1; i <= 5; i++) {
            network.addNetworkingBlock(toNode(new Vector3i(i, 0, 5), allDirections));
        }

        assertTrue(network.isInDistance(10, toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(5, 0, 5), allDirections)));
        assertFalse(network.isInDistance(9, toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(5, 0, 5), allDirections)));
        assertEquals(10, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(5, 0, 5), allDirections)));

    }
}
