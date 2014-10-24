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
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluidTransport.components.FluidDisplayComponent;
import org.terasology.fluidTransport.components.FluidTankDisplayComponent;
import org.terasology.itemRendering.components.RenderItemComponent;
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
    public void onTankChanged(OnChangedComponent event, EntityRef entityRef, FluidTankDisplayComponent fluidTankDisplayComponent, FluidInventoryComponent fluidInventoryComponent) {
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef);
        if (tankFluidVolume == 0) {
            entityRef.removeComponent(FluidDisplayComponent.class);
        } else {
            setDisplayMesh(entityRef, tankFluidVolume / tankTotalVolume);
        }
    }

    @ReceiveEvent
    public void onTankActivated(OnActivatedComponent event, EntityRef entityRef, FluidTankDisplayComponent fluidTankDisplayComponent, FluidInventoryComponent fluidInventoryComponent) {
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef);
        if (tankFluidVolume > 0) {
            setDisplayMesh(entityRef, tankFluidVolume / tankTotalVolume);
        }
    }

    @ReceiveEvent
    public void removeRenderedTank(BeforeDeactivateComponent event, EntityRef entityRef, FluidTankDisplayComponent fluidTankDisplayComponent) {
        entityRef.removeComponent(FluidDisplayComponent.class);
    }

    @ReceiveEvent
    public void removeRenderedFluid(BeforeDeactivateComponent event, EntityRef entityRef, FluidDisplayComponent fluidDisplayComponent) {
        if (fluidDisplayComponent.renderedEntity != null) {
            fluidDisplayComponent.renderedEntity.destroy();
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

        MeshComponent meshComponent = new MeshComponent();
        meshComponent.mesh = getMesh(fullness);
        meshComponent.material = Assets.getMaterial("engine:default");
        if (renderedEntity.hasComponent(MeshComponent.class)) {
            renderedEntity.saveComponent(meshComponent);
        } else {
            renderedEntity.addComponent(meshComponent);
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
