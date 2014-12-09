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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.machines.components.ProcessRequirementsProviderComponent;
import org.terasology.machines.components.ProvidesProcessRequirements;
import org.terasology.machines.events.RequirementUsedEvent;
import org.terasology.registry.CoreRegistry;
import org.terasology.workstation.process.DescribeProcess;
import org.terasology.workstation.process.ProcessPart;
import org.terasology.workstation.process.ProcessPartDescription;
import org.terasology.workstation.process.ProcessPartOrdering;
import org.terasology.workstation.process.inventory.InventoryInputComponent;
import org.terasology.workstation.process.inventory.ValidateInventoryItem;

import java.util.List;

public class RequirementInputComponent implements Component, ProcessPart, DescribeProcess, ValidateInventoryItem, ProcessPartOrdering {
    public static final String REQUIREMENTSINVENTORYCATEGORY = "REQUIREMENTS";

    public List<String> requirements = Lists.newArrayList();

    @Override
    public boolean validateBeforeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        List<String> requirementsProvided = Lists.newArrayList();

        // get the requirements provided by the machine
        for (Component component : workstation.iterateComponents()) {
            if (component instanceof ProvidesProcessRequirements) {
                requirementsProvided.addAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()));
            }
        }

        // get the requirements provided by items (tools)
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, REQUIREMENTSINVENTORYCATEGORY)) {
            for (Component component : item.iterateComponents()) {
                if (component instanceof ProvidesProcessRequirements) {
                    requirementsProvided.addAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()));
                }
            }
        }

        return requirementsProvided.containsAll(requirements);
    }

    @Override
    public long getDuration(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        return 0;
    }

    @Override
    public void executeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        List<String> requirementsRequired = Lists.newArrayList(requirements);

        // get the requirements provided by the machine
        for (Component component : workstation.iterateComponents()) {
            if (component instanceof ProvidesProcessRequirements) {
                if (requirementsRequired.removeAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()))) {
                    workstation.send(new RequirementUsedEvent(processEntity));
                }
            }
        }

        // get the requirements provided by items (tools)
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, REQUIREMENTSINVENTORYCATEGORY)) {
            for (Component component : item.iterateComponents()) {
                if (component instanceof ProvidesProcessRequirements) {
                    if (requirementsRequired.removeAll(Lists.newArrayList(((ProvidesProcessRequirements) component).getRequirementsProvided()))) {
                        item.send(new RequirementUsedEvent(processEntity));
                    }
                }
            }
        }

    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {

    }

    @Override
    public ProcessPartDescription getOutputDescription() {
        return null;
    }

    @Override
    public ProcessPartDescription getInputDescription() {
        return new ProcessPartDescription(Joiner.on(", ").join(requirements));
    }

    @Override
    public int getComplexity() {
        return requirements.size();
    }

    @Override
    public boolean isResponsibleForSlot(EntityRef workstation, int slotNo) {
        CategorizedInventoryComponent categorizedInventoryComponent = workstation.getComponent(CategorizedInventoryComponent.class);
        if (categorizedInventoryComponent != null && categorizedInventoryComponent.slotMapping.containsKey(REQUIREMENTSINVENTORYCATEGORY)) {
            return categorizedInventoryComponent.slotMapping.get(REQUIREMENTSINVENTORYCATEGORY).contains(slotNo);
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

    @Override
    public int getSortOrder() {
        return InventoryInputComponent.SORTORDER - 1;
    }
}
