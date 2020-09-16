// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.entityNetwork;

import org.junit.Before;
import org.junit.Test;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;

import java.util.function.BiPredicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SidedLocationNetworkNodeTest {
    static final String NETWORK_ID = "test";
    private byte allDirections;

    @Before
    public void setup() {
        allDirections = 63;
    }

    @Test
    public void isConnectedToAllSides() {
        SidedBlockLocationNetworkNode bottom = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0
                , 0), allDirections);
        SidedBlockLocationNetworkNode top = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1,
                0), allDirections);

        assertTrue(bottom.isConnectedTo(top));
        assertTrue(top.isConnectedTo(bottom));
    }

    @Test
    public void correctConnectionSides() {
        SidedBlockLocationNetworkNode bottom = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0
                , 0), allDirections);
        SidedBlockLocationNetworkNode top = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1,
                0), allDirections);

        assertEquals(Side.TOP, bottom.connectionSide(top));
        assertEquals(Side.BOTTOM, top.connectionSide(bottom));
    }

    @Test
    public void isConnectedToSpecificSides() {
        SidedBlockLocationNetworkNode bottom = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0
                , 0), SideBitFlag.getSide(Side.TOP));
        SidedBlockLocationNetworkNode top = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1,
                0), SideBitFlag.getSide(Side.BOTTOM));
        SidedBlockLocationNetworkNode side = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0,
                1), allDirections);

        assertTrue(bottom.isConnectedTo(top));
        assertTrue(top.isConnectedTo(bottom));
        assertFalse(side.isConnectedTo(bottom));
        assertFalse(bottom.isConnectedTo(side));
    }

    @Test
    public void connectsToLocationNode() {
        SidedBlockLocationNetworkNode bottom = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0
                , 0), SideBitFlag.getSide(Side.TOP));
        BlockLocationNetworkNode top = new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0));

        assertTrue(bottom.isConnectedTo(top));
        assertTrue(top.isConnectedTo(bottom));
    }

    @Test
    public void avoidsConnectionFromLocationNode() {
        SidedBlockLocationNetworkNode bottom = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0
                , 0), SideBitFlag.getSide(Side.BOTTOM));
        BlockLocationNetworkNode top = new BlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0));

        assertFalse(bottom.isConnectedTo(top));
        // this will still be connected because it is agnostic of the sidelocation
        assertTrue(top.isConnectedTo(bottom));
    }

    @Test
    public void filterToSpecificSides() {
        SidedBlockLocationNetworkNode bottom = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0
                , 0), SideBitFlag.getSide(Side.TOP));
        SidedBlockLocationNetworkNode side = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0,
                1), allDirections);
        SidedBlockLocationNetworkNode top = new SidedBlockLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1,
                0), SideBitFlag.getSide(Side.BOTTOM));

        BiPredicate<NetworkNode, NetworkNode> filter =
                SidedBlockLocationNetworkNode.createSideConnectivityFilter(Side.TOP, bottom.location);
        assertTrue(filter.test(top, bottom));

        filter = SidedBlockLocationNetworkNode.createSideConnectivityFilter(Side.TOP, bottom.location);
        assertFalse(filter.test(side, bottom));
    }

}
