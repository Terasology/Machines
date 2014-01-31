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
import org.terasology.asset.Assets;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.logic.common.DisplayInformationComponent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.SlotBasedInventoryManager;
import org.terasology.world.block.BlockComponent;
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

    public static EntityRef getItemByItemName(SlotBasedInventoryManager inventoryManager, EntityRef inventoryEntity, String itemName) {
        InventoryComponent inventoryComponent = inventoryEntity.getComponent(InventoryComponent.class);
        if (inventoryComponent != null) {
            for (EntityRef existingItem : iterateItems(inventoryManager, inventoryEntity)) {
                Prefab existingItemPrefab = existingItem.getParentPrefab();
                Prefab itemPrefab = Assets.getPrefab(itemName);
                if (itemPrefab != null && itemPrefab.equals(existingItemPrefab)) {
                    return existingItem;
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


    public static boolean hasInventorySpace(SlotBasedInventoryManager inventoryManager, EntityRef entity, List<EntityRef> items) {
        int emptySlots = 0;
        // loop through all the items in the inventory
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
            if (inventoryManager.getStackSize(item) == 0) {
                emptySlots++;
                continue;
            }

            // loop through the items that we want to ensure room for
            EntityRef foundStackableItem = null;
            for (EntityRef createdItem : items) {
                if (inventoryManager.canStackTogether(item, createdItem)) {
                    foundStackableItem = createdItem;
                    break;
                }
            }

            // we found an item that we have room for
            if (foundStackableItem != null) {
                items.remove(foundStackableItem);
            }
        }

        return (emptySlots >= items.size());
    }


    public static String getLabelFor(EntityRef item) {
        BlockComponent block = item.getComponent(BlockComponent.class);
        if (block != null) {
            return block.getBlock().getBlockFamily().getDisplayName();
        }

        DisplayInformationComponent info = item.getComponent(DisplayInformationComponent.class);
        if (info != null) {
            return info.name;
        }
        BlockItemComponent blockItem = item.getComponent(BlockItemComponent.class);
        if (blockItem != null) {
            return blockItem.blockFamily.getDisplayName();
        }
        ItemComponent itemComponent = item.getComponent(ItemComponent.class);
        if (itemComponent != null) {
            return itemComponent.name;
        }
        return "";
    }

    public static EntityRef createItem(EntityManager entityManager, String prefabName, int stackCount) {
        EntityRef newItem = entityManager.create(prefabName);
        ItemComponent itemComponent = newItem.getComponent(ItemComponent.class);
        itemComponent.stackCount = (byte) stackCount;
        return newItem;
    }
}
