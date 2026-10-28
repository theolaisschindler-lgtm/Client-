package dev.amethyst.client.scan;

import dev.amethyst.client.config.TargetType;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreScannerTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void normalAndDeepslateVariantsShareTheirIndividualOreColorCategory() {
        Block[][] variants = {
                {Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE},
                {Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE},
                {Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE},
                {Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE},
                {Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE},
                {Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE},
                {Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE},
                {Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE}
        };
        TargetType[] types = {TargetType.ORE_COAL, TargetType.ORE_COPPER, TargetType.ORE_IRON,
                TargetType.ORE_GOLD, TargetType.ORE_REDSTONE, TargetType.ORE_LAPIS,
                TargetType.ORE_DIAMOND, TargetType.ORE_EMERALD};
        for (int i = 0; i < variants.length; i++) for (Block block : variants[i])
            assertEquals(types[i], OreScanner.classify(block), block.toString());
    }

    @Test void netherOresAreRecognizedWithoutTreatingTerrainOrStorageAsOre() {
        assertEquals(TargetType.ORE_GOLD, OreScanner.classify(Blocks.NETHER_GOLD_ORE));
        assertEquals(TargetType.ORE_QUARTZ, OreScanner.classify(Blocks.NETHER_QUARTZ_ORE));
        assertEquals(TargetType.ORE_ANCIENT_DEBRIS, OreScanner.classify(Blocks.ANCIENT_DEBRIS));
        for (Block block : new Block[] {Blocks.STONE, Blocks.DEEPSLATE, Blocks.NETHERRACK,
                Blocks.AIR, Blocks.DIAMOND_BLOCK, Blocks.RAW_IRON_BLOCK, Blocks.AMETHYST_BLOCK,
                Blocks.BUDDING_AMETHYST, Blocks.CHEST, Blocks.REDSTONE_WIRE})
            assertNull(OreScanner.classify(block), block.toString());
    }

    @Test void rangeIsSphericalAndIncludesItsExactBoundaryAtNegativeCoordinates() {
        assertTrue(OreScanner.withinRange(new BlockPos(-64, -20, 0), 0.5, -19.5, 0.5, 64));
        assertFalse(OreScanner.withinRange(new BlockPos(-65, -20, 0), 0.5, -19.5, 0.5, 64));
        assertFalse(OreScanner.withinRange(new BlockPos(50, 50, 50), 0.5, 0.5, 0.5, 64));
        assertTrue(OreScanner.withinRange(new BlockPos(0, 64, 0), 0.5, 0.5, 0.5, 64));
    }
}
