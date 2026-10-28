package dev.amethyst.client.scan;

import dev.amethyst.client.config.TargetType;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChunkScannerTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void paletteFilterSkipsOrdinaryTerrainAndRetainsAllSignalFamilies() {
        assertFalse(ChunkScanner.relevantBlock(Blocks.STONE));
        assertFalse(ChunkScanner.relevantBlock(Blocks.DEEPSLATE));
        assertFalse(ChunkScanner.relevantBlock(Blocks.AIR));
        assertTrue(ChunkScanner.relevantBlock(Blocks.AMETHYST_BLOCK));
        assertTrue(ChunkScanner.relevantBlock(Blocks.BUDDING_AMETHYST));
        assertTrue(ChunkScanner.relevantBlock(Blocks.CALCITE));
        assertTrue(ChunkScanner.relevantBlock(Blocks.TORCH));
        assertTrue(ChunkScanner.relevantBlock(Blocks.CRAFTING_TABLE));
        assertTrue(ChunkScanner.relevantBlock(Blocks.RED_BED));
        assertTrue(ChunkScanner.relevantBlock(Blocks.REDSTONE_WIRE));
    }

    @Test
    void sectionMarkersContainTheirEvidenceAcrossNegativeCoordinatesAndSectionSeams() {
        ChunkPos chunk = new ChunkPos(-3, 2);
        for (int y : new int[] {-64, -17, -16, -1, 0, 15, 16, 319}) {
            BlockPos evidence = new BlockPos(-33, y, 47);
            Highlight highlight = ChunkScanner.sectionHighlight(chunk, evidence, TargetType.GEODE);
            assertEquals(TargetType.GEODE, highlight.type());
            assertEquals(16, highlight.width());
            assertEquals(16, highlight.height());
            assertEquals(16, highlight.depth());
            assertTrue(evidence.getX() >= highlight.pos().getX());
            assertTrue(evidence.getX() < highlight.pos().getX() + highlight.width());
            assertTrue(evidence.getY() >= highlight.pos().getY());
            assertTrue(evidence.getY() < highlight.pos().getY() + highlight.height());
            assertTrue(evidence.getZ() >= highlight.pos().getZ());
            assertTrue(evidence.getZ() < highlight.pos().getZ() + highlight.depth());
        }
    }
}
