package dev.amethyst.client.gui.effects;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.amethyst.client.config.GuiEffectSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** One procedural GUI quad; the normal GUI renderer owns its buffers and resource lifetime. */
public final class GuiEffects {
    private static final int PADDING = 24;
    private static final long START_TIME = System.nanoTime();
    private static final RenderPipeline AURA = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("amethyst", "pipeline/gui_aura"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("amethyst", "core/gui_aura"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("amethyst", "core/gui_aura"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .build());

    private GuiEffects() { }

    /** Register the pipeline during client initialization, before shader/resource loading. */
    public static void initialize() { }

    public static void render(GuiGraphics graphics, int x, int y, int width, int height,
                              int accent, GuiEffectSettings settings) {
        if (!settings.glow || settings.glowIntensity <= 0 || width <= 0 || height <= 0) return;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = new ScreenRectangle(x - PADDING, y - PADDING,
                width + PADDING * 2, height + PADDING * 2).transformMaxBounds(pose);
        if (scissor != null) bounds = bounds.intersection(scissor);
        if (bounds == null) return;
        float phase = settings.animations
                ? (float) (((System.nanoTime() - START_TIME) * 0.00000000025) % (Math.PI * 2)) : 0f;
        int color = (Math.clamp((int) Math.round(settings.glowIntensity * 255), 0, 255) << 24)
                | (accent & 0xFFFFFF);
        graphics.guiRenderState.submitGuiElement(new AuraState(pose, x, y, width, height,
                phase, color, scissor, bounds));
        // Keep text, cards, and controls after the effect regardless of pipeline sorting.
        graphics.nextStratum();
    }

    private record AuraState(Matrix3x2f pose, int x, int y, int width, int height,
                             float phase, int color, ScreenRectangle scissorArea,
                             ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return AURA; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }

        @Override
        public void buildVertices(VertexConsumer vertices) {
            vertex(vertices, x - PADDING, y - PADDING, 0, 0);
            vertex(vertices, x - PADDING, y + height + PADDING, 0, 1);
            vertex(vertices, x + width + PADDING, y + height + PADDING, 1, 1);
            vertex(vertices, x + width + PADDING, y - PADDING, 1, 0);
        }

        private void vertex(VertexConsumer vertices, float px, float py, float u, float v) {
            float tx = pose.m00() * px + pose.m10() * py + pose.m20();
            float ty = pose.m01() * px + pose.m11() * py + pose.m21();
            // This 2D pipeline uses Z as animation phase and UV2 as panel dimensions.
            // Its vertex shader supplies the actual GUI depth, and samples no lightmap.
            vertices.addVertex(tx, ty, phase).setColor(color).setUv(u, v).setUv2(width, height);
        }
    }
}
