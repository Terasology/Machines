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
package org.terasology.machines.ui;


import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluid.system.FluidRegistry;
import org.terasology.fluid.ui.FluidContainerWidget;
import org.terasology.math.geom.Vector2i;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.Canvas;
import org.terasology.rendering.nui.CoreWidget;
import org.terasology.rendering.nui.UIWidget;
import org.terasology.rendering.nui.layouts.ColumnLayout;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.fluid.FluidInputComponent;
import org.terasology.workstation.process.fluid.FluidOutputComponent;
import org.terasology.workstation.ui.WorkstationUI;

import java.util.Collections;
import java.util.Iterator;

public abstract class FluidsWidget extends CoreWidget implements WorkstationUI {
    String inventoryCategory;
    boolean isOutput;
    ColumnLayout content;

    protected FluidsWidget(String inventoryCategory) {
        this.inventoryCategory = inventoryCategory;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (content != null) {
            canvas.drawWidget(content);
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        if (content != null) {
            return canvas.calculateRestrictedSize(content, sizeHint);
        }
        return Vector2i.zero();
    }

    @Override
    public Iterator<UIWidget> iterator() {
        if (content != null) {
            return content.iterator();
        }
        return Collections.emptyIterator();
    }

    @Override
    public void initializeWorkstation(EntityRef workstation) {
        FluidRegistry fluidRegistry = CoreRegistry.get(FluidRegistry.class);

        content = new ColumnLayout();

        FluidInventoryComponent fluidInventoryComponent = workstation.getComponent(FluidInventoryComponent.class);
        if (fluidInventoryComponent != null) {
            Iterable<Integer> slots = null;
            if (isOutput) {
                slots = WorkstationInventoryUtils.getAssignedOutputSlots(workstation, FluidOutputComponent.FLUIDOUTPUTCATEGORY);
            } else {
                slots = WorkstationInventoryUtils.getAssignedInputSlots(workstation, FluidInputComponent.FLUIDINPUTCATEGORY);
            }

            for (int slot : slots) {
                FluidContainerWidget fluidContainerWidget = new FluidContainerWidget();
                fluidContainerWidget.setSlotNo(slot);
                fluidContainerWidget.setEntity(workstation);

                fluidContainerWidget.setMaxX(fluidContainerWidget.getImage().getWidth());
                fluidContainerWidget.setMinY(fluidContainerWidget.getImage().getHeight());

                content.addWidget(fluidContainerWidget);
            }
        }
    }
}
