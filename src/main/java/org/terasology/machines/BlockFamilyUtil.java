// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines;

import org.terasology.engine.math.Side;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.block.family.SideDefinedBlockFamily;

public class BlockFamilyUtil {
    public static Side getSideDefinedDirection(Block block) {
        Side blockDirection = Side.FRONT;
        BlockFamily blockFamily = block.getBlockFamily();
        if (blockFamily instanceof SideDefinedBlockFamily) {
            blockDirection = ((SideDefinedBlockFamily) blockFamily).getSide(block);
        }
        return blockDirection;
    }
}
