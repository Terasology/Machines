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
package org.terasology.machines.processParts;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.action.RemoveItemAction;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.BlockManager;

import java.util.Map;

public class ItemInputComponent implements Component, ProcessPart {
    public Map<String, Integer> items;

    @Override
    public void resolve(EntityRef inputEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
        EntityManager entityManager = CoreRegistry.get(EntityManager.class);

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            EntityRef inputItem = ExtendedInventoryManager.getItemByItemName(inventoryManager, inputEntity, CategorizedInventoryComponent.INPUT, entry.getKey());
            int stackSize = inventoryManager.getStackSize(inputItem);
            RemoveItemAction removeItemAction = new RemoveItemAction(inputEntity, inputItem, false, entry.getValue());
            inputEntity.send(removeItemAction);
        }
    }

    @Override
    public boolean validate(EntityRef entity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
        BlockManager blockManager = CoreRegistry.get(BlockManager.class);


        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            EntityRef itemStack = ExtendedInventoryManager.getItemByItemName(inventoryManager, entity, CategorizedInventoryComponent.INPUT, entry.getKey());

            if (itemStack == null || entry.getValue() > inventoryManager.getStackSize(itemStack)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isOutput() {
        return false;
    }

    @Override
    public boolean isEnd() {
        return false;
    }
}
