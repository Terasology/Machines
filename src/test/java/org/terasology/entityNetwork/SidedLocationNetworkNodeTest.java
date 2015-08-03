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

import org.junit.Before;
import org.junit.Test;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
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
        SidedLocationNetworkNode bottom = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0), allDirections);
        SidedLocationNetworkNode top = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0), allDirections);

        assertTrue(bottom.isConnectedTo(top));
        assertTrue(top.isConnectedTo(bottom));
    }

    @Test
    public void correctConnectionSides() {
        SidedLocationNetworkNode bottom = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0), allDirections);
        SidedLocationNetworkNode top = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0), allDirections);

        assertEquals(Side.TOP, bottom.connectionSide(top));
        assertEquals(Side.BOTTOM, top.connectionSide(bottom));
    }

    @Test
    public void isConnectedToSpecificSides() {
        SidedLocationNetworkNode bottom = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0), SideBitFlag.getSide(Side.TOP));
        SidedLocationNetworkNode top = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0), SideBitFlag.getSide(Side.BOTTOM));
        SidedLocationNetworkNode side = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 1), allDirections);

        assertTrue(bottom.isConnectedTo(top));
        assertTrue(top.isConnectedTo(bottom));
        assertFalse(side.isConnectedTo(bottom));
        assertFalse(bottom.isConnectedTo(side));
    }

    @Test
    public void filterToSpecificSides() {
        SidedLocationNetworkNode bottom = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 0), SideBitFlag.getSide(Side.TOP));
        SidedLocationNetworkNode side = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 0, 1), allDirections);
        SidedLocationNetworkNode top = new SidedLocationNetworkNode(NETWORK_ID, false, new Vector3i(0, 1, 0), SideBitFlag.getSide(Side.BOTTOM));

        BiPredicate<NetworkNode, NetworkNode> filter = SidedLocationNetworkNode.createSideConnectivityFilter(Side.TOP, bottom.location);
        assertTrue(filter.test(top, bottom));

        filter = SidedLocationNetworkNode.createSideConnectivityFilter(Side.TOP, bottom.location);
        assertFalse(filter.test(side, bottom));
    }

}
