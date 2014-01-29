/*
 * Copyright 2013 MovingBlocks
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
package org.terasology.machines;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.SlotBasedInventoryManager;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemComponent;

import java.util.List;

public abstract class ExtendedInventoryManager {
    public static EntityRef getItemByBlockFamily(SlotBasedInventoryManager inventoryManager, EntityRef inventoryEntity, BlockFamily blockFamily) {
        InventoryComponent inventoryComponent = inventoryEntity.getComponent(InventoryComponent.class);
        if (inventoryComponent != null) {
            for (EntityRef item : iterateItems(inventoryManager, inventoryEntity)) {
                BlockItemComponent blockItemComponent = item.getComponent(BlockItemComponent.class);
                if (blockItemComponent != null && blockItemComponent.blockFamily.getURI().equals(blockFamily.getURI())) {
                    return item;
                }
            }
        }

        return null;
    }

    public static Iterable<EntityRef> iterateItems(SlotBasedInventoryManager inventoryManager, EntityRef inventoryEntity) {
        List<EntityRef> items = Lists.newArrayList();

        for (Integer i = 0; i < inventoryManager.getNumSlots(inventoryEntity); i++) {
            items.add(inventoryManager.getItemInSlot(inventoryEntity, i));
        }

        return items;
    }
}
