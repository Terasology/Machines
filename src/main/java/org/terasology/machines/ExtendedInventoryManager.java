// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines;

import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.inventory.logic.InventoryManager;
import org.terasology.workstation.process.WorkstationInventoryUtils;

import java.util.List;

public abstract class ExtendedInventoryManager {

    /**
     * returns the items in the inventory category.
     */
    public static Iterable<EntityRef> iterateItems(InventoryManager inventoryManager, EntityRef inventoryEntity,
                                                   boolean isOutputCategory, String inventoryCategory) {
        List<EntityRef> items = Lists.newArrayList();
        List<Integer> slots = WorkstationInventoryUtils.getAssignedSlots(inventoryEntity, isOutputCategory,
                inventoryCategory);
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
