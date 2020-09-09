// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.events;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.Event;

public class RequirementUsedEvent implements Event {
    EntityRef processEntity;

    public RequirementUsedEvent(EntityRef processEntity) {
        this.processEntity = processEntity;
    }

    public EntityRef getProcessEntity() {
        return processEntity;
    }
}
