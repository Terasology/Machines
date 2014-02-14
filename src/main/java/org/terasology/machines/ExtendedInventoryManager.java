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
import org.terasology.logic.common.DisplayNameComponent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.PickupBuilder;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.math.Side;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemComponent;

import javax.vecmath.Vector3f;
import java.util.List;

public abstract class ExtendedInventoryManager {
    static Random random = new FastRandom();

    public static EntityRef getItemByBlockFamily(InventoryManager inventoryManager, EntityRef inventoryEntity, String inventoryCategory, BlockFamily blockFamily) {
        InventoryComponent inventoryComponent = inventoryEntity.getComponent(InventoryComponent.class);
        if (inventoryComponent != null) {
            for (EntityRef item : iterateItems(inventoryManager, inventoryEntity, inventoryCategory)) {
                BlockItemComponent blockItemComponent = item.getComponent(BlockItemComponent.class);
                if (blockItemComponent != null && blockItemComponent.blockFamily.getURI().equals(blockFamily.getURI())) {
                    return item;
                }
            }
        }

        return null;
    }

    public static EntityRef getItemByItemName(InventoryManager inventoryManager, EntityRef inventoryEntity, String inventoryCategory, String itemName) {
        InventoryComponent inventoryComponent = inventoryEntity.getComponent(InventoryComponent.class);
        if (inventoryComponent != null) {
            for (EntityRef existingItem : iterateItems(inventoryManager, inventoryEntity, inventoryCategory)) {
                Prefab existingItemPrefab = existingItem.getParentPrefab();
                Prefab itemPrefab = Assets.getPrefab(itemName);
                if (itemPrefab != null && itemPrefab.equals(existingItemPrefab)) {
                    return existingItem;
                }
            }
        }

        return null;
    }

    public static Iterable<EntityRef> iterateItems(InventoryManager inventoryManager, EntityRef inventoryEntity, Side side) {
        return iterateItems(inventoryManager, inventoryEntity, side.toString());
    }

    public static Iterable<EntityRef> iterateItems(InventoryManager inventoryManager, EntityRef inventoryEntity, String inventoryCategory) {
        CategorizedInventoryComponent categorizedInventoryComponent = inventoryEntity.getComponent(CategorizedInventoryComponent.class);

        if (categorizedInventoryComponent != null) {
            return categorizedInventoryComponent.iterateItems(inventoryEntity, inventoryCategory);
        } else {
            return iterateItems(inventoryManager, inventoryEntity);
        }
    }

    public static Iterable<EntityRef> iterateItems(InventoryManager inventoryManager, EntityRef inventoryEntity) {
        List<EntityRef> items = Lists.newArrayList();

        for (Integer i = 0; i < inventoryManager.getNumSlots(inventoryEntity); i++) {
            items.add(inventoryManager.getItemInSlot(inventoryEntity, i));
        }

        return items;
    }


    public static boolean hasInventorySpace(InventoryManager inventoryManager, EntityRef entity, String inventoryCategory, List<EntityRef> items) {
        int emptySlots = 0;
        // loop through all the items in the inventory
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity, inventoryCategory)) {
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

        DisplayNameComponent info = item.getComponent(DisplayNameComponent.class);
        if (info != null) {
            return info.name;
        }
        BlockItemComponent blockItem = item.getComponent(BlockItemComponent.class);
        if (blockItem != null) {
            return blockItem.blockFamily.getDisplayName();
        }
        return "";
    }

    public static EntityRef createItem(EntityManager entityManager, String prefabName, int stackCount) {
        EntityRef newItem = entityManager.create(prefabName);
        ItemComponent itemComponent = newItem.getComponent(ItemComponent.class);
        itemComponent.stackCount = (byte) stackCount;
        return newItem;
    }

    public static void dropItem(EntityRef item, Vector3f location) {
        PickupBuilder pickupBuilder = new PickupBuilder();
        EntityRef pickup = pickupBuilder.createPickupFor(item, location, 200, true);
        pickup.send(new ImpulseEvent(random.nextVector3f(10.0f)));
    }
}
