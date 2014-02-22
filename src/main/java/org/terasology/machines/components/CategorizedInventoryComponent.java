/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.network.Replicate;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.ForceBlockActive;

import java.util.List;
import java.util.Map;

@ForceBlockActive
public class CategorizedInventoryComponent implements Component {

    @Replicate
    public Map<String, List<Integer>> slotMapping = Maps.newHashMap();

    public Iterable<EntityRef> iterateItems(EntityRef inventoryEntity, String category) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
        List<EntityRef> items = Lists.newArrayList();

        if (slotMapping.containsKey(category)) {
            List<Integer> slots = slotMapping.get(category);
            for (int i : slots) {
                items.add(inventoryManager.getItemInSlot(inventoryEntity, i));
            }
        }

        return items;
    }


}
