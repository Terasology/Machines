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
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.components.ProvidesProcessRequirements;
import org.terasology.registry.CoreRegistry;

import java.util.List;

public class RequirementInputComponent implements Component, ProcessPart {
    public List<String> requirements = Lists.newArrayList();

    @Override
    public void resolve(EntityRef inputEntity) {
    }

    @Override
    public boolean validate(EntityRef entity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        List<String> requirementsProvided = Lists.newArrayList();

        // get the requirements provided by the machine
        for (Component component : entity.iterateComponents()) {
            if (component instanceof ProvidesProcessRequirements) {
                requirementsProvided.addAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()));
            }
        }

        // get the requirements provided by items (tools)
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, entity)) {
            for (Component component : item.iterateComponents()) {
                if (component instanceof ProvidesProcessRequirements) {
                    requirementsProvided.addAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()));
                }
            }
        }

        return requirementsProvided.containsAll(requirements);
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
