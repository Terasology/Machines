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
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.inventory.InventoryManager;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.workstation.process.WorkstationInventoryUtils;

import java.util.List;

public abstract class ExtendedInventoryManager {

    /**
     * returns the items in the inventory category.
     */
    public static Iterable<EntityRef> iterateItems(InventoryManager inventoryManager, EntityRef inventoryEntity, boolean isOutputCategory, String inventoryCategory) {
        List<EntityRef> items = Lists.newArrayList();
        List<Integer> slots = WorkstationInventoryUtils.getAssignedSlots(inventoryEntity, isOutputCategory, inventoryCategory);
        for (int i : slots) {
            items.add(inventoryManager.getItemInSlot(inventoryEntity, i));
        }
        return items;
    }

    public static Iterable<EntityRef> iterateItems(InventoryManager inventoryManager, EntityRef inventoryEntity) {
        List<EntityRef> items = Lists.newArrayList();

        for (Integer i = 0; i < inventoryManager.getNumSlots(inventoryEntity); i++) {
            items.add(inventoryManager.getItemInSlot(inventoryEntity, i));
        }

        return items;
    }

    public static EntityRef createItem(EntityManager entityManager, String prefabName, int stackCount) {
        EntityRef newItem = entityManager.create(prefabName);
        ItemComponent itemComponent = newItem.getComponent(ItemComponent.class);
        itemComponent.stackCount = (byte) stackCount;
        return newItem;
    }
}
