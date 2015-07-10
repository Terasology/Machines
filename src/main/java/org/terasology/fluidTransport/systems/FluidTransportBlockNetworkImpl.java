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
package org.terasology.fluidTransport.systems;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.blockNetwork.BlockNetwork;
import org.terasology.blockNetwork.Network;
import org.terasology.blockNetwork.NetworkNode;
import org.terasology.blockNetwork.NetworkTopologyListener;
import org.terasology.blockNetwork.SimpleNetwork;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluidTransport.components.FluidPipeComponent;
import org.terasology.fluidTransport.components.FluidPumpComponent;
import org.terasology.fluidTransport.components.FluidTransportBlockNetworkComponent;
import org.terasology.machines.BlockFamilyUtil;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BeforeDeactivateBlocks;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.OnActivatedBlocks;

import java.util.Map;
import java.util.Set;

@RegisterSystem
@Share(FluidTransportBlockNetwork.class)
public class FluidTransportBlockNetworkImpl extends BaseComponentSystem implements NetworkTopologyListener<NetworkNode>, FluidTransportBlockNetwork {
    private static final Logger logger = LoggerFactory.getLogger(FluidTransportBlockNetworkImpl.class);

    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private WorldProvider worldProvider;

    private BlockNetwork<NetworkNode> blockNetwork;
    private Map<Vector3i, NetworkNode> networkNodes = Maps.newHashMap();
    private Map<Network, FluidTransportNetworkDetails> networks = Maps.newHashMap();

    public FluidTransportBlockNetworkImpl() {
        blockNetwork = new BlockNetwork<>();
        blockNetwork.addTopologyListener(this);
        logger.info("Initialized Fluid Transport System");
    }

    @Override
    public Network getNetwork(Vector3i position) {
        try {
            return blockNetwork.getNetworkWithNetworkingBlock(networkNodes.get(position));
        } catch (IllegalStateException ex) {
            logger.error(ex.getMessage());
            return new SimpleNetwork();
        }
    }

    @Override
    public Iterable<NetworkNode> getNetworkNodes(Network network) {
        Iterable<NetworkNode> nodes = network.getNetworkingNodes();

        return Iterables.filter(nodes, NetworkNode.class);
    }

    @Override
    public Iterable<Network> getNetworks() {
        return Sets.newHashSet(blockNetwork.getNetworks());
    }

    @Override
    public FluidTransportNetworkDetails getMechanicalPowerNetwork(Network network) {
        return networks.get(network);
    }

    @Override
    public void addTopologyListener(NetworkTopologyListener networkTopologyListener) {
        blockNetwork.addTopologyListener(networkTopologyListener);
    }


