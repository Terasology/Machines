/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.entityNetwork.systems;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.terasology.assets.management.AssetManager;
import org.terasology.entityNetwork.Network;
import org.terasology.entityNetwork.NetworkNode;
import org.terasology.entityNetwork.NetworkNodeBuilder;
import org.terasology.entityNetwork.components.EntityNetworkComponent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.registry.In;
import org.terasology.registry.Share;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RegisterSystem
@Share(EntityNetworkManager.class)
public class EntityNetworkCommonSystem extends BaseComponentSystem implements UpdateSubscriberSystem, EntityNetworkManager {

    Map<String, BlockNetwork> blockNetworks = Maps.newHashMap();
    Multimap<EntityRef, NetworkNodeBuilder> pendingEntitiesToBeAdded = HashMultimap.create();
    Multimap<NetworkNode, EntityRef> entityLookup = HashMultimap.create();
    Multimap<EntityRef, NetworkNode> nodeLookup = HashMultimap.create();

    @In
    AssetManager assetManager;

    @ReceiveEvent
    public void onRemovedEntityNetwork(BeforeDeactivateComponent event, EntityRef entityRef, EntityNetworkComponent entityNetworkComponent) {
        for (NetworkNode node : Lists.newArrayList(nodeLookup.get(entityRef))) {
            remove(entityRef, node);
        }
    }

    private void remove(EntityRef entityRef, NetworkNode node) {
        nodeLookup.remove(entityRef, node);

        if (entityLookup.get(node).size() == 1) {
            BlockNetwork blocknetwork = blockNetworks.get(node.getNetworkId());
            blocknetwork.removeNetworkingBlock(node);
        }

        entityLookup.remove(node, entityRef);
    }

    private void add(EntityRef entityRef, NetworkNode node) {
        entityLookup.put(node, entityRef);

        // add to the actual network
        String networkId = node.getNetworkId();
        BlockNetwork blockNetwork = blockNetworks.get(networkId);
        if (blockNetwork == null) {
            blockNetwork = new BlockNetwork();
            blockNetworks.put(networkId, blockNetwork);
        }
        blockNetwork.addNetworkingBlock(node);

        nodeLookup.put(entityRef, node);
    }

    @ReceiveEvent
    public void onActivateEntityNetwork(OnActivatedComponent event, EntityRef entityRef, EntityNetworkComponent entityNetworkComponent) {
        Prefab connectionsPrefab = assetManager.getAsset(entityNetworkComponent.connectionsPrefab, Prefab.class).get();
        List<NetworkNodeBuilder> networkNodeBuilders = Lists.newArrayList();
        for (NetworkNodeBuilder builder : Iterables.filter(connectionsPrefab.iterateComponents(), NetworkNodeBuilder.class)) {
            NetworkNode newNetworkNode = builder.build(entityRef);
            if (newNetworkNode != null) {
                // we could already determine the type of network node, add it to the network
                add(entityRef, newNetworkNode);
            } else {
                // save this builder to resolve it later
                pendingEntitiesToBeAdded.put(entityRef, builder);
            }
        }
    }

    @Override
    public void update(float delta) {
        for (Map.Entry<EntityRef, Collection<NetworkNodeBuilder>> entry : pendingEntitiesToBeAdded.asMap().entrySet()) {
            if (!entry.getKey().exists() || !entry.getKey().hasComponent(EntityNetworkComponent.class)) {
                pendingEntitiesToBeAdded.removeAll(entry);
                continue;
            }

            for (NetworkNodeBuilder builder : Lists.newArrayList(entry.getValue())) {
                NetworkNode newNetworkNode = builder.build(entry.getKey());
                if (newNetworkNode != null) {
                    // we could already determine the type of network node, add it to the network
                    pendingEntitiesToBeAdded.remove(entry.getKey(), builder);
                    add(entry.getKey(), newNetworkNode);
                } else {
                    // keep this builder around for next time
                }
            }
        }
    }

    @Override
    public Iterable<NetworkNode> getNetworkNodes(Network network) {
        for (BlockNetwork blockNetwork : blockNetworks.values()) {
            if (blockNetwork.getNetworks().contains(network)) {
                return blockNetwork.getNetworkNodes(network);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public Iterable<Network> getNetworks(String networkId) {
        if (blockNetworks.containsKey(networkId)) {
            return blockNetworks.get(networkId).getNetworks();
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public EntityRef getEntityForNode(NetworkNode node) {
        Optional<EntityRef> entity = entityLookup.get(node).stream().findFirst();
        if (entity.isPresent()) {
            return entity.get();
        } else {
            return EntityRef.NULL;
        }
    }

    @Override
    public Collection<NetworkNode> getNodesForEntity(EntityRef entity) {
        return nodeLookup.get(entity);
    }

    @Override
    public Network getNetwork(NetworkNode node) {
        BlockNetwork blockNetwork = blockNetworks.get(node.getNetworkId());
        return blockNetwork.getNetwork(node);
    }
}
