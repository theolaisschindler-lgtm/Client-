package dev.amethyst.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.amethyst.client.config.ModConfig;
import dev.amethyst.client.alerts.DiscoveryAlerts;
import dev.amethyst.client.pvp.CombatActions;
import dev.amethyst.client.pvp.PvpAutomation;
import dev.amethyst.client.pvp.SafeAnchorController;
import dev.amethyst.client.gui.ClickGuiScreen;
import dev.amethyst.client.gui.effects.GuiEffects;
import dev.amethyst.client.freecam.FreecamController;
import dev.amethyst.client.render.HighlightRenderer;
import dev.amethyst.client.scan.BlockScanner;
import dev.amethyst.client.scan.ChunkScanner;
import dev.amethyst.client.scan.Highlight;
import dev.amethyst.client.scan.OreScanner;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AmethystClient implements ClientModInitializer {
    private static ModConfig config;
    private static final BlockScanner SCANNER = new BlockScanner();
    private static final ChunkScanner CHUNKS = new ChunkScanner();
    private static final OreScanner ORES = new OreScanner();
    private static final DiscoveryAlerts DISCOVERIES = new DiscoveryAlerts();

    public static String chunkStatus() { return CHUNKS.status(); }
    public static ModConfig config() { return config; }
    public static List<Highlight> highlights() {
        return Stream.of(SCANNER.highlights(), CHUNKS.highlights(), ORES.highlights()).flatMap(List::stream).toList();
    }

    @Override
    public void onInitializeClient() {
        config = ModConfig.load(FabricLoader.getInstance().getConfigDir().resolve("amethyst.json"));
        GuiEffects.initialize();
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("amethyst", "client"));
        KeyMapping openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.amethyst.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        KeyMapping toggleFreecam = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.amethyst.toggle_freecam", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, category));
        HighlightRenderer.register();
        FreecamController.register();
        SafeAnchorController.register();
        ClientTickEvents.START_CLIENT_TICK.register(client -> CombatActions.beginTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PvpAutomation.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.consumeClick()) {
                if (client.screen == null) client.setScreen(new ClickGuiScreen());
            }
            while (toggleFreecam.consumeClick()) {
                if (client.screen == null && client.level != null && client.player != null) {
                    config.freecam.enabled = !config.freecam.enabled;
                }
            }
            SCANNER.tick(client, config);
            CHUNKS.tick(client, config.chunks);
            DISCOVERIES.tick(client, config.chunks, CHUNKS.highlights());
            ORES.tick(client, config.ores);
            PvpAutomation.tick(client);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            FreecamController.disable();
            config.save();
        });
    }
}
