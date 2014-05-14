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

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.workstation.process.DescribeProcess;
import org.terasology.workstation.process.ProcessPart;

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
        }
    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {

    }

    @Override
    public String getOutputDescription() {
        return null;
    }

    @Override
    public String getInputDescription() {
        return power + " energy";
    }

    @Override
    public int getComplexity() {
        return 0;
    }
}
