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
package org.terasology.itemRendering.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.Pitch;

import javax.vecmath.Vector3f;

/**
 * Add this to a block that you want items displayed from an inventory category.
 * Also add the RenderItemTransformComponent to adjust the location of the item,  otherwise it will be in the center of the containing block.
 */
public class RenderInventoryInCategoryComponent implements Component {
    public String category;
    public Vector3f translate = new Vector3f();
    public float size = 0.3f;
    public boolean itemsAreFlat = true;

    public RenderItemTransformComponent createRenderItemTransformComponent(EntityRef referenceBlock, EntityRef item) {
        RenderItemTransformComponent renderItemTransform = new RenderItemTransformComponent();
        if (itemsAreFlat && item.hasComponent(ItemComponent.class)) {
            // make it flat
            renderItemTransform.pitch = Pitch.CLOCKWISE_90;
        }
        renderItemTransform.translate = translate;
        renderItemTransform.size = size;

        return renderItemTransform;
    }
}
