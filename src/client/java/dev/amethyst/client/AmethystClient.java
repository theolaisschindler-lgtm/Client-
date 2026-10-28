package dev.amethyst.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.amethyst.client.config.ModConfig;
import dev.amethyst.client.gui.ClickGuiScreen;
import dev.amethyst.client.render.HighlightRenderer;
import dev.amethyst.client.scan.BlockScanner;
import dev.amethyst.client.scan.Highlight;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AmethystClient implements ClientModInitializer {
    private static ModConfig config;
    private static final BlockScanner SCANNER = new BlockScanner();

    public static ModConfig config() { return config; }
    public static List<Highlight> highlights() { return SCANNER.highlights(); }

    @Override
    public void onInitializeClient() {
        config = ModConfig.load(FabricLoader.getInstance().getConfigDir().resolve("amethyst.json"));
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("amethyst", "client"));
        KeyMapping openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.amethyst.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        HighlightRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.consumeClick()) {
                if (client.screen == null) client.setScreen(new ClickGuiScreen());
            }
            SCANNER.tick(client, config);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());
    }
}
