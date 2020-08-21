// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.ui;


import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.fluid.system.FluidRegistry;
import org.terasology.fluid.ui.FluidContainerWidget;
import org.joml.Vector2i;
import org.terasology.registry.CoreRegistry;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.UIWidget;
import org.terasology.nui.layouts.ColumnLayout;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.fluid.FluidInputProcessPartCommonSystem;
import org.terasology.workstation.process.fluid.FluidOutputProcessPartCommonSystem;
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
        return new Vector2i();
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
                slots = WorkstationInventoryUtils.getAssignedOutputSlots(workstation, FluidOutputProcessPartCommonSystem.FLUIDOUTPUTCATEGORY);
            } else {
                slots = WorkstationInventoryUtils.getAssignedInputSlots(workstation, FluidInputProcessPartCommonSystem.FLUIDINPUTCATEGORY);
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
