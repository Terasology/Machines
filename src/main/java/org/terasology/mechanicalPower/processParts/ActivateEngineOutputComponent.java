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
import org.terasology.mechanicalPower.components.MechanicalPowerProducerComponent;
import org.terasology.workstation.process.ProcessPart;

public class ActivateEngineOutputComponent implements Component, ProcessPart {
    public long activateTime;

    @Override
    public boolean validateBeforeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerProducerComponent producer = workstation.getComponent(MechanicalPowerProducerComponent.class);
        if (producer != null && !producer.active) {
            return true;
        }
        return false;
    }

    @Override
    public long getDuration(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        return activateTime;
    }

    @Override
    public void executeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerProducerComponent producer = workstation.getComponent(MechanicalPowerProducerComponent.class);
        if (producer != null) {
            producer.active = true;
            workstation.saveComponent(producer);
        }
    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerProducerComponent producer = workstation.getComponent(MechanicalPowerProducerComponent.class);
        if (producer != null) {
            producer.active = false;
            workstation.saveComponent(producer);
        }
    }
}
