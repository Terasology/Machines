/*
 * Copyright 2014 MovingBlocks
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

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.fluidTransport.components.FluidPumpComponent;
import org.terasology.fluidTransport.systems.FluidTransportAuthoritySystem;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.workstation.process.DescribeProcess;
import org.terasology.workstation.process.ProcessPart;
import org.terasology.workstation.process.ProcessPartDescription;

import java.util.Collection;
import java.util.List;

public class MechanicalPowerToPressureComponent implements Component, ProcessPart, DescribeProcess {
    @Override
    public boolean validateBeforeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerConsumerComponent consumer = workstation.getComponent(MechanicalPowerConsumerComponent.class);
        FluidPumpComponent fluidPumpComponent = workstation.getComponent(FluidPumpComponent.class);
        if (consumer != null && fluidPumpComponent != null) {
            if (consumer.currentStoredPower > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getDuration(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        return FluidTransportAuthoritySystem.UPDATE_INTERVAL;
    }

    @Override
    public void executeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerConsumerComponent consumer = workstation.getComponent(MechanicalPowerConsumerComponent.class);
        FluidPumpComponent fluidPumpComponent = workstation.getComponent(FluidPumpComponent.class);
        if (consumer != null && fluidPumpComponent != null) {
            fluidPumpComponent.pressure = Math.max(consumer.currentStoredPower, fluidPumpComponent.pressure);
            workstation.saveComponent(fluidPumpComponent);

            consumer.currentStoredPower = 0;
            workstation.saveComponent(consumer);
        }
    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
/*        FluidPumpComponent fluidPumpComponent = workstation.getComponent(FluidPumpComponent.class);
        if (fluidPumpComponent != null) {
            fluidPumpComponent.pressure = 0;
            workstation.saveComponent(fluidPumpComponent);
        }*/
    }

    @Override
    public Collection<ProcessPartDescription> getOutputDescriptions() {
        List<ProcessPartDescription> descriptions = Lists.newLinkedList();
        descriptions.add(new ProcessPartDescription(null, "1 psi"));
        return descriptions;
    }

    @Override
    public Collection<ProcessPartDescription> getInputDescriptions() {
        List<ProcessPartDescription> descriptions = Lists.newLinkedList();
        descriptions.add(new ProcessPartDescription(null, "1 energy"));
        return descriptions;
    }
}
