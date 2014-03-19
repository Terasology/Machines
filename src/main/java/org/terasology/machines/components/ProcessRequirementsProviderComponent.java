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
package org.terasology.machines.components;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.network.Replicate;
import org.terasology.workstation.process.inventory.ValidateInventoryItem;
import org.terasology.world.block.ForceBlockActive;

import java.util.List;

@ForceBlockActive
public class ProcessRequirementsProviderComponent implements Component, ProvidesProcessRequirements, ValidateInventoryItem {

    @Replicate
    public List<String> requirements = Lists.newArrayList();

    public ProcessRequirementsProviderComponent() {
    }

    public ProcessRequirementsProviderComponent(String... requirements) {
        for (String requirement : requirements) {
            this.requirements.add(requirement);
        }
    }

    @Override
    public String[] getRequirementsProvided() {
        return requirements.toArray(new String[0]);
    }

    @Override
    public boolean isResponsibleForSlot(EntityRef workstation, int slotNo) {
        CategorizedInventoryComponent categorizedInventory = workstation.getComponent(CategorizedInventoryComponent.class);
        if (categorizedInventory != null) {
            return categorizedInventory.slotMapping.get("REQUIREMENTS").contains(slotNo);
        }

        return false;
    }

    @Override
    public boolean isValid(EntityRef workstation, int slotNo, EntityRef instigator, EntityRef item) {
        ProcessRequirementsProviderComponent requirementsProvider = item.getComponent(ProcessRequirementsProviderComponent.class);
        if (requirementsProvider != null) {
            return requirementsProvider.requirements.containsAll(requirements);
        }

        return false;
    }
}
