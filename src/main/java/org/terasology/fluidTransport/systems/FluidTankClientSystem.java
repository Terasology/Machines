// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.systems;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.terasology.engine.entitySystem.entity.EntityBuilder;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.assets.material.Material;
import org.terasology.engine.rendering.assets.material.MaterialData;
import org.terasology.engine.rendering.assets.mesh.Mesh;
import org.terasology.engine.rendering.assets.mesh.MeshBuilder;
import org.terasology.engine.rendering.assets.texture.Texture;
import org.terasology.engine.rendering.logic.MeshComponent;
import org.terasology.engine.utilities.Assets;
import org.terasology.engine.world.block.regions.BlockRegionComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluid.system.FluidContainerAssetResolver;
import org.terasology.fluid.system.FluidRegistry;
import org.terasology.fluidTransport.components.FluidDisplayComponent;
import org.terasology.fluidTransport.components.FluidTankDisplayComponent;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.itemRendering.components.RenderItemComponent;
import org.terasology.module.inventory.ui.GetItemTooltip;
import org.terasology.nui.widgets.TooltipLine;

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
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef, true);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef, true);
        String tankFluidType = ExtendedFluidManager.getTankFluidType(entityRef, true);
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
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef, true);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef, true);
        String tankFluidType = ExtendedFluidManager.getTankFluidType(entityRef, true);
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
            Texture texture = Assets.getTexture(FluidContainerAssetResolver.getFluidBaseUri(tankFluidType)).get();
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
            public void initialize(Vector3fc offset, Vector3fc size) {

            }

            @Override
            public Vector2f map(int vertexIndex, float u, float v) {
                switch (vertexIndex) {
                    // Front face
                    case  0 : return new Vector2f(0f, 1f);
                    case  1 : return new Vector2f(1f, 1f);
                    case  2 : return new Vector2f(1f, 1 - fullness);
                    case  3 : return new Vector2f(0f, 1 - fullness);
                    // Back face
                    case  4 : return new Vector2f(1f, 1f);
                    case  5 : return new Vector2f(1f, 1 - fullness);
                    case  6 : return new Vector2f(0f, 1 - fullness);
                    case  7 : return new Vector2f(0f, 1f);
                    // Top face
                    case  8 : return new Vector2f(1f, 0f);
                    case  9 : return new Vector2f(1f, 1f);
                    case 10 : return new Vector2f(0f, 1f);
                    case 11 : return new Vector2f(0f, 0f);
                    // Bottom face
                    case 12 : return new Vector2f(1f, 0f);
                    case 13 : return new Vector2f(0f, 0f);
                    case 14 : return new Vector2f(0f, 1f);
                    case 15 : return new Vector2f(1f, 1f);
                    // Right face
                    case 16 : return new Vector2f(1f, 1f);
                    case 17 : return new Vector2f(1f, 1 - fullness);
                    case 18 : return new Vector2f(0f, 1 - fullness);
                    case 19 : return new Vector2f(0f, 1f);
                    // Left face
                    case 20 : return new Vector2f(0f, 0f);
                    case 21 : return new Vector2f(1f, 0f);
                    case 22 : return new Vector2f(1f, fullness);
                    case 23 : return new Vector2f(0f, fullness);

                    default : throw new RuntimeException("Unreachable state.");
                }
            }
        });

        Vector3f size;
        Vector3f min;
        if (entityRef.hasComponent(BlockRegionComponent.class)) {
            BlockRegionComponent blockRegion = entityRef.getComponent(BlockRegionComponent.class);
            LocationComponent location = entityRef.getComponent(LocationComponent.class);
            size = new Vector3f(blockRegion.region.getSize(new Vector3i()));
            min = new Vector3f(blockRegion.region.getMin(new Vector3i())).sub(location.getWorldPosition(new Vector3f()));

        } else {
            size = new Vector3f(1, 1, 1);
            min = new Vector3f();
        }
        // move from block grid coordinates into world space
        min.sub(0.5f, 0.5f, 0.5f);

        // bring it away from the block's edges
        min.add(0.01f, 0.01f, 0.01f);
        size.sub(0.02f, 0.02f, 0.02f);

        // deal will fullness
        size.mul(1, fullness, 1);

        meshBuilder.addBox(min, size, 1f, 1f);
        return meshBuilder.build();
    }


    @ReceiveEvent
    public void getItemTooltip(GetItemTooltip event, EntityRef entityRef, FluidInventoryComponent fluidInventoryComponent) {
        float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(entityRef, true);
        float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(entityRef, true);
        String tankFluidType = ExtendedFluidManager.getTankFluidType(entityRef, true);
        String fluidDisplay = tankFluidType == null ? "Fluid" : fluidRegistry.getDisplayName(tankFluidType);
        event.getTooltipLines().add(new TooltipLine(fluidDisplay + ": " + tankFluidVolume + "/" + tankTotalVolume));
    }

}