    private void addNetworkNode(EntityRef entity) {
        NetworkNode networkNode = null;

        FluidTransportBlockNetworkComponent networkItem = entity.getComponent(FluidTransportBlockNetworkComponent.class);

        BlockComponent block = entity.getComponent(BlockComponent.class);
        Vector3i position = block.getPosition();
        // do not add an overlapping node,  this has to do with multiplayer entity creation
        if (!networkNodes.containsKey(position)) {
            byte connectionSides = calculateConnectionSides(networkItem, block);

            if (entity.hasComponent(FluidInventoryComponent.class)) {
                TankNode tankNode = new TankNode(position, connectionSides);
                networkNode = tankNode;
            } else if (entity.hasComponent(FluidPumpComponent.class)) {
                PumpNode pumpNode = new PumpNode(position, connectionSides);
                networkNode = pumpNode;
            } else {
                networkNode = new NetworkNode(position, connectionSides);
            }

            networkNodes.put(position, networkNode);
            try {
                blockNetwork.addNetworkingBlock(networkNode);
            } catch (IllegalStateException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    private byte calculateConnectionSides(FluidTransportBlockNetworkComponent networkItem, BlockComponent block) {
        Side blockDirection = BlockFamilyUtil.getSideDefinedDirection(block.getBlock());
        byte connectionSides = 0;
        if (networkItem.directions.size() == 0) {
            connectionSides = (byte) 63;
        } else {
            // convert these directions to sides relative to the facing of the block
            Set<Side> sides = Sets.newHashSet();
            for (String directionString : networkItem.directions) {
                Direction direction = Direction.valueOf(directionString);
                sides.add(blockDirection.getRelativeSide(direction));
            }

            connectionSides = SideBitFlag.getSides(sides);
        }
        return connectionSides;
    }

    private void removeNetworkNode(Vector3i position) {
        NetworkNode networkNode = networkNodes.get(position);

        try {
            blockNetwork.removeNetworkingBlock(networkNode);
        } catch (IllegalStateException ex) {
            logger.error(ex.getMessage());
        }
        networkNodes.remove(position);
    }

    private void updateNetworkNode(Vector3i position, byte connectionSides) {
        NetworkNode oldNetworkNode = networkNodes.get(position);
        NetworkNode networkNode = new NetworkNode(position, connectionSides);
        try {
            blockNetwork.updateNetworkingBlock(oldNetworkNode, networkNode);
        } catch (IllegalStateException ex) {
            logger.error(ex.getMessage());
        }
        networkNodes.put(position, networkNode);
    }

    //region adding and removing from the block network

    // this event can happen generically because the full entity will be retrieved
    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void createNetworkNodesOnWorldLoad(OnActivatedBlocks event, EntityRef blockType,
                                              FluidTransportBlockNetworkComponent fluidTransportBlockNetworkComponent) {
        for (Vector3i location : event.getBlockPositions()) {
            addNetworkNode(blockEntityRegistry.getBlockEntityAt(location));
        }
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void removeNetworkNodesOnWorldUnload(BeforeDeactivateBlocks event, EntityRef blockType,
                                                FluidTransportBlockNetworkComponent fluidTransportBlockNetworkComponent) {
        for (Vector3i location : event.getBlockPositions()) {
            removeNetworkNode(location);
        }
    }

    // this event can happen generically because we are only updating the connection sides
    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void updateNetworkNode(OnChangedComponent event, EntityRef entity,
                                  FluidTransportBlockNetworkComponent fluidTransportBlockNetworkComponent,
                                  BlockComponent block) {
        byte connectingOnSides = calculateConnectionSides(fluidTransportBlockNetworkComponent, block);
        final Vector3i location = block.getPosition();
        updateNetworkNode(location, connectingOnSides);
    }

    // this event can happen generically because it is position based
    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void removeNetworkNode(BeforeDeactivateComponent event, EntityRef entity,
                                  FluidTransportBlockNetworkComponent fluidTransportBlockNetworkComponent,
                                  BlockComponent block) {
        // only remove a node if the block actually doesnt exist anymore.
        // This is particularly a problem in multiplayer where an entity is created both when the block
        // is placed (causing a default entity to be created) and when the entity from the server is sent.
        if (worldProvider.getBlock(block.getPosition()).getId() != block.getBlock().getId()) {
            removeNetworkNode(block.getPosition());
        }
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void createTankNetworkNode(OnActivatedComponent event,
                                      EntityRef entity,
                                      FluidInventoryComponent type,
                                      FluidTransportBlockNetworkComponent mechanicalPowerBlockNetwork,
                                      BlockComponent block) {
        addNetworkNode(entity);
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void createPipeNetworkNode(OnActivatedComponent event,
                                      EntityRef entity,
                                      FluidPipeComponent type,
                                      FluidTransportBlockNetworkComponent mechanicalPowerBlockNetwork,
                                      BlockComponent block) {
        addNetworkNode(entity);
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void createPumpNetworkNode(OnActivatedComponent event,
                                      EntityRef entity,
                                      FluidPumpComponent type,
                                      FluidTransportBlockNetworkComponent mechanicalPowerBlockNetwork,
                                      BlockComponent block) {
        addNetworkNode(entity);
    }


    private void updateNetworkDetails(Network<NetworkNode> network) {
    }

    @Override
    public void networkAdded(Network<NetworkNode> network) {
        networks.put(network, new FluidTransportNetworkDetails());
    }

    @Override
    public void networkingNodesAdded(Network<NetworkNode> network, Set<NetworkNode> networkingNodes) {
        updateNetworkDetails(network);
    }

    @Override
    public void networkingNodesRemoved(Network<NetworkNode> network, Set<NetworkNode> networkingNodes) {
        updateNetworkDetails(network);
    }

    @Override
    public void leafNodesAdded(Network<NetworkNode> network, Set<NetworkNode> leafNodes) {

    }

    @Override
    public void leafNodesRemoved(Network<NetworkNode> network, Set<NetworkNode> leafNodes) {

    }

    @Override
    public void networkRemoved(Network<NetworkNode> network) {
        networks.remove(network);
    }

    //endregion
}
