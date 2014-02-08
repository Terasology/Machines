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
package org.terasology.machines.internal;

import org.terasology.engine.Time;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.machines.ProcessingManager;
import org.terasology.machines.components.DelayedProcessOutputComponent;
import org.terasology.machines.processParts.ProcessDefinitionComponent;
import org.terasology.machines.processParts.ProcessPart;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.BlockItemFactory;

@RegisterSystem
@Share(ProcessingManager.class)
public class ProcessingManagerImpl implements ProcessingManager, ComponentSystem {

    @In
    EntityManager entityManager;
    @In
    BlockManager blockManager;
    @In
    PrefabManager prefabManager;
    @In
    InventoryManager inventoryManager;
    @In
    Time time;

    BlockItemFactory blockItemFactory;

    @Override
    public void performProcessing(EntityRef inputEntity, EntityRef outputEntity) {
        if (!outputEntity.hasComponent(DelayedProcessOutputComponent.class)) {
            Prefab process = getProcessDefinition(inputEntity, outputEntity);

            if (process != null) {
                resolveParts(inputEntity, process, false, false);
                resolveParts(outputEntity, process, true, false);

                // check for a delayed output
                ProcessDefinitionComponent processDefinition = process.getComponent(ProcessDefinitionComponent.class);
                DelayedProcessOutputComponent delayedProcessOutput = outputEntity.getComponent(DelayedProcessOutputComponent.class);

                if (delayedProcessOutput == null && processDefinition.processingTime > 0) {
                    // add a delayed process output
                    delayedProcessOutput = new DelayedProcessOutputComponent();
                    delayedProcessOutput.startTime = time.getGameTimeInMs();
                    delayedProcessOutput.endTime = delayedProcessOutput.startTime + processDefinition.processingTime;
                    delayedProcessOutput.process = process;
                    outputEntity.addComponent(delayedProcessOutput);
                } else {
                    resolveParts(outputEntity, process, false, true);
                    resolveParts(outputEntity, process, true, true);
                }
            }
        }
    }

    @Override
    public void resolveParts(EntityRef entity, Prefab process, boolean isOutput, boolean isEnd) {
        if (process != null) {
            for (Component component : process.iterateComponents()) {
                if (component instanceof ProcessPart) {
                    ProcessPart processPart = (ProcessPart) component;
                    if (processPart.isEnd() == isEnd && processPart.isOutput() == isOutput) {
                        processPart.resolve(entity);
                    }
                }
            }
        }
    }

    @Override
    public Prefab getProcessDefinition(EntityRef inputEntity, EntityRef outputEntity) {
        for (Prefab recipePrefab : prefabManager.listPrefabs(ProcessDefinitionComponent.class)) {
            Boolean allValidate = true;
            for (Component component : recipePrefab.iterateComponents()) {
                if (component instanceof ProcessPart) {
                    ProcessPart processPart = (ProcessPart) component;
                    EntityRef entity = processPart.isEnd() ? outputEntity : inputEntity;
                    if (!(processPart.validate(entity))) {
                        allValidate = false;
                        break;
                    }
                }
            }

            if (allValidate) {
                // all the components validate
                return recipePrefab;
            }
        }
        return null;
    }

    @Override
    public void initialise() {
        blockItemFactory = new BlockItemFactory(entityManager);
    }

    @Override
    public void shutdown() {

    }
}
