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
import org.terasology.asset.Assets;
import org.terasology.assets.ResourceUrn;
import org.terasology.engine.Time;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.components.ProcessRequirementsProviderComponent;
import org.terasology.machines.components.ProcessRequirementsProviderFromWorkstationComponent;
import org.terasology.machines.events.RequirementUsedEvent;
import org.terasology.machines.ui.OverlapLayout;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.UIWidget;
import org.terasology.rendering.nui.databinding.ReadOnlyBinding;
import org.terasology.workstation.process.DescribeProcess;
import org.terasology.workstation.process.ProcessPart;
import org.terasology.workstation.process.ProcessPartDescription;
import org.terasology.workstation.process.ProcessPartOrdering;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.inventory.InventoryInputComponent;
import org.terasology.workstation.process.inventory.ValidateInventoryItem;
import org.terasology.workstation.ui.InventoryItem;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RequirementInputComponent implements Component, ProcessPart, DescribeProcess, ValidateInventoryItem, ProcessPartOrdering {
    public static final String REQUIREMENTSINVENTORYCATEGORY = "REQUIREMENTS";
    public static final int TIMEBETWEENWIDGETSWITCH = 1500;

    public List<String> requirements = Lists.newArrayList();

    @Override
    public boolean validateBeforeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        List<String> requirementsProvided = Lists.newArrayList();

        // get the requirements provided by the machine
        ProcessRequirementsProviderFromWorkstationComponent workstationProcessRequirements
                = workstation.getComponent(ProcessRequirementsProviderFromWorkstationComponent.class);
        if (workstationProcessRequirements != null) {
            requirementsProvided.addAll(workstationProcessRequirements.requirements);
        }

        // get the requirements provided by items (tools)
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, false, REQUIREMENTSINVENTORYCATEGORY)) {
            ProcessRequirementsProviderComponent processRequirementsProviderComponent = item.getComponent(ProcessRequirementsProviderComponent.class);
            if (processRequirementsProviderComponent != null) {
                requirementsProvided.addAll(processRequirementsProviderComponent.requirements);
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
        ProcessRequirementsProviderFromWorkstationComponent workstationProcessRequirements
                = workstation.getComponent(ProcessRequirementsProviderFromWorkstationComponent.class);
        if (workstationProcessRequirements != null) {
            if (requirementsRequired.removeAll(workstationProcessRequirements.requirements)) {
                workstation.send(new RequirementUsedEvent(processEntity));
            }
        }

        // get the requirements provided by items (tools)
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, false, REQUIREMENTSINVENTORYCATEGORY)) {
            ProcessRequirementsProviderComponent processRequirementsProviderComponent = item.getComponent(ProcessRequirementsProviderComponent.class);
            if (processRequirementsProviderComponent != null) {
                if (requirementsRequired.removeAll(processRequirementsProviderComponent.requirements)) {
                    item.send(new RequirementUsedEvent(processEntity));
                }
            }
        }


    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {

    }

    @Override
    public Collection<ProcessPartDescription> getInputDescriptions() {
        List<ProcessPartDescription> descriptions = Lists.newLinkedList();
        for (String requirement : requirements) {
            UIWidget widget = createWidgetForRequirement(requirement, CoreRegistry.get(EntityManager.class), CoreRegistry.get(Time.class));
            descriptions.add(new ProcessPartDescription(null, requirement, widget));
        }
        return descriptions;
    }

    private static UIWidget createWidgetForRequirement(String requirement, EntityManager entityManager, Time time) {
        List<Prefab> relatedPrefabs = Lists.newArrayList();
        for (ResourceUrn resourceUrn : Assets.list(Prefab.class)) {
            Prefab prefab = Assets.get(resourceUrn, Prefab.class).get();
            ProcessRequirementsProviderComponent requirementsProviderComponent = prefab.getComponent(ProcessRequirementsProviderComponent.class);
            if (prefab.hasComponent(ItemComponent.class) && requirementsProviderComponent != null && requirementsProviderComponent.requirements.contains(requirement)) {
                relatedPrefabs.add(prefab);
            }
        }

        OverlapLayout layout = new OverlapLayout();
        for (int i = 0; i < relatedPrefabs.size(); i++) {
            Prefab prefab = relatedPrefabs.get(i);
            final Integer visibleAtIndex = i;

            // create entity to display
            EntityBuilder entityBuilder = entityManager.newBuilder(prefab);
            entityBuilder.setPersistent(false);
            EntityRef entityRef = entityBuilder.build();

            // create the widget
            InventoryItem inventoryItem = new InventoryItem(entityRef);
            inventoryItem.bindVisible(new ReadOnlyBinding<Boolean>() {
                @Override
                public Boolean get() {
                    return Math.floor(time.getRealTimeInMs() / TIMEBETWEENWIDGETSWITCH) % relatedPrefabs.size() == visibleAtIndex;
                }
            });

            // clean up and add the widget
            entityRef.destroy();
            layout.addWidget(inventoryItem);
        }

        return layout;

    }

    @Override
    public Collection<ProcessPartDescription> getOutputDescriptions() {
        return Collections.emptyList();
    }

    @Override
    public boolean isResponsibleForSlot(EntityRef workstation, int slotNo) {
        return WorkstationInventoryUtils.getAssignedInputSlots(workstation, REQUIREMENTSINVENTORYCATEGORY).contains(slotNo);
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
