// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.mechanicalPower.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.entityNetwork.components.SidedBlockLocationNetworkNodeComponent;
import org.terasology.mechanicalPower.systems.MechanicalPowerAuthoritySystem;

@ForceBlockActive
public class MechanicalPowerSidedBlockNetworkComponent extends SidedBlockLocationNetworkNodeComponent implements Component {
    public MechanicalPowerSidedBlockNetworkComponent() {
        networkId = MechanicalPowerAuthoritySystem.NETWORK_ID;
    }
}
