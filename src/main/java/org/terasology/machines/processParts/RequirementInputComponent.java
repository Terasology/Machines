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
import com.google.common.collect.Sets;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.components.ProvidesProcessRequirements;
import org.terasology.registry.CoreRegistry;
import org.terasology.workstation.process.InvalidProcessException;
import org.terasology.workstation.process.ProcessPart;

import java.util.List;
import java.util.Set;

public class RequirementInputComponent implements Component, ProcessPart {
    public List<String> requirements = Lists.newArrayList();

    @Override
    public Set<String> validate(EntityRef instigator, EntityRef workstation, String parameter) throws InvalidProcessException {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        List<String> requirementsProvided = Lists.newArrayList();

        // get the requirements provided by the machine
        for (Component component : workstation.iterateComponents()) {
            if (component instanceof ProvidesProcessRequirements) {
                requirementsProvided.addAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()));
            }
        }

        // get the requirements provided by items (tools)
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, "REQUIREMENTS")) {
            for (Component component : item.iterateComponents()) {
                if (component instanceof ProvidesProcessRequirements) {
                    requirementsProvided.addAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()));
                }
            }
        }

        if (requirementsProvided.containsAll(requirements)) {
            Set<String> results = Sets.newHashSet();
            results.add("");
            return results;
        } else {
            return null;
        }
    }

    @Override
    public long getDuration(EntityRef instigator, EntityRef workstation, String result, String parameter) {
        return 0;
    }

    @Override
    public void executeStart(EntityRef instigator, EntityRef workstation, String result, String parameter) {

    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, String result, String parameter) {

    }
}
