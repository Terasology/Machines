// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.transport.fluid.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.machines.entityNetwork.components.BlockLocationNetworkNodeComponent;
import org.terasology.machines.transport.fluid.systems.FluidTransportAuthoritySystem;

@ForceBlockActive
public class FluidTransportBlockNetworkComponent extends BlockLocationNetworkNodeComponent implements Component {
    public FluidTransportBlockNetworkComponent() {
        this.networkId = FluidTransportAuthoritySystem.NETWORK_ID;
    }
}
