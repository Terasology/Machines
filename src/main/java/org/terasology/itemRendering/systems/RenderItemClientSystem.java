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
package org.terasology.itemRendering.systems;

import org.terasology.asset.Assets;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.itemRendering.components.CustomRenderedItemMeshComponent;
import org.terasology.itemRendering.components.RenderItemComponent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Rotation;
import org.terasology.registry.In;
import org.terasology.rendering.iconmesh.IconMeshFactory;
import org.terasology.rendering.logic.LightComponent;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemComponent;

import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.CLIENT)
public class RenderItemClientSystem extends BaseComponentSystem {

    Random rand;

    @In
    WorldProvider worldProvider;

    @Override
    public void initialise() {
        rand = new FastRandom();
    }

    @ReceiveEvent
    public void onChangedItemDisplay(OnChangedComponent event, EntityRef entity, RenderItemComponent itemDisplay) {
        LocationComponent location = entity.getComponent(LocationComponent.class);
        if (location != null) {
            updateLocation(entity, itemDisplay, location);
        }
    }

    private void updateLocation(EntityRef entity, RenderItemComponent itemDisplay, LocationComponent location) {
        location.setLocalScale(itemDisplay.size);
        Rotation rotation = Rotation.rotate(itemDisplay.yaw, itemDisplay.pitch, itemDisplay.roll);
        entity.saveComponent(location);
        Location.attachChild(entity.getOwner(), entity, itemDisplay.translate, rotation.getQuat4f());
    }

    @ReceiveEvent
    public void onAddedItemDisplay(OnAddedComponent event, EntityRef entity, RenderItemComponent itemDisplay) {
        LocationComponent locationComponent = entity.getOwner().getComponent(LocationComponent.class);

        if (locationComponent != null) {
            if (!entity.hasComponent(MeshComponent.class)) {
                if (entity.hasComponent(CustomRenderedItemMeshComponent.class)) {
                    addCustomItemRendering(entity);
                } else if (entity.hasComponent(BlockItemComponent.class)) {
                    addBlockRendering(entity);
                } else {
                    addItemRendering(entity);
                }
            }

            updateLocation(entity, itemDisplay, new LocationComponent());
        }
    }

    private void addCustomItemRendering(EntityRef entity) {
        CustomRenderedItemMeshComponent customRenderedItemMeshComponent = entity.getComponent(CustomRenderedItemMeshComponent.class);
        MeshComponent meshComponent = new MeshComponent();
        meshComponent.mesh = customRenderedItemMeshComponent.mesh;
        meshComponent.material = customRenderedItemMeshComponent.material;
        entity.addComponent(meshComponent);
    }


    private void addItemRendering(EntityRef entityRef) {
        ItemComponent itemComponent = entityRef.getComponent(ItemComponent.class);
        if (itemComponent != null) {
            MeshComponent meshComponent = new MeshComponent();
            meshComponent.material = Assets.getMaterial("engine:droppedItem");
            if (itemComponent.icon != null) {
                meshComponent.mesh = IconMeshFactory.getIconMesh(itemComponent.icon);
            }
            entityRef.addComponent(meshComponent);
        }
    }

    private void addBlockRendering(EntityRef entityRef) {
        MeshComponent mesh = new MeshComponent();
        BlockItemComponent blockItemComponent = entityRef.getComponent(BlockItemComponent.class);
        BlockFamily blockFamily = blockItemComponent.blockFamily;

        if (blockFamily == null) {
            return;
        }

        mesh.mesh = blockFamily.getArchetypeBlock().getMesh();
        mesh.material = Assets.getMaterial("engine:terrain");

        if (blockFamily.getArchetypeBlock().getLuminance() > 0 && !entityRef.hasComponent(LightComponent.class)) {
            LightComponent lightComponent = entityRef.addComponent(new LightComponent());

            Vector3f randColor = new Vector3f(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
            lightComponent.lightColorDiffuse.set(randColor);
            lightComponent.lightColorAmbient.set(randColor);
        }

        entityRef.addComponent(mesh);
    }

    @ReceiveEvent
    public void onRemoveItemDisplay(BeforeDeactivateComponent event, EntityRef entity, RenderItemComponent itemDisplay) {
        Location.removeChild(entity.getOwner(), entity);
        entity.removeComponent(LocationComponent.class);
        entity.removeComponent(MeshComponent.class);
    }
}
