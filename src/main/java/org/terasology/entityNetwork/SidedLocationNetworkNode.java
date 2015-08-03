package org.terasology.entityNetwork;

import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;

import java.util.function.BiPredicate;

public class SidedLocationNetworkNode extends LocationNetworkNode {
    public final byte connectionSides;

    public SidedLocationNetworkNode(String networkId, boolean isLeaf, Vector3i location, byte connectionSides) {
        super(networkId, isLeaf, location);
        this.connectionSides = connectionSides;
    }

    public SidedLocationNetworkNode(String networkId, boolean isLeaf, Vector3i location, Side... sides) {
        this(networkId, isLeaf, location, SideBitFlag.getSides(sides));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SidedLocationNetworkNode)) return false;
        if (!super.equals(o)) return false;

        SidedLocationNetworkNode that = (SidedLocationNetworkNode) o;

        if (connectionSides != that.connectionSides) return false;

        return true;
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
        if (networkNode == null || !(networkNode instanceof SidedLocationNetworkNode)) return false;

        if (super.isConnectedTo(networkNode)) {
            SidedLocationNetworkNode locationNetworkNode = (SidedLocationNetworkNode) networkNode;
            return areConnected(location, connectionSides, locationNetworkNode.location, locationNetworkNode.connectionSides);
        }

        return false;
    }

    private static boolean areConnected(Vector3i lhsLocation, byte lhsSide, Vector3i rhsLocation, byte rhsSide) {
        Vector3i sideVector = new Vector3i(rhsLocation);
        sideVector.sub(lhsLocation);
        Side side = Side.inDirection(sideVector.toVector3f());
        byte sideBit = SideBitFlag.getSide(side);

        return (sideBit & lhsSide) == sideBit && (SideBitFlag.getReverse(sideBit) & rhsSide) == SideBitFlag.getReverse(sideBit);

    }

    public Side connectionSide(SidedLocationNetworkNode node) {
        Vector3i sideVector = new Vector3i(node.location);
        sideVector.sub(location);
        Side side = Side.inDirection(sideVector.toVector3f());
        return side;
    }

    public static BiPredicate<NetworkNode, NetworkNode> createSideConnectivityFilter(Side targetSide, Vector3i targetLocation) {
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
            if (!(lhs instanceof SidedLocationNetworkNode && rhs instanceof SidedLocationNetworkNode)) {
                return false;
            }

            SidedLocationNetworkNode source = (SidedLocationNetworkNode) lhs;
            SidedLocationNetworkNode target = (SidedLocationNetworkNode) rhs;

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
