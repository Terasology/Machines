// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.world;

import com.google.common.collect.Maps;
import org.terasology.engine.math.Pitch;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.Yaw;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockBuilderHelper;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.AbstractBlockFamily;
import org.terasology.engine.world.block.family.BlockPlacementData;
import org.terasology.engine.world.block.family.BlockSections;
import org.terasology.engine.world.block.family.MultiSection;
import org.terasology.engine.world.block.family.MultiSections;
import org.terasology.engine.world.block.family.RegisterBlockFamily;
import org.terasology.engine.world.block.family.SideDefinedBlockFamily;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.gestalt.naming.Name;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

@RegisterBlockFamily("surfacePlacement")
@BlockSections({"front", "left", "right", "back", "top", "bottom", "archetype"})
@MultiSections({
        @MultiSection(name = "all", coversSection = "null", appliesToSections = {"front", "left", "right", "back", "top", "bottom"}),
        @MultiSection(name = "topBottom", coversSection = "null", appliesToSections = {"top", "bottom"}),
        @MultiSection(name = "sides", coversSection = "null", appliesToSections = {"front", "left", "right", "back"})
})
public class SurfacePlacementFamily extends AbstractBlockFamily implements SideDefinedBlockFamily {
    private final Map<Side, Block> blocks = Maps.newEnumMap(Side.class);
    private final Block archetype;

    public SurfacePlacementFamily(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder) {
        super(family, blockBuilder);

        ArrayList<Block> blocksBySide = new ArrayList<>();


        Block archetypeBlock = blockBuilder.constructSimpleBlock(family, "archetype", new BlockUri(family.getUrn()), this);
        blocksBySide.add(blockBuilder.constructTransformedBlock(family, "front", Rotation.none(), new BlockUri(family.getUrn(), new Name(Side.FRONT.name())), this));
        blocksBySide.add(blockBuilder.constructTransformedBlock(family, "left", Rotation.rotate(Yaw.CLOCKWISE_90), new BlockUri(family.getUrn(), new Name(Side.LEFT.name())), this));
        blocksBySide.add(blockBuilder.constructTransformedBlock(family, "back", Rotation.rotate(Yaw.CLOCKWISE_180), new BlockUri(family.getUrn(), new Name(Side.BACK.name())), this));
        blocksBySide.add(blockBuilder.constructTransformedBlock(family, "right", Rotation.rotate(Yaw.CLOCKWISE_270), new BlockUri(family.getUrn(), new Name(Side.RIGHT.name())), this));
        blocksBySide.add(blockBuilder.constructTransformedBlock(family, "top", Rotation.rotate(Pitch.CLOCKWISE_90), new BlockUri(family.getUrn(), new Name(Side.TOP.name())), this));
        blocksBySide.add(blockBuilder.constructTransformedBlock(family, "bottom", Rotation.rotate(Pitch.CLOCKWISE_270), new BlockUri(family.getUrn(), new Name(Side.BOTTOM.name())), this));
        BlockUri familyUri = new BlockUri(family.getUrn());

        for (Block block : blocksBySide) {
            blocks.put(block.getDirection(), block);
        }

        if (!blocksBySide.contains(archetypeBlock)) {
            archetypeBlock.setBlockFamily(this);
            archetypeBlock.setUri(new BlockUri(familyUri, new Name("archetype")));
        }

        archetype = archetypeBlock;
    }

    @Override
    public Block getBlockForPlacement(BlockPlacementData data) {
        return blocks.get(data.attachmentSide);
    }

    @Override
    public Block getArchetypeBlock() {
        return archetype;
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                Side side = Side.valueOf(blockUri.getIdentifier().toString().toUpperCase(Locale.ENGLISH));
                return blocks.get(side);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Iterable<Block> getBlocks() {
        return blocks.values();
    }

    @Override
    public Block getBlockForSide(Side side) {
        return blocks.get(side);
    }

    @Override
    public Side getSide(Block block) {
        for (Map.Entry<Side, Block> sideBlockEntry : blocks.entrySet()) {
            if (sideBlockEntry.getValue() == block) {
                return sideBlockEntry.getKey();
            }
        }

        return null;
    }
}
