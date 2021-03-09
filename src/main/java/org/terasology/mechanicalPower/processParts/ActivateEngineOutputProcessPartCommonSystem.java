/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.mechanicalPower.processParts;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.mechanicalPower.components.MechanicalPowerProducerComponent;
import org.terasology.workstation.process.ProcessPartDescription;
import org.terasology.workstation.processPart.ProcessEntityFinishExecutionEvent;
import org.terasology.workstation.processPart.ProcessEntityGetDurationEvent;
import org.terasology.workstation.processPart.ProcessEntityIsInvalidToStartEvent;
import org.terasology.workstation.processPart.ProcessEntityStartExecutionEvent;
import org.terasology.workstation.processPart.metadata.ProcessEntityGetOutputDescriptionEvent;

@RegisterSystem
public class ActivateEngineOutputProcessPartCommonSystem extends BaseComponentSystem {

    ///// Processing

    @ReceiveEvent
    public void validateToStartExecution(ProcessEntityIsInvalidToStartEvent event, EntityRef processEntity,
                                         ActivateEngineOutputComponent activateEngineOutputComponent) {
        MechanicalPowerProducerComponent producer = event.getWorkstation().getComponent(MechanicalPowerProducerComponent.class);
        if (producer == null || producer.active) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void startExecution(ProcessEntityStartExecutionEvent event, EntityRef processEntity,
                               ActivateEngineOutputComponent activateEngineOutputComponent) {
        MechanicalPowerProducerComponent producer = event.getWorkstation().getComponent(MechanicalPowerProducerComponent.class);
        if (producer != null) {
            producer.active = true;
            event.getWorkstation().saveComponent(producer);
        }
    }

    @ReceiveEvent
    public void getDuration(ProcessEntityGetDurationEvent event, EntityRef processEntity,
                            ActivateEngineOutputComponent activateEngineOutputComponent) {
        event.add(activateEngineOutputComponent.activateTime / 1000f);
    }

    @ReceiveEvent
    public void finishExecution(ProcessEntityFinishExecutionEvent event, EntityRef entityRef,
                                ActivateEngineOutputComponent activateEngineOutputComponent) {
        MechanicalPowerProducerComponent producer = event.getWorkstation().getComponent(MechanicalPowerProducerComponent.class);
        if (producer != null) {
            producer.active = false;
            event.getWorkstation().saveComponent(producer);
        }
    }

    ///// Metadata

    @ReceiveEvent
    public void getOutputDescriptions(ProcessEntityGetOutputDescriptionEvent event, EntityRef processEntity,
                                      ActivateEngineOutputComponent activateEngineOutputComponent) {
        event.addOutputDescription(new ProcessPartDescription(null, (activateEngineOutputComponent.activateTime / 1000f) + " sec"));
    }
}
