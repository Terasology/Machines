/*
 * Copyright 2015 MovingBlocks
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
import org.terasology.assets.ResourceUrn;
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
import org.terasology.fluid.system.FluidRegistry;
import org.terasology.fluid.system.FluidRenderer;
import org.terasology.fluidTransport.components.FluidDisplayComponent;
import org.terasology.fluidTransport.components.FluidTankDisplayComponent;
import org.terasology.itemRendering.components.RenderItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector2f;
import org.terasology.math.geom.Vector3f;
import org.terasology.registry.In;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.assets.material.MaterialData;
import org.terasology.rendering.assets.mesh.Mesh;
import org.terasology.rendering.assets.mesh.MeshBuilder;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.rendering.nui.layers.ingame.inventory.GetItemTooltip;
import org.terasology.rendering.nui.widgets.TooltipLine;
import org.terasology.world.block.regions.BlockRegionComponent;

import java.util.Optional;

@RegisterSystem(RegisterMode.CLIENT)
public class FluidTankClientSystem extends BaseComponentSystem {
    @In
    EntityManager entityManager;
    @In
    FluidRegistry fluidRegistry;

    @ReceiveEvent
    public void onTankChanged(OnChangedComponent event, EntityRef entityRef,
                              FluidTankDisplayComponent fluidTankDisplayComponent,
                              FluidInventoryComponent fluidInventoryComponent) {
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef);
        String tankFluidType = ExtendedFluidManager.getTankFluidType(entityRef);
        if (tankFluidVolume == 0) {
            entityRef.removeComponent(FluidDisplayComponent.class);
        } else {
            setDisplayMesh(entityRef, tankFluidVolume / tankTotalVolume, tankFluidType);
        }
    }

    @ReceiveEvent
    public void onTankActivated(OnActivatedComponent event, EntityRef entityRef,
                                FluidTankDisplayComponent fluidTankDisplayComponent,
                                FluidInventoryComponent fluidInventoryComponent) {
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef);
        String tankFluidType = ExtendedFluidManager.getTankFluidType(entityRef);
        if (tankFluidVolume > 0) {
            setDisplayMesh(entityRef, tankFluidVolume / tankTotalVolume, tankFluidType);
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

    private void setDisplayMesh(EntityRef entity, float fullness, String tankFluidType) {
        if (tankFluidType == null) {
            return;
        }

        FluidDisplayComponent fluidDisplayComponent = entity.getComponent(FluidDisplayComponent.class);
        if (fluidDisplayComponent == null) {
            fluidDisplayComponent = new FluidDisplayComponent();
            EntityBuilder renderedEntityBuilder = entityManager.newBuilder();
            renderedEntityBuilder.setOwner(entity);
            fluidDisplayComponent.renderedEntity = renderedEntityBuilder.build();
            entity.addComponent(fluidDisplayComponent);
        }

        EntityRef renderedEntity = fluidDisplayComponent.renderedEntity;

        // get an existing material for this fluid type,  or generate a new one.
        ResourceUrn materialUrn = new ResourceUrn("Machines", "FluidTank", tankFluidType);
        Optional<Material> material = Assets.getMaterial(materialUrn.toString());
        if (!material.isPresent()) {
            FluidRenderer fluidRenderer = fluidRegistry.getFluidRenderer(tankFluidType);
            Texture texture = fluidRenderer.getTexture().getTexture();
            MaterialData terrainMatData = new MaterialData(Assets.getShader("engine:genericMeshMaterial").get());
            terrainMatData.setParam("diffuse", texture);
            terrainMatData.setParam("colorOffset", new float[]{1, 1, 1});
            terrainMatData.setParam("textured", true);
            material = Optional.of(Assets.generateAsset(materialUrn, terrainMatData, Material.class));
        }

        MeshComponent meshComponent = new MeshComponent();
        meshComponent.mesh = getMesh(fullness, entity);
        meshComponent.material = material.get();
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

    private Mesh getMesh(float fullness, EntityRef entityRef) {
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

        Vector3f size;
        Vector3f min;
        if (entityRef.hasComponent(BlockRegionComponent.class)) {
            BlockRegionComponent blockRegion = entityRef.getComponent(BlockRegionComponent.class);
            LocationComponent location = entityRef.getComponent(LocationComponent.class);
            size = blockRegion.region.size().toVector3f();
            min = new Vector3f(blockRegion.region.min().toVector3f()).sub(location.getWorldPosition());

        } else {
            size = Vector3f.one();
            min = Vector3f.zero();
        }
        // move from block grid coordinates into world space
        min.sub(0.5f, 0.5f, 0.5f);

        // bring it away from the block's edges
        min.add(0.01f, 0.01f, 0.01f);
        size.sub(0.02f, 0.02f, 0.02f);

        // deal will fullness
        size.mulY(fullness);

        meshBuilder.addBox(min, size, 1f, 1f);
        return meshBuilder.build();
    }


    @ReceiveEvent
    public void getDurabilityItemTooltip(GetItemTooltip event, EntityRef entityRef, FluidInventoryComponent fluidInventoryComponent) {
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef);
        String tankFluidType = ExtendedFluidManager.getTankFluidType(entityRef);
        String fluidDisplay = tankFluidType == null ? "Fluid" : fluidRegistry.getFluidRenderer(tankFluidType).getFluidName();
        event.getTooltipLines().add(new TooltipLine(fluidDisplay + ": " + tankFluidVolume + "/" + tankTotalVolume));
    }

}
