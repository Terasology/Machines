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
package org.terasology.mechanicalPower.processParts;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.workstation.process.DescribeProcess;
import org.terasology.workstation.process.ProcessPart;
import org.terasology.workstation.process.ProcessPartDescription;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MechanicalPowerInputComponent implements Component, ProcessPart, DescribeProcess {
    public float power;

    @Override
    public boolean validateBeforeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerConsumerComponent consumer = workstation.getComponent(MechanicalPowerConsumerComponent.class);
        if (consumer != null) {
            if (consumer.currentStoredPower >= power) {
                return true;
            }
        }

        consumer = instigator.getComponent(MechanicalPowerConsumerComponent.class);
        if (consumer != null) {
            if (consumer.currentStoredPower >= power) {
                return true;
            }
        }

        return false;
    }

    @Override
    public long getDuration(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        return 0;
    }

    @Override
    public void executeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerConsumerComponent consumer = workstation.getComponent(MechanicalPowerConsumerComponent.class);
        if (consumer != null) {
            consumer.currentStoredPower -= power;
            workstation.saveComponent(consumer);
            return;
        }

        consumer = workstation.getComponent(MechanicalPowerConsumerComponent.class);
        if (consumer != null) {
            consumer.currentStoredPower -= power;
            workstation.saveComponent(consumer);
            return;
        }
    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {

    }

    @Override
    public Collection<ProcessPartDescription> getInputDescriptions() {
        List<ProcessPartDescription> descriptions = Lists.newLinkedList();
        descriptions.add(new ProcessPartDescription(null, power + " energy"));
        return descriptions;
    }

    @Override
    public Collection<ProcessPartDescription> getOutputDescriptions() {
        return Collections.emptyList();
    }
}
