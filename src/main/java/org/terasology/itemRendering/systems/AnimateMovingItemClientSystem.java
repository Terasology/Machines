/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.itemRendering.systems;

import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.itemRendering.components.AnimatedMovingItemComponent;
import org.terasology.itemRendering.components.RenderItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;

import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.CLIENT)
public class AnimateMovingItemClientSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    //static final long UPDATE_INTERVAL = 10;

    @In
    Time time;
    @In
    EntityManager entityManager;

    Random rand;
    long nextUpdateTime;

    @Override
    public void initialise() {
        rand = new FastRandom();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void update(float delta) {
        for (EntityRef entity : entityManager.getEntitiesWith(AnimatedMovingItemComponent.class, LocationComponent.class, RenderItemComponent.class)) {
            AnimatedMovingItemComponent animatedMovingItemComponent = entity.getComponent(AnimatedMovingItemComponent.class);
            LocationComponent locationComponent = entity.getComponent(LocationComponent.class);
            RenderItemComponent renderItemComponent = entity.getComponent(RenderItemComponent.class);

            updateItemLocation(locationComponent, animatedMovingItemComponent, renderItemComponent);
            entity.saveComponent(locationComponent);
        }
    }

    @ReceiveEvent
    public void onUpdateMovingItem(OnChangedComponent event,
                                   EntityRef entityRef,
                                   AnimatedMovingItemComponent animatedMovingItemComponent,
                                   RenderItemComponent renderItemComponent) {
        LocationComponent locationComponent = entityRef.getComponent(LocationComponent.class);
        if (locationComponent != null) {
            updateItemLocation(locationComponent, animatedMovingItemComponent, renderItemComponent);
            entityRef.saveComponent(locationComponent);
        }
    }


    private void updateItemLocation(LocationComponent locationComponent,
                                    AnimatedMovingItemComponent animatedMovingItemComponent,
                                    RenderItemComponent renderItemComponent) {
        float percentToTarget = 1.0f - (float) (animatedMovingItemComponent.arrivalTime - time.getGameTimeInMs())
                / (float) (animatedMovingItemComponent.arrivalTime - animatedMovingItemComponent.startTime);
        if (percentToTarget < 0f) {
            percentToTarget = 0f;
        }
        if (percentToTarget > 1f) {
            percentToTarget = 1f;
        }

        Vector3f relativePosition;
        if (percentToTarget > 0.5f) {
            // 0 - 50%
            relativePosition = animatedMovingItemComponent.exitSide.toDirection().getVector3f();
            percentToTarget -= 0.5f;
        } else {
            // 50% - 0
            relativePosition = animatedMovingItemComponent.entranceSide.toDirection().getVector3f();
            percentToTarget = 0.5f - percentToTarget;
        }

        relativePosition.x *= percentToTarget;
        relativePosition.y *= percentToTarget;
        relativePosition.z *= percentToTarget;

        relativePosition.add(renderItemComponent.translate);

        locationComponent.setLocalPosition(relativePosition);

    }

    @ReceiveEvent
    public void onRemoveMovingItem(BeforeDeactivateComponent event, EntityRef entityRef, RenderItemComponent renderItemTransform) {
        entityRef.removeComponent(AnimatedMovingItemComponent.class);
    }
}
