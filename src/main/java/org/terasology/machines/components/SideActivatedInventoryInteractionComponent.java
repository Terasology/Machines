// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class SideActivatedInventoryInteractionComponent implements Component<SideActivatedInventoryInteractionComponent> {
    public String direction;
    public String inputType;
    public Boolean inputIsOutputType;
    public String outputType;
    public Boolean outputIsOutputType;

    @Override
    public void copy(SideActivatedInventoryInteractionComponent other) {
        this.direction = other.direction;
        this.inputType = other.inputType;
        this.inputIsOutputType = other.inputIsOutputType;
        this.outputType = other.outputType;
        this.outputIsOutputType = other.outputIsOutputType;
    }
}
