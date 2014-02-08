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

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.action.RemoveItemAction;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.family.BlockFamily;

import java.util.Map;

public class BlockInputComponent implements Component, ProcessPart {
    public Map<String, Integer> blocks;

    @Override
    public void resolve(EntityRef inputEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
        BlockManager blockManager = CoreRegistry.get(BlockManager.class);

        for (Map.Entry<String, Integer> entry : blocks.entrySet()) {
            BlockFamily blockFamily = blockManager.getBlockFamily(entry.getKey());
            EntityRef inputBlock = ExtendedInventoryManager.getItemByBlockFamily(inventoryManager, inputEntity, blockFamily);
            int stackSize = inventoryManager.getStackSize(inputBlock);

            RemoveItemAction removeItemAction = new RemoveItemAction(inputEntity, inputBlock, true, entry.getValue());
            inputEntity.send(removeItemAction);
        }
    }

    @Override
    public boolean validate(EntityRef entity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
        BlockManager blockManager = CoreRegistry.get(BlockManager.class);

        for (Map.Entry<String, Integer> entry : blocks.entrySet()) {
            BlockFamily blockFamily = blockManager.getBlockFamily(entry.getKey());
            EntityRef itemStack = ExtendedInventoryManager.getItemByBlockFamily(inventoryManager, entity, blockFamily);

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
