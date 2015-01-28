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
package org.terasology.fluidTransport.systems;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.registry.In;
import org.terasology.world.OnChangedBlock;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.liquid.LiquidData;
import org.terasology.world.liquid.LiquidType;

@RegisterSystem(RegisterMode.AUTHORITY)
public class WaterFixingAuthoritySystem extends BaseComponentSystem {
    @In
    BlockManager blockManager;
    @In
    WorldProvider worldProvider;

    @ReceiveEvent
    public void setWaterLiquidLevel(OnChangedBlock event, EntityRef entityRef, BlockComponent block) {
        Block waterBlock = blockManager.getBlock("Core:Water");
        if (event.getNewType().equals(waterBlock)) {
            worldProvider.setLiquid(block.getPosition(),
                    new LiquidData(LiquidType.WATER, LiquidData.MAX_LIQUID_DEPTH),
                    worldProvider.getLiquid(block.getPosition()));
        } else if (event.getOldType().equals(waterBlock)) {

            worldProvider.setLiquid(block.getPosition(),
                    new LiquidData(),
                    worldProvider.getLiquid(block.getPosition()));
        }
    }

}
