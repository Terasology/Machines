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
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.console.commandSystem.annotations.Command;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.block.BlockComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    @In
    EntityManager entityManager;

    @ReceiveEvent
    public void onRemovedEntityNetwork(BeforeDeactivateComponent event, EntityRef entityRef, EntityNetworkComponent entityNetworkComponent) {
        removeEntityFromNetworks(entityRef);
    }

    private void removeEntityFromNetworks(EntityRef entityRef) {
        for (NetworkNode node : Lists.newArrayList(nodeLookup.get(entityRef))) {
            remove(entityRef, node);
        }
    }

    private void addEntityToNetworks(EntityRef entityRef) {
        Prefab entityPrefab = entityRef.getParentPrefab();

        // Treat block entities differently as they do not follow normal entity creation with an expected parentPrefab
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent != null) {
            entityPrefab = blockComponent.getBlock().getPrefab().get();
        }

        if (entityPrefab != null) {
            addToNetworkViaBuilders(entityRef, entityPrefab);
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
        if (!entityLookup.containsKey(node)) {
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
    }

    /**
     * Treat block entities differently as they do not follow normal entity creation with an expected parentPrefab
     */
    @ReceiveEvent
    public void onActivateEntityNetwork(OnActivatedComponent event, EntityRef entityRef, EntityNetworkComponent entityNetworkComponent, BlockComponent blockComponent) {
        addEntityToNetworks(entityRef);
    }

    @ReceiveEvent
    public void onActivateEntityNetwork(OnActivatedComponent event, EntityRef entityRef, EntityNetworkComponent entityNetworkComponent) {
        addEntityToNetworks(entityRef);
    }

    @ReceiveEvent
    public void onChangedEntityNetwork(OnChangedComponent event, EntityRef entityRef, EntityNetworkComponent entityNetworkComponent) {
        removeEntityFromNetworks(entityRef);
        addEntityToNetworks(entityRef);
    }

    private void addToNetworkViaBuilders(EntityRef entityRef, Prefab entityPrefab) {
        for (NetworkNodeBuilder builder : Iterables.filter(entityPrefab.iterateComponents(), NetworkNodeBuilder.class)) {
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
    public Collection<NetworkNode> getNetworkNodes(Network network) {
        for (BlockNetwork blockNetwork : blockNetworks.values()) {
            if (blockNetwork.getNetworks().contains(network)) {
                return Collections.unmodifiableCollection(new ArrayList<>(blockNetwork.getNetworkNodes(network)));
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<Network> getNetworks(String networkId) {
        if (blockNetworks.containsKey(networkId)) {
            return Collections.unmodifiableCollection(new ArrayList<>(blockNetworks.get(networkId).getNetworks()));
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
        return Collections.unmodifiableCollection(new ArrayList<>(nodeLookup.get(entity)));
    }

    @Override
    public Collection<Network> getNetworks(NetworkNode node) {
        BlockNetwork blockNetwork = blockNetworks.get(node.getNetworkId());
        return Collections.unmodifiableCollection(new ArrayList<>(blockNetwork.getNetworks(node)));
    }

    @Command(shortDescription = "Resets the entity network and reconnects everything", runOnServer = true)
    public String entityNetworkResetAllNetworks() {
        resetAllNetworks();
        return "Networks Reset";
    }

    private void resetAllNetworks() {
        nodeLookup.clear();
        entityLookup.clear();
        blockNetworks.clear();
        pendingEntitiesToBeAdded.clear();

        for (EntityRef entityRef : entityManager.getEntitiesWith(EntityNetworkComponent.class)) {
            addEntityToNetworks(entityRef);
        }
    }
}
