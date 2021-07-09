// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.itemRendering.systems.RenderOwnedEntityDetails;

/**
 * Add this to a block that you want items displayed from an inventory category.
 * Also add the RenderItemComponent to adjust the location of the item,  otherwise it will be in the center of the containing block.
 */
public class RenderInventoryInCategoryComponent extends RenderOwnedEntityDetails implements Component<RenderInventoryInCategoryComponent> {
    public String category;
    public boolean isOutputCategory;

    @Override
    public void copy(RenderInventoryInCategoryComponent other) {
        this.category = other.category;
        this.isOutputCategory = other.isOutputCategory;
    }
}
