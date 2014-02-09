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
package org.terasology.machines.processParts;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.action.GiveItemAction;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemFactory;

import java.util.List;
import java.util.Map;

public class BlockOutputComponent implements Component, ProcessPart, ProcessDescriptor {
    public Map<String, Integer> blocks;

    @Override
    public String getDescription() {
        String result = "";

        BlockManager blockManager = CoreRegistry.get(BlockManager.class);

        for (Map.Entry<String, Integer> entry : blocks.entrySet()) {
            BlockFamily blockFamily = blockManager.getBlockFamily(entry.getKey());
            result += entry.getValue() + " " + blockFamily.getDisplayName() + "\n";
        }

        return result;
    }

    @Override
    public void resolve(EntityRef outputEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        for (EntityRef item : createItems()) {
            CategorizedInventoryComponent categorizedInventoryComponent = outputEntity.getComponent(CategorizedInventoryComponent.class);
            List<Integer> slots = categorizedInventoryComponent.slotMapping.get(CategorizedInventoryComponent.OUTPUT);

            for (int slot : slots) {
                GiveItemAction giveItemAction = new GiveItemAction(outputEntity, item, slot);
                outputEntity.send(giveItemAction);
                if (!giveItemAction.isConsumed()) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean validate(EntityRef entity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        // find a spot for each item
        List<EntityRef> items = createItems();
        int emptySlots = 0;
        // loop through all the items in the inventory
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity, CategorizedInventoryComponent.OUTPUT)) {
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

    @Override
    public boolean isOutput() {
        return true;
    }

    @Override
    public boolean isEnd() {
        return true;
    }

    private List<EntityRef> createItems() {
        BlockManager blockManager = CoreRegistry.get(BlockManager.class);
        BlockItemFactory blockItemFactory = new BlockItemFactory(CoreRegistry.get(EntityManager.class));

        List<EntityRef> outputItems = Lists.newArrayList();
        for (Map.Entry<String, Integer> entry : blocks.entrySet()) {
            BlockFamily blockFamily = blockManager.getBlockFamily(entry.getKey());
            EntityRef newItem = blockItemFactory.newInstance(blockFamily, entry.getValue());
            outputItems.add(newItem);
        }
        return outputItems;
    }
}
