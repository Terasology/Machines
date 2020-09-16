// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.mechanicalPower.processParts;

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
        PotentialEnergyDeviceComponent consumer =
                event.getWorkstation().getComponent(PotentialEnergyDeviceComponent.class);
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
        PotentialEnergyDeviceComponent consumer =
                event.getWorkstation().getComponent(PotentialEnergyDeviceComponent.class);
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
