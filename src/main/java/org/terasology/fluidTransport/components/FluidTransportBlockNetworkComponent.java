// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.fluidTransport.components;

import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.entityNetwork.components.BlockLocationNetworkNodeComponent;
import org.terasology.fluidTransport.systems.FluidTransportAuthoritySystem;
import org.terasology.gestalt.entitysystem.component.Component;

@ForceBlockActive
public class FluidTransportBlockNetworkComponent extends BlockLocationNetworkNodeComponent implements Component<FluidTransportBlockNetworkComponent extends BlockLocationNetworkNodeComponent> {
    public FluidTransportBlockNetworkComponent() {
        this.networkId = FluidTransportAuthoritySystem.NETWORK_ID;
    }
}
