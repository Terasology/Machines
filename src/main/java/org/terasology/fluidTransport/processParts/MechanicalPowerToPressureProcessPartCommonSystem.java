// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.processParts;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.fluidTransport.components.FluidPumpComponent;
import org.terasology.fluidTransport.systems.FluidTransportAuthoritySystem;
import org.terasology.potentialEnergyDevices.components.PotentialEnergyDeviceComponent;
import org.terasology.workstation.process.ProcessPartDescription;
import org.terasology.workstation.processPart.ProcessEntityGetDurationEvent;
import org.terasology.workstation.processPart.ProcessEntityIsInvalidToStartEvent;
import org.terasology.workstation.processPart.ProcessEntityStartExecutionEvent;
import org.terasology.workstation.processPart.metadata.ProcessEntityGetInputDescriptionEvent;
import org.terasology.workstation.processPart.metadata.ProcessEntityGetOutputDescriptionEvent;

@RegisterSystem
public class MechanicalPowerToPressureProcessPartCommonSystem extends BaseComponentSystem {

    ///// Processing

    @ReceiveEvent
    public void validateToStartExecution(ProcessEntityIsInvalidToStartEvent event, EntityRef processEntity,
                                         MechanicalPowerToPressureComponent mechanicalPowerToPressureComponent) {
        PotentialEnergyDeviceComponent consumer =
                event.getWorkstation().getComponent(PotentialEnergyDeviceComponent.class);
        FluidPumpComponent fluidPumpComponent = event.getWorkstation().getComponent(FluidPumpComponent.class);
        if (consumer != null && fluidPumpComponent != null) {
            if (consumer.currentStoredEnergy > 0) {
                return;
            }
        }
        event.consume();
    }

    @ReceiveEvent
    public void startExecution(ProcessEntityStartExecutionEvent event, EntityRef processEntity,
                               MechanicalPowerToPressureComponent mechanicalPowerToPressureComponent) {
        PotentialEnergyDeviceComponent consumer =
                event.getWorkstation().getComponent(PotentialEnergyDeviceComponent.class);
        FluidPumpComponent fluidPumpComponent = event.getWorkstation().getComponent(FluidPumpComponent.class);
        if (consumer != null && fluidPumpComponent != null) {
            fluidPumpComponent.pressure = Math.max(consumer.currentStoredEnergy, fluidPumpComponent.pressure);
            event.getWorkstation().saveComponent(fluidPumpComponent);

            consumer.currentStoredEnergy = 0;
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
