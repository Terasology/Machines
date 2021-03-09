package org.terasology.entityNetwork;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;

import java.util.function.BiPredicate;

public class SidedBlockLocationNetworkNode extends BlockLocationNetworkNode {
    public final byte connectionSides;

    public SidedBlockLocationNetworkNode(String networkId, boolean isLeaf, Vector3ic location, byte connectionSides) {
        super(networkId, isLeaf, location);
        this.connectionSides = connectionSides;
    }

    public SidedBlockLocationNetworkNode(String networkId, boolean isLeaf, Vector3ic location, Side... sides) {
        this(networkId, isLeaf, location, SideBitFlag.getSides(sides));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SidedBlockLocationNetworkNode)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        SidedBlockLocationNetworkNode that = (SidedBlockLocationNetworkNode) o;

        return connectionSides == that.connectionSides;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) connectionSides;
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + " " + connectionSides;
    }

    @Override
    public boolean isConnectedTo(NetworkNode networkNode) {
        if (super.isConnectedTo(networkNode)) {
            byte connectionSidesToTest = 63;
            if (networkNode instanceof SidedBlockLocationNetworkNode) {
                connectionSidesToTest = ((SidedBlockLocationNetworkNode) networkNode).connectionSides;
            }
            BlockLocationNetworkNode locationNetworkNode = (BlockLocationNetworkNode) networkNode;
            return areConnected(location, connectionSides, locationNetworkNode.location, connectionSidesToTest);
        }
        return false;
    }

    private static boolean areConnected(Vector3ic lhsLocation, byte lhsSide, Vector3ic rhsLocation, byte rhsSide) {
        Vector3i sideVector = new Vector3i(rhsLocation);
        sideVector.sub(lhsLocation);
        Side side = Side.inDirection(new Vector3f(sideVector));
        byte sideBit = SideBitFlag.getSide(side);

        return (sideBit & lhsSide) == sideBit && (SideBitFlag.getReverse(sideBit) & rhsSide) == SideBitFlag.getReverse(sideBit);

    }

    public Side connectionSide(SidedBlockLocationNetworkNode node) {
        Vector3i sideVector = new Vector3i(node.location);
        sideVector.sub(location);
        Side side = Side.inDirection(new Vector3f(sideVector));
        return side;
    }

    public static BiPredicate<NetworkNode, NetworkNode> createSideConnectivityFilter(Side targetSide,
                                                                                     Vector3i targetLocation) {
        return new SideConnectivityFilter(targetSide, targetLocation);
    }

    private static class SideConnectivityFilter implements BiPredicate<NetworkNode, NetworkNode> {
        final Side targetSide;
        final Vector3i targetLocation;

        public SideConnectivityFilter(Side targetSide, Vector3i targetLocation) {
            this.targetSide = targetSide;
            this.targetLocation = targetLocation;
        }

        @Override
        public boolean test(NetworkNode lhs, NetworkNode rhs) {
            if (!(lhs instanceof SidedBlockLocationNetworkNode && rhs instanceof SidedBlockLocationNetworkNode)) {
                return false;
            }

            SidedBlockLocationNetworkNode source = (SidedBlockLocationNetworkNode) lhs;
            SidedBlockLocationNetworkNode target = (SidedBlockLocationNetworkNode) rhs;

            if (target.location.equals(targetLocation)) {
                byte targetSideBitFlag = SideBitFlag.getSide(targetSide);
                if ((targetSideBitFlag & target.connectionSides) == targetSideBitFlag) {
                    return areConnected(source.location, source.connectionSides, target.location, targetSideBitFlag);
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }
}
