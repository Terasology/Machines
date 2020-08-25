// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.ui;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.fluidTransport.systems.ExtendedFluidManager;
import org.terasology.rendering.nui.BaseInteractionScreen;
import org.terasology.nui.databinding.ReadOnlyBinding;
import org.terasology.nui.widgets.UILabel;

public class TankScreen extends BaseInteractionScreen {

    UILabel fluidContainerWidget;

    @Override
    public void initialise() {
        fluidContainerWidget = find("fluidWidget", UILabel.class);
    }

    @Override
    public boolean isModal() {
        return false;
    }

    @Override
    protected void initializeWithInteractionTarget(EntityRef interactionTarget) {
        if (ExtendedFluidManager.isTank(interactionTarget)) {
            fluidContainerWidget.bindText(new ReadOnlyBinding<String>() {
                @Override
                public String get() {
                    float tankFluidVolume = ExtendedFluidManager.getTankFluidVolume(getInteractionTarget(), true);
                    float tankTotalVolume = ExtendedFluidManager.getTankTotalVolume(getInteractionTarget(), true);
                    return tankFluidVolume + "/" + tankTotalVolume + "mL";
                }
            });
        }
    }
}
