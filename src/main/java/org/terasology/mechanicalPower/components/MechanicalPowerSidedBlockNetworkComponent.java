// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.mechanicalPower.components;

import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.entityNetwork.components.SidedBlockLocationNetworkNodeComponent;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.mechanicalPower.systems.MechanicalPowerAuthoritySystem;

@ForceBlockActive
public class MechanicalPowerSidedBlockNetworkComponent extends SidedBlockLocationNetworkNodeComponent implements Component<MechanicalPowerSidedBlockNetworkComponent extends SidedBlockLocationNetworkNodeComponent> {
    public MechanicalPowerSidedBlockNetworkComponent() {
        networkId = MechanicalPowerAuthoritySystem.NETWORK_ID;
    }
}
