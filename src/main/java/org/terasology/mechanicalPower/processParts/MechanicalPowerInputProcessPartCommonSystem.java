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
import org.terasology.potentialEnergyDevices.components.PotentialEnergyDeviceComponent;
import org.terasology.workstation.process.ProcessPartDescription;
import org.terasology.workstation.processPart.ProcessEntityIsInvalidToStartEvent;
import org.terasology.workstation.processPart.ProcessEntityStartExecutionEvent;
import org.terasology.workstation.processPart.metadata.ProcessEntityGetInputDescriptionEvent;
import org.terasology.workstation.processPart.metadata.ProcessEntityGetOutputDescriptionEvent;

@RegisterSystem
public class MechanicalPowerInputProcessPartCommonSystem extends BaseComponentSystem {

    ///// Processing

    @ReceiveEvent
    public void validateToStartExecution(ProcessEntityIsInvalidToStartEvent event, EntityRef processEntity,
                                         MechanicalPowerInputComponent mechanicalPowerInputComponent) {
        PotentialEnergyDeviceComponent consumer = event.getWorkstation().getComponent(PotentialEnergyDeviceComponent.class);
        if (consumer != null) {
            if (consumer.currentStoredEnergy >= mechanicalPowerInputComponent.power) {
                return;
            }
        }

        consumer = event.getInstigator().getComponent(PotentialEnergyDeviceComponent.class);
        if (consumer != null) {
            if (consumer.currentStoredEnergy >= mechanicalPowerInputComponent.power) {
                return;
            }
        }

        event.consume();
    }

    @ReceiveEvent
    public void startExecution(ProcessEntityStartExecutionEvent event, EntityRef processEntity,
                               MechanicalPowerInputComponent mechanicalPowerInputComponent) {
        PotentialEnergyDeviceComponent consumer = event.getWorkstation().getComponent(PotentialEnergyDeviceComponent.class);
        if (consumer != null) {
            consumer.currentStoredEnergy -= mechanicalPowerInputComponent.power;
            event.getWorkstation().saveComponent(consumer);
            return;
        }

        consumer = event.getInstigator().getComponent(PotentialEnergyDeviceComponent.class);
        if (consumer != null) {
            consumer.currentStoredEnergy -= mechanicalPowerInputComponent.power;
            event.getInstigator().saveComponent(consumer);
            return;
        }
    }

    ///// Metadata

    @ReceiveEvent
    public void getInputDescriptions(ProcessEntityGetInputDescriptionEvent event, EntityRef processEntity,
                                     MechanicalPowerInputComponent mechanicalPowerInputComponent) {
        event.addInputDescription(new ProcessPartDescription(null, mechanicalPowerInputComponent.power + " energy"));
    }

    @ReceiveEvent
    public void getOutputDescriptions(ProcessEntityGetOutputDescriptionEvent event, EntityRef processEntity,
                                      MechanicalPowerInputComponent mechanicalPowerInputComponent) {

    }
}
