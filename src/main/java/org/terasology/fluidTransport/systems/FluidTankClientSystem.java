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
package org.terasology.fluidTransport.systems;

import org.terasology.asset.Assets;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.fluid.component.FluidComponent;
import org.terasology.fluidTransport.components.FluidDisplayComponent;
import org.terasology.fluidTransport.components.FluidTankComponent;
import org.terasology.itemRendering.components.CustomRenderedItemMeshComponent;
import org.terasology.itemRendering.components.RenderItemComponent;
import org.terasology.mechanicalPower.components.RotatingAxleComponent;
import org.terasology.registry.In;
import org.terasology.rendering.assets.mesh.Mesh;
import org.terasology.rendering.assets.mesh.MeshBuilder;
import org.terasology.rendering.logic.MeshComponent;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.CLIENT)
public class FluidTankClientSystem extends BaseComponentSystem {
    @In
    EntityManager entityManager;

    @ReceiveEvent
    public void onFluidChanged(OnChangedComponent event, EntityRef entityRef, FluidComponent fluidComponent, FluidTankComponent fluidTankComponent, FluidDisplayComponent fluidDisplayComponent) {
        MeshComponent meshComponent = fluidDisplayComponent.renderedEntity.getComponent(MeshComponent.class);
        meshComponent.mesh = getMesh(fluidComponent.volume / fluidTankComponent.maximumVolume);
    }

    @ReceiveEvent
    public void onFluidAdded(OnAddedComponent event, EntityRef entityRef, FluidComponent fluidComponent, FluidTankComponent fluidTankComponent) {
        setDisplayMesh(entityRef, fluidComponent.volume / fluidTankComponent.maximumVolume);
    }


    @ReceiveEvent
    public void removeRenderedAxle(BeforeDeactivateComponent event, EntityRef entityRef, RotatingAxleComponent rotatingAxle) {
        if (rotatingAxle.renderedEntity != null) {
            rotatingAxle.renderedEntity.destroy();
        }
    }

    private void setDisplayMesh(EntityRef entity, float fullness) {
        FluidDisplayComponent fluidDisplayComponent = entity.getComponent(FluidDisplayComponent.class);
        if (fluidDisplayComponent == null) {
            fluidDisplayComponent = new FluidDisplayComponent();
            EntityBuilder renderedEntityBuilder = entityManager.newBuilder();
            renderedEntityBuilder.setOwner(entity);
            fluidDisplayComponent.renderedEntity = renderedEntityBuilder.build();
            entity.addComponent(fluidDisplayComponent);
        }

        EntityRef renderedEntity = fluidDisplayComponent.renderedEntity;

        CustomRenderedItemMeshComponent customRenderedItemMeshComponent = new CustomRenderedItemMeshComponent();
        customRenderedItemMeshComponent.mesh = getMesh(fullness);
        customRenderedItemMeshComponent.material = Assets.getMaterial("engine:default");
        if (renderedEntity.hasComponent(CustomRenderedItemMeshComponent.class)) {
            renderedEntity.saveComponent(customRenderedItemMeshComponent);
        } else {
            renderedEntity.addComponent(customRenderedItemMeshComponent);
        }

        RenderItemComponent renderItemComponent = new RenderItemComponent();
        renderItemComponent.size = 1;
        if (renderedEntity.hasComponent(RenderItemComponent.class)) {
            renderedEntity.saveComponent(renderItemComponent);
        } else {
            renderedEntity.addComponent(renderItemComponent);
        }

    }

    private Mesh getMesh(float fullness) {
        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.setTextureMapper(new MeshBuilder.TextureMapper() {
            @Override
            public void initialize(Vector3f offset, Vector3f size) {

            }

            @Override
            public Vector2f map(int vertexIndex, float u, float v) {
                return new Vector2f(1f, 1f);
            }
        });
        meshBuilder.addBox(new Vector3f(-0.49f, -0.49f, -0.49f), new Vector3f(0.98f, fullness - 0.02f, 0.98f), 1f, 1f);
        return meshBuilder.build();
    }

}
