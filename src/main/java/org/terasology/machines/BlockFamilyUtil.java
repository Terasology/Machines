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
package org.terasology.machines;

import org.terasology.math.Side;
import org.terasology.world.block.Block;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.family.SideDefinedBlockFamily;

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
