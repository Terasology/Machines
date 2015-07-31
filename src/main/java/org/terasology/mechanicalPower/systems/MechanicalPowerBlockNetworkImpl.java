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
package org.terasology.mechanicalPower.systems;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.blockNetwork.SimpleNetwork;
import org.terasology.entityNetwork.BlockNetwork;
import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.NetworkTopologyListener;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.machines.BlockFamilyUtil;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.mechanicalPower.components.MechanicalPowerBlockNetworkComponent;
import org.terasology.mechanicalPower.components.MechanicalPowerConductorComponent;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.mechanicalPower.components.MechanicalPowerProducerComponent;
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
@Share(MechanicalPowerBlockNetwork.class)
public class MechanicalPowerBlockNetworkImpl extends BaseComponentSystem implements NetworkTopologyListener<NetworkNode>, MechanicalPowerBlockNetwork {
    private static final Logger logger = LoggerFactory.getLogger(MechanicalPowerBlockNetworkImpl.class);

    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    WorldProvider worldProvider;

    private BlockNetwork<NetworkNode> blockNetwork;
    private Map<Vector3i, NetworkNode> networkNodes = Maps.newHashMap();
    private Map<Network, MechanicalPowerNetworkDetails> networks = Maps.newHashMap();

    public MechanicalPowerBlockNetworkImpl() {
        blockNetwork = new BlockNetwork<>();
        blockNetwork.addTopologyListener(this);
        logger.info("Initialized Mechanical Power System");
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
    public MechanicalPowerNetworkDetails getMechanicalPowerNetwork(Network network) {
        return networks.get(network);
    }

    @Override
    public void addTopologyListener(NetworkTopologyListener networkTopologyListener) {
        blockNetwork.addTopologyListener(networkTopologyListener);
    }


    private void addNetworkNode(EntityRef entity) {
        NetworkNode networkNode = null;

        MechanicalPowerBlockNetworkComponent networkItem = entity.getComponent(MechanicalPowerBlockNetworkComponent.class);

        BlockComponent block = entity.getComponent(BlockComponent.class);
        Vector3i position = block.getPosition();
        // do not add an overlapping node,  this has to do with multiplayer entity creation
        if (!networkNodes.containsKey(position)) {
            byte connectionSides = calculateConnectionSides(networkItem, block);

            if (entity.hasComponent(MechanicalPowerProducerComponent.class)) {
                MechanicalPowerProducerComponent producer = entity.getComponent(MechanicalPowerProducerComponent.class);
                ProducerNode producerNode = new ProducerNode(position, connectionSides);
                producerNode.power = producer.active ? producer.power : 0;
                networkNode = producerNode;
            } else if (entity.hasComponent(MechanicalPowerConsumerComponent.class)) {
                networkNode = new ConsumerNode(position, connectionSides);
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

    private byte calculateConnectionSides(MechanicalPowerBlockNetworkComponent networkItem, BlockComponent block) {
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
    public void createNetworkNodesOnWorldLoad(OnActivatedBlocks event, EntityRef blockType, MechanicalPowerBlockNetworkComponent mechanicalPowerBlockNetwork) {
        for (Vector3i location : event.getBlockPositions()) {
            addNetworkNode(blockEntityRegistry.getBlockEntityAt(location));
        }
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void removeNetworkNodesOnWorldUnload(BeforeDeactivateBlocks event, EntityRef blockType, MechanicalPowerBlockNetworkComponent mechanicalPowerBlockNetwork) {
        for (Vector3i location : event.getBlockPositions()) {
            removeNetworkNode(location);
        }
    }

    // this event can happen generically because we are only updating the connection sides
    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void updateNetworkNode(OnChangedComponent event, EntityRef entity, MechanicalPowerBlockNetworkComponent mechanicalPowerBlockNetwork, BlockComponent block) {
        byte connectingOnSides = calculateConnectionSides(mechanicalPowerBlockNetwork, block);
        final Vector3i location = block.getPosition();
        updateNetworkNode(location, connectingOnSides);
    }

    // this event can happen generically because it is position based
    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void removeNetworkNode(BeforeDeactivateComponent event, EntityRef entity, MechanicalPowerBlockNetworkComponent mechanicalPowerBlockNetwork, BlockComponent block) {
        // only remove a node if the block actually doesnt exist anymore.
        // This is particularly a problem in multiplayer where an entity is created both when the block
        // is placed (causing a default entity to be created) and when the entity from the server is sent.
        if (worldProvider.getBlock(block.getPosition()).getId() != block.getBlock().getId()) {
            removeNetworkNode(block.getPosition());
        }
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void createProducerNetworkNode(OnActivatedComponent event,
                                          EntityRef entity,
                                          MechanicalPowerProducerComponent type,
                                          MechanicalPowerBlockNetworkComponent mechanicalPowerBlockNetwork,
                                          BlockComponent block) {
        addNetworkNode(entity);
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void createConsumerNetworkNode(OnActivatedComponent event,
                                          EntityRef entity,
                                          MechanicalPowerConsumerComponent type,
                                          MechanicalPowerBlockNetworkComponent mechanicalPowerBlockNetwork,
                                          BlockComponent block) {
        addNetworkNode(entity);
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void createConductorNetworkNode(OnActivatedComponent event,
                                           EntityRef entity,
                                           MechanicalPowerConductorComponent type,
                                           MechanicalPowerBlockNetworkComponent mechanicalPowerBlockNetwork,
                                           BlockComponent block) {
        addNetworkNode(entity);
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void updateCurrentPowerInNetwork(OnChangedComponent event, EntityRef entity, MechanicalPowerProducerComponent producer, BlockComponent block) {
        Vector3i location = block.getPosition();
        if (networkNodes.containsKey(location)) {
            ProducerNode producerNode = (ProducerNode) networkNodes.get(location);
            // save the current power output so we can remove it if the node is removed
            producerNode.power = producer.active ? producer.power : 0;

            Network network = getNetwork(location);

            if (network != null) {
                updateNetworkDetails(network);
            }
        }
    }

    private void updateNetworkDetails(Network network) {
        MechanicalPowerNetworkDetails powerNetwork = getMechanicalPowerNetwork(network);
        powerNetwork.totalPower = 0f;
        powerNetwork.totalConsumers = 0;
        powerNetwork.totalProducers = 0;
        for (NetworkNode networkingNode : getNetworkNodes(network)) {
            if (networkingNode instanceof ProducerNode) {
                ProducerNode producerNode = (ProducerNode) networkingNode;
                powerNetwork.totalPower += producerNode.power;
                powerNetwork.totalProducers++;
            } else if (networkingNode instanceof ConsumerNode) {
                powerNetwork.totalConsumers++;
            }
        }
    }

    @Override
    public void networkAdded(Network<NetworkNode> network) {
        networks.put(network, new MechanicalPowerNetworkDetails());
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
