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
package org.terasology.machines.systems;

import org.terasology.entityNetwork.components.EntityNetworkComponent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.registry.In;
import org.terasology.world.OnChangedBlock;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.family.UpdatesWithNeighboursFamily;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SameNetworkByBlockBlockFamilyAuthoritySystem extends BaseComponentSystem {
    @In
    WorldProvider worldProvider;

    /**
     * Ensure that when an UpdatesWithNeighboursFamily block is placed, it verifies the block type after the entity magic has happened
     *
     * @param event the event received
     * @param entityRef the entity that sent the event
     */
    @ReceiveEvent
    public void onBlockChangedWithSameNetworkByBlockBlockFamily(OnChangedBlock event, EntityRef entityRef, EntityNetworkComponent entityNetworkComponent) {
        if (event.getNewType().getBlockFamily() instanceof UpdatesWithNeighboursFamily) {
            UpdatesWithNeighboursFamily blockFamily = (UpdatesWithNeighboursFamily) event.getNewType().getBlockFamily();
            Block shouldBeBlock = blockFamily.getBlockForNeighborUpdate(event.getBlockPosition(), event.getNewType());
            if (shouldBeBlock != event.getNewType()) {
                worldProvider.setBlock(event.getBlockPosition(), shouldBeBlock);
            }
        }
    }
}
