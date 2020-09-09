// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.itemRendering.systems.RenderOwnedEntityDetails;

/**
 * Add this to a block that you want items displayed from an inventory category. Also add the RenderItemComponent to
 * adjust the location of the item,  otherwise it will be in the center of the containing block.
 */
public class RenderInventoryInCategoryComponent extends RenderOwnedEntityDetails implements Component {
    public String category;
    public boolean isOutputCategory;
}
