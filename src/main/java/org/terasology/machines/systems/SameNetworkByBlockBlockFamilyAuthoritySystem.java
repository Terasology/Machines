// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.OnChangedBlock;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.family.UpdatesWithNeighboursFamily;
import org.terasology.entityNetwork.components.EntityNetworkComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SameNetworkByBlockBlockFamilyAuthoritySystem extends BaseComponentSystem {
    @In
    WorldProvider worldProvider;

    /**
     * Ensure that when an UpdatesWithNeighboursFamily block is placed, it verifies the block type after the entity
     * magic has happened
     *
     * @param event the event received
     * @param entityRef the entity that sent the event
     */
    @ReceiveEvent
    public void onBlockChangedWithSameNetworkByBlockBlockFamily(OnChangedBlock event, EntityRef entityRef,
                                                                EntityNetworkComponent entityNetworkComponent) {
        if (event.getNewType().getBlockFamily() instanceof UpdatesWithNeighboursFamily) {
            UpdatesWithNeighboursFamily blockFamily = (UpdatesWithNeighboursFamily) event.getNewType().getBlockFamily();
            Block shouldBeBlock = blockFamily.getBlockForNeighborUpdate(event.getBlockPosition(), event.getNewType());
            if (shouldBeBlock != event.getNewType()) {
                worldProvider.setBlock(event.getBlockPosition(), shouldBeBlock);
            }
        }
    }
}
