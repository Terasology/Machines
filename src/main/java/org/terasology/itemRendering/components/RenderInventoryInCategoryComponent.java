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
import org.terasology.math.Pitch;
import org.terasology.world.block.items.BlockItemComponent;

import javax.vecmath.Vector3f;

/**
 * Add this to a block that you want items displayed from an inventory category.
 * Also add the RenderItemComponent to adjust the location of the item,  otherwise it will be in the center of the containing block.
 */
public class RenderInventoryInCategoryComponent implements Component {
    public String category;
    public Vector3f translate = new Vector3f();
    public float blockSize = 0.3f;
    public float itemSize = 0.3f;
    public boolean itemsAreFlat;
    public boolean verticalAlignmentBottom;
    public boolean rotateWithBlock;

    public RenderItemComponent createRenderItemComponent(EntityRef referenceBlock, EntityRef item) {
        RenderItemComponent renderItem = new RenderItemComponent();

        boolean isBlockItem = item.hasComponent(BlockItemComponent.class);

        if (itemsAreFlat && !isBlockItem) {
            // make it flat
            renderItem.pitch = Pitch.CLOCKWISE_90;
        }

        renderItem.translate = new Vector3f(translate);
        if (verticalAlignmentBottom) {
            if (!isBlockItem && itemsAreFlat) {
                // shift items up half their thickness
                renderItem.translate.y += 0.125f * 0.25f;
            } else {
                renderItem.translate.y += blockSize * 0.5f;
            }
        }

        if (isBlockItem) {
            renderItem.size = blockSize;
        } else {
            renderItem.size = itemSize;
        }

        return renderItem;
    }
}
