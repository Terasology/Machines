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
package org.terasology.fluidTransport.processParts;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.fluidTransport.components.FluidPumpComponent;
import org.terasology.fluidTransport.systems.FluidTransportAuthoritySystem;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.workstation.process.ProcessPartDescription;
import org.terasology.workstation.processPart.ProcessEntityGetDurationEvent;
import org.terasology.workstation.processPart.ProcessEntityIsInvalidToStartEvent;
import org.terasology.workstation.processPart.ProcessEntityStartExecutionEvent;
import org.terasology.workstation.processPart.metadata.ProcessEntityGetInputDescriptionEvent;
import org.terasology.workstation.processPart.metadata.ProcessEntityGetOutputDescriptionEvent;

public class MechanicalPowerToPressureProcessPartCommonSystem extends BaseComponentSystem {

    ///// Processing

    @ReceiveEvent
    public void validateToStartExecution(ProcessEntityIsInvalidToStartEvent event, EntityRef processEntity,
                                         MechanicalPowerToPressureComponent mechanicalPowerToPressureComponent) {
        MechanicalPowerConsumerComponent consumer = event.getWorkstation().getComponent(MechanicalPowerConsumerComponent.class);
        FluidPumpComponent fluidPumpComponent = event.getWorkstation().getComponent(FluidPumpComponent.class);
        if (consumer != null && fluidPumpComponent != null) {
            if (consumer.currentStoredPower > 0) {
                return;
            }
        }
        event.consume();
    }

    @ReceiveEvent
    public void startExecution(ProcessEntityStartExecutionEvent event, EntityRef processEntity,
                               MechanicalPowerToPressureComponent mechanicalPowerToPressureComponent) {
        MechanicalPowerConsumerComponent consumer = event.getWorkstation().getComponent(MechanicalPowerConsumerComponent.class);
        FluidPumpComponent fluidPumpComponent = event.getWorkstation().getComponent(FluidPumpComponent.class);
        if (consumer != null && fluidPumpComponent != null) {
            fluidPumpComponent.pressure = Math.max(consumer.currentStoredPower, fluidPumpComponent.pressure);
            event.getWorkstation().saveComponent(fluidPumpComponent);

            consumer.currentStoredPower = 0;
            event.getWorkstation().saveComponent(consumer);
        }
    }

    @ReceiveEvent
    public void getDuration(ProcessEntityGetDurationEvent event, EntityRef processEntity,
                            MechanicalPowerToPressureComponent mechanicalPowerToPressureComponent) {
        event.add(FluidTransportAuthoritySystem.UPDATE_INTERVAL / 1000f);
    }

    ///// Metadata

    @ReceiveEvent
    public void getInputDescriptions(ProcessEntityGetInputDescriptionEvent event, EntityRef processEntity,
                                     MechanicalPowerToPressureComponent mechanicalPowerToPressureComponent) {
        event.addInputDescription(new ProcessPartDescription(null, "1 energy"));
    }

    @ReceiveEvent
    public void getOutputDescriptions(ProcessEntityGetOutputDescriptionEvent event, EntityRef processEntity,
                                      MechanicalPowerToPressureComponent mechanicalPowerToPressureComponent) {
        event.addOutputDescription(new ProcessPartDescription(null, "1 psi"));
    }
}
