// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.components;

import org.terasology.engine.entitySystem.Component;

public class SideActivatedInventoryInteractionComponent implements Component {
    public String direction;
    public String inputType;
    public Boolean inputIsOutputType;
    public String outputType;
    public Boolean outputIsOutputType;

}
