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

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.action.GiveItemAction;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.registry.CoreRegistry;

import java.util.List;
import java.util.Map;

public class ItemOutputComponent implements Component, ProcessPart, ProcessDescriptor {
    public Map<String, Integer> items;

    @Override
    public String getDescription() {
        String result = "";

        List<EntityRef> itemEntities = createItems();

        for (EntityRef item : itemEntities) {
            String name = ExtendedInventoryManager.getLabelFor(item);
            result += "?" + " " + name + "\n";
        }

        return result;
    }

    @Override
    public void resolve(EntityRef outputEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        for (EntityRef item : createItems()) {
            GiveItemAction giveItemAction = new GiveItemAction(outputEntity, item);
            outputEntity.send(giveItemAction);
        }
    }

    @Override
    public boolean validate(EntityRef entity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        // find a spot for each item
        List<EntityRef> itemEntities = createItems();
        return ExtendedInventoryManager.hasInventorySpace(inventoryManager, entity, itemEntities);
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
        EntityManager entityManager = CoreRegistry.get(EntityManager.class);

        List<EntityRef> outputItems = Lists.newArrayList();
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            outputItems.add(ExtendedInventoryManager.createItem(entityManager, entry.getKey(), entry.getValue()));
        }
        return outputItems;
    }
}
