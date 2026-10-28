package dev.amethyst.client.gui;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.config.ModConfig;
import dev.amethyst.client.config.ModuleSettings;
import dev.amethyst.client.config.TargetType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;

/** A self-contained, responsive screen. All positions are in Minecraft's GUI coordinates. */
public final class ClickGuiScreen extends Screen {
    private static final int BACKGROUND = 0xFF111016;
    private static final int SIDEBAR = 0xFF15131D;
    private static final int CARD = 0xFF1B1923;
    private static final int BORDER = 0xFF2B2737;
    private static final int TEXT = 0xFFEDEAF5;
    private static final int MUTED = 0xFF918A9F;
    private final ModConfig config = AmethystClient.config();
    private final List<Hit> hits = new ArrayList<>();
    private int selectedModule;
    private boolean appearance;
    private TargetType selectedColor = TargetType.CHEST;
    private Hit dragging;
    private int focused = -1;
    private double scroll;
    private int maxScroll;
    private int viewportX, viewportY, viewportW, viewportH;
    private int mouseX, mouseY;

    public ClickGuiScreen() {
        super(Component.literal("Amethyst • Visual modules"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        hits.clear();
        g.fill(0, 0, width, height, 0x8807060B);
        int w = Math.min(870, width - 16);
        int h = Math.min(550, height - 16);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        boolean compact = w < 640;
        int side = compact ? 42 : 145;
        round(g, x - 4, y - 2, w + 8, h + 8, 10, 0x42000000);
        round(g, x, y, w, h, 8, BACKGROUND);
        round(g, x, y, side, h, 8, SIDEBAR);
        g.fill(x + side - 8, y, x + side, y + h, SIDEBAR);
        g.fill(x + side, y + 1, x + side + 1, y + h - 1, BORDER);
        sidebar(g, x, y, side, h, compact);

        int left = x + side + 15;
        int available = w - side - 30;
        text(g, appearance ? "Appearance" : "Visuals", left, y + 18, TEXT);
        if (available > 280) text(g, appearance ? "Make your client feel like you." : "A little clarity. A different perspective.", left, y + 33, MUTED);
        int enabled = (config.storage.enabled ? 1 : 0) + (config.spawners.enabled ? 1 : 0) + (config.chunks.enabled ? 1 : 0) + (config.freecam.enabled ? 1 : 0);
        if (available > 360) pill(g, left + available - 108, y + 15, 80, 18, enabled + " / 4 active", 0xFF24202F, accent());
        button(g, x + w - 28, y + 11, 20, 20, "×", this::onClose, false);
        g.fill(left, y + 52, left + available, y + 53, BORDER);

        if (appearance) {
            viewportX = left;
            viewportY = y + 64;
            viewportW = available;
            viewportH = h - 88;
            settingsViewport(g, true);
        } else {
            boolean columns = available >= 500 && h >= 400;
            int listW = columns ? 200 : available;
            if (columns) {
                text(g, "MODULES", left, y + 65, MUTED);
                text(g, "04", left + listW - 13, y + 65, MUTED);
                int cardHeight = Math.clamp((h - 140) / 4, 64, 96);
                for (int index = 0; index < 4; index++)
                    moduleCard(g, left, y + 85 + index * (cardHeight + 8), listW, cardHeight, index);
                viewportX = left + listW + 14;
                viewportY = y + 64;
                viewportW = available - listW - 14;
                viewportH = h - 88;
            } else {
                int tabWidth = (available - 18) / 4;
                String[] tabs = {"Storage", tabWidth < 62 ? "Spawn" : "Spawners", "Chunks", "Freecam"};
                for (int index = 0; index < tabs.length; index++) {
                    int module = index;
                    button(g, left + index * (tabWidth + 6), y + 62, tabWidth, 25,
                            tabs[index], () -> selectModule(module), selectedModule == module);
                }
                viewportX = left;
                viewportY = y + 97;
                viewportW = available;
                viewportH = h - 121;
            }
            settingsViewport(g, false);
        }
        text(g, "Changes apply live", left, y + h - 15, MUTED);
        if (available > 320) rightText(g, "ESC to close  ·  Saved automatically", left + available, y + h - 15, MUTED);
        if (focused >= 0 && focused < hits.size()) {
            Hit hit = hits.get(focused);
            outline(g, hit.x - 2, hit.y - 2, hit.w + 4, hit.h + 4, accent());
        }
    }

    private void sidebar(GuiGraphics g, int x, int y, int w, int h, boolean compact) {
        round(g, x + 11, y + 14, 20, 20, 7, 0xFF322448);
        diamond(g, x + 21, y + 24, 5, accent());
        if (!compact) {
            text(g, "AMETHYST", x + 39, y + 16, TEXT);
            text(g, "Visual client / 1.2", x + 39, y + 28, MUTED);
            if (h >= 230) text(g, "WORKSPACE", x + 13, y + 63, 0xFF625B70);
        }
        int navY = h < 230 ? 55 : 85;
        nav(g, x + 7, y + navY, w - 14, "Visuals", false, compact);
        nav(g, x + 7, y + navY + 31, w - 14, "Appearance", true, compact);
        round(g, x + 7, y + h - 43, w - 14, 35, 5, 0xFF1D1927);
        circle(g, x + 16, y + h - 27, 2, 0xFF8BDBB4);
        if (!compact) {
            text(g, "Amethyst", x + 24, y + h - 34, TEXT);
            text(g, "Fabric 1.21.11", x + 24, y + h - 21, MUTED);
        }
    }

    private void nav(GuiGraphics g, int x, int y, int w, String label, boolean isAppearance, boolean compact) {
        boolean selected = appearance == isAppearance;
        if (selected || hover(x, y, w, 27)) round(g, x, y, w, 27, 4, selected ? 0xFF282036 : 0xFF1C1826);
        if (selected) g.fill(x, y + 6, x + 2, y + 21, accent());
        if (isAppearance) diamond(g, x + 13, y + 13, 4, selected ? accent() : MUTED);
        else {
            outline(g, x + 9, y + 9, 8, 8, selected ? accent() : MUTED);
            g.fill(x + 12, y + 6, x + 19, y + 7, selected ? accent() : MUTED);
        }
        if (!compact) text(g, label, x + 27, y + 10, selected ? TEXT : MUTED);
        hit(x, y, w, 27, () -> { appearance = isAppearance; scroll = 0; focused = -1; });
    }

    private void moduleCard(GuiGraphics g, int x, int y, int w, int h, int index) {
        boolean enabled = moduleEnabled(index);
        boolean selected = selectedModule == index;
        String name = switch (index) {
            case 0 -> "Storage ESP";
            case 1 -> "Spawner Finder";
            case 2 -> "Chunk Finder";
            default -> "Freecam";
        };
        String category = switch (index) {
            case 0 -> "WORLD / STORAGE";
            case 1 -> "WORLD / DISCOVERY";
            case 2 -> "WORLD / CHUNKS";
            default -> "CAMERA / FREE FLIGHT";
        };
        round(g, x, y, w, h, 6, selected ? 0xFF221C30 : hover(x, y, w, h) ? 0xFF201C2A : CARD);
        if (selected) {
            outline(g, x, y, w, h, 0xFF57416E);
            g.fill(x, y + 11, x + 2, y + h - 11, accent());
        }
        round(g, x + 12, y + 12, 26, 26, 5, 0xFF332642);
        if (index == 2) {
            diamond(g, x + 25, y + 25, 6, accent());
            outline(g, x + 16, y + 16, 18, 18, 0xFF66537D);
        } else if (index == 3) {
            outline(g, x + 16, y + 20, 13, 10, accent());
            g.fill(x + 29, y + 23, x + 34, y + 28, accent());
        } else boxIcon(g, x + 17, y + 18, 15, accent());
        text(g, name, x + 47, y + 17, TEXT);
        if (h >= 90) text(g, category, x + 12, y + 49, MUTED);
        pill(g, x + 12, y + h - 25, 52, 14, enabled ? "ENABLED" : "DISABLED", 0xFF16121F, enabled ? accent() : MUTED);
        text(g, "Configure  ›", x + w - 74, y + h - 21, selected ? accent() : MUTED);
        hit(x, y, w, h, () -> selectModule(index));
        toggle(g, x + w - 38, y + 15, enabled, () -> toggleModule(index), false);
    }

    private boolean moduleEnabled(int index) {
        return switch (index) {
            case 0 -> config.storage.enabled;
            case 1 -> config.spawners.enabled;
            case 2 -> config.chunks.enabled;
            default -> config.freecam.enabled;
        };
    }

    private void toggleModule(int index) {
        switch (index) {
            case 0 -> config.storage.enabled = !config.storage.enabled;
            case 1 -> config.spawners.enabled = !config.spawners.enabled;
            case 2 -> config.chunks.enabled = !config.chunks.enabled;
            default -> config.freecam.enabled = !config.freecam.enabled;
        }
    }

    private void selectModule(int module) {
        selectedModule = module;
        selectedColor = module == 0 ? TargetType.CHEST : module == 1 ? TargetType.SPAWNER : TargetType.GEODE;
        scroll = 0;
        maxScroll = 0;
        dragging = null;
        focused = -1;
    }

    private void settingsViewport(GuiGraphics g, boolean theme) {
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        g.enableScissor(viewportX, viewportY, viewportX + viewportW, viewportY + viewportH);
        int end = theme ? appearanceSettings(g, viewportX, viewportY - (int) scroll, viewportW - 6)
                : moduleSettings(g, viewportX, viewportY - (int) scroll, viewportW - 6);
        g.disableScissor();
        maxScroll = Math.max(0, end + (int) scroll - viewportY - viewportH + 8);
        if (maxScroll > 0) {
            int track = viewportH - 4;
            int thumb = Math.max(18, track * viewportH / (viewportH + maxScroll));
            int sy = viewportY + (int) (scroll / maxScroll * (track - thumb));
            round(g, viewportX + viewportW - 2, sy, 2, thumb, 1, 0xFF665477);
        }
    }

    private int moduleSettings(GuiGraphics g, int x, int y, int w) {
        if (selectedModule == 3) return freecamSettings(g, x, y, w);
        boolean chunkSelected = selectedModule == 2;
        boolean storageSelected = selectedModule == 0;
        ModuleSettings module = chunkSelected ? config.chunks : storageSelected ? config.storage : config.spawners;
        text(g, "MODULE SETTINGS", x, y + 2, MUTED);
        text(g, chunkSelected ? "Chunk Finder" : storageSelected ? "Storage ESP" : "Spawner Finder", x, y + 22, TEXT);
        toggle(g, x + w - 28, y + 18, module.enabled, () -> module.enabled = !module.enabled, true);
        text(g, chunkSelected ? "Geodes & possible activity clues." : storageSelected ? "Outline the things you keep." : "Reveal nearby monster spawners.", x, y + 38, MUTED);
        int yy = y + 61;
        if (chunkSelected) {
            yy = section(g, "CHUNK SCANNING", x, yy, w);
            yy = slider(g, x, yy, w, "Sensitivity", config.chunks.sensitivity + "%", config.chunks.sensitivity, 1, 100, v -> config.chunks.sensitivity = (int) Math.round(v));
            yy = paragraph(g, "Higher sensitivity accepts weaker clues and may produce more false positives.", x, yy, w, MUTED);
            yy = slider(g, x, yy, w, "Simulation distance", config.chunks.simulationDistance + " chunks", config.chunks.simulationDistance, 1, 16, v -> config.chunks.simulationDistance = (int) Math.round(v));
            yy = paragraph(g, "Scan radius in loaded chunks. Does not change the server's simulation distance.", x, yy, w, MUTED);
            yy = paragraph(g, "Natural geodes are separate from possible activity clues. Neither proves a player was here.", x, yy, w, MUTED);
            yy = paragraph(g, "Outlines mark candidate chunk sections.", x, yy, w, MUTED);
            yy = paragraph(g, AmethystClient.chunkStatus(), x, yy, w, MUTED) + 6;
        }
        yy = section(g, "RENDERING", x, yy, w);
        yy = switchRow(g, x, yy, w, "Through walls", module.throughWalls, () -> module.throughWalls = !module.throughWalls);
        yy = switchRow(g, x, yy, w, "Rainbow colors", module.rainbow, () -> module.rainbow = !module.rainbow);
        yy = switchRow(g, x, yy, w, "Pulse opacity", module.pulse, () -> module.pulse = !module.pulse);
        yy += 5;
        if (!chunkSelected) yy = slider(g, x, yy, w, "Render distance", module.range + " blocks", module.range, 16, 256, value -> module.range = (int) Math.round(value));
        yy = slider(g, x, yy, w, "Outline opacity", Math.round(module.opacity * 100) + "%", module.opacity, 0.05, 1, value -> module.opacity = (float) value);
        yy = slider(g, x, yy, w, "Outline width", String.format(Locale.ROOT, "%.1f px", module.lineWidth), module.lineWidth, 1, 5, value -> module.lineWidth = (float) (Math.round(value * 4) / 4.0));
        yy += 8;
        yy = section(g, "TARGETS & COLORS", x, yy, w);
        text(g, "Toggle a target. Click its color to edit.", x, yy, MUTED);
        yy += 19;
        for (TargetType type : TargetType.values()) {
            if (config.module(type) != module) continue;
            boolean enabled = module.targets.contains(type);
            round(g, x, yy, w, 26, 4, selectedColor == type ? 0xFF292132 : CARD);
            round(g, x + 8, yy + 7, 12, 12, 3, enabled ? accent() : 0xFF34303D);
            if (enabled) {
                g.fill(x + 11, yy + 12, x + 14, yy + 14, 0xFF17111F);
                g.fill(x + 13, yy + 10, x + 17, yy + 12, 0xFF17111F);
            }
            text(g, type.label(), x + 28, yy + 9, enabled ? TEXT : MUTED);
            hitVisible(x, yy, w - 52, 26, () -> { if (!module.targets.remove(type)) module.targets.add(type); });
            int color = 0xFF000000 | module.colors.getOrDefault(type, config.color(type));
            round(g, x + w - 41, yy + 6, 30, 14, 3, color);
            hitVisible(x + w - 49, yy, 49, 26, () -> selectedColor = type);
            yy += 30;
        }
        yy += 5;
        int rgb = module.colors.getOrDefault(selectedColor, 0x9B7CFF);
        round(g, x, yy, w, 37, 4, 0xFF211C2A);
        round(g, x + 9, yy + 9, 18, 18, 4, 0xFF000000 | rgb);
        text(g, selectedColor.label() + " outline", x + 36, yy + 7, TEXT);
        text(g, String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF), x + 36, yy + 21, MUTED);
        yy += 47;
        yy = colorSlider(g, x, yy, w, "Red", rgb, 16, value -> setChannel(module, 16, value));
        rgb = module.colors.getOrDefault(selectedColor, rgb);
        yy = colorSlider(g, x, yy, w, "Green", rgb, 8, value -> setChannel(module, 8, value));
        rgb = module.colors.getOrDefault(selectedColor, rgb);
        yy = colorSlider(g, x, yy, w, "Blue", rgb, 0, value -> setChannel(module, 0, value));
        yy += 6;
        text(g, "PALETTES", x, yy, MUTED);
        yy += 17;
        int chip = (w - 12) / 3;
        visibleButton(g, x, yy, chip, 23, "Amethyst", () -> palette(module, 0), false);
        visibleButton(g, x + chip + 6, yy, chip, 23, "Arctic", () -> palette(module, 1), false);
        visibleButton(g, x + 2 * (chip + 6), yy, chip, 23, "Sunset", () -> palette(module, 2), false);
        yy += 34;
        if (module.rainbow) {
            text(g, "Rainbow is active; saved colors are retained.", x, yy, MUTED);
            yy += 17;
        }
        text(g, "Only blocks received from the server appear.", x, yy, MUTED);
        return yy + 15;
    }

    private int freecamSettings(GuiGraphics g, int x, int y, int w) {
        text(g, "MODULE SETTINGS", x, y + 2, MUTED);
        text(g, "Freecam", x, y + 22, TEXT);
        toggle(g, x + w - 28, y + 18, config.freecam.enabled,
                () -> config.freecam.enabled = !config.freecam.enabled, true);
        text(g, "A different point of view.", x, y + 38, MUTED);
        int yy = section(g, "FREE FLIGHT", x, y + 61, w);
        yy = slider(g, x, yy, w, "Movement speed",
                String.format(Locale.ROOT, "%.2f blocks/tick", config.freecam.speed),
                config.freecam.speed, 0.1, 3.0, value -> config.freecam.speed = Math.round(value * 100) / 100.0);
        yy = slider(g, x, yy, w, "Sprint boost",
                String.format(Locale.ROOT, "%.1f x", config.freecam.boostMultiplier),
                config.freecam.boostMultiplier, 1.0, 5.0,
                value -> config.freecam.boostMultiplier = Math.round(value * 10) / 10.0);
        yy += 8;
        yy = section(g, "CONTROLS", x, yy, w);
        yy = paragraph(g, "Use your movement keys to fly. Jump goes up; Sneak goes down. Hold Sprint to boost.", x, yy, w, TEXT);
        yy = paragraph(g, "F6 toggles Freecam. Rebind it in Minecraft's Controls menu.", x, yy, w, MUTED);
        yy = paragraph(g, "Close this menu to move the camera. Camera movement pauses while a screen is open.", x, yy, w, MUTED);
        yy += 8;
        yy = section(g, "CAMERA SESSION", x, yy, w);
        yy = paragraph(g, "Your body remains in the world and can still take damage or move with gravity. Disable Freecam to return to it.", x, yy, w, MUTED);
        yy = paragraph(g, "Freecam starts off each session. Speed and boost are saved automatically.", x, yy, w, MUTED);
        return yy + 8;
    }

    private int appearanceSettings(GuiGraphics g, int x, int y, int w) {
        text(g, "YOUR WORKSPACE", x, y + 2, MUTED);
        text(g, "A touch of color", x, y + 23, TEXT);
        text(g, "Choose an accent for the interface.", x, y + 41, MUTED);
        int yy = y + 65;
        int[] accents = {0x9B7CFF, 0x6ED9DF, 0xF08DB5, 0x8DD7A5, 0xF2BB75, 0xA5B8EB};
        int swatch = Math.min(36, (w - 25) / 6);
        for (int i = 0; i < accents.length; i++) {
            int color = accents[i];
            int xx = x + i * (swatch + 5);
            round(g, xx, yy, swatch, 28, 5, 0xFF000000 | color);
            if (config.accent == color) outline(g, xx - 2, yy - 2, swatch + 4, 32, TEXT);
            hitVisible(xx, yy, swatch, 28, () -> config.accent = color);
        }
        yy += 49;
        yy = slider(g, x, yy, w, "Accent red", Integer.toString((config.accent >> 16) & 255), (config.accent >> 16) & 255, 0, 255, value -> setAccent(16, value));
        yy = slider(g, x, yy, w, "Accent green", Integer.toString((config.accent >> 8) & 255), (config.accent >> 8) & 255, 0, 255, value -> setAccent(8, value));
        yy = slider(g, x, yy, w, "Accent blue", Integer.toString(config.accent & 255), config.accent & 255, 0, 255, value -> setAccent(0, value));
        yy += 12;
        yy = section(g, "THE DETAILS", x, yy, w);
        text(g, "Right Shift opens this workspace.", x, yy, TEXT);
        text(g, "Change the key in Minecraft's Controls menu.", x, yy + 17, MUTED);
        text(g, "Tab to focus  ·  Enter to toggle", x, yy + 42, MUTED);
        text(g, "Arrow keys adjust focused sliders.", x, yy + 59, MUTED);
        text(g, "Settings are saved when you close this screen.", x, yy + 84, MUTED);
        return yy + 105;
    }

    private void setChannel(ModuleSettings module, int shift, double value) {
        int rgb = module.colors.getOrDefault(selectedColor, 0x9B7CFF);
        module.colors.put(selectedColor, (rgb & ~(255 << shift)) | ((int) Math.round(value) << shift));
    }

    private void setAccent(int shift, double value) {
        config.accent = (config.accent & ~(255 << shift)) | ((int) Math.round(value) << shift);
    }

    private void palette(ModuleSettings module, int palette) {
        int[][] colors = {{0xB497FF, 0xDAB0FF, 0x80C8FF, 0xF49FE0, 0xFF6F98},
                {0x78D9F5, 0xA2F1DD, 0x8BAEFA, 0xD2B2FF, 0x56E5E2},
                {0xFFC078, 0xF0A267, 0xFF8585, 0xE48FCD, 0xFF637E}};
        for (TargetType type : TargetType.values()) {
            if (config.module(type) == module) module.colors.put(type, colors[palette][type.ordinal() % colors[palette].length]);
        }
    }

    private int section(GuiGraphics g, String label, int x, int y, int w) {
        text(g, label, x, y, MUTED);
        int labelWidth = font.width(label) + 12;
        g.fill(x + labelWidth, y + 4, x + w, y + 5, BORDER);
        return y + 20;
    }

    private int switchRow(GuiGraphics g, int x, int y, int w, String label, boolean state, Runnable action) {
        text(g, label, x, y + 6, TEXT);
        toggle(g, x + w - 28, y + 3, state, action, true);
        return y + 27;
    }

    private int colorSlider(GuiGraphics g, int x, int y, int w, String label, int rgb, int shift, DoubleConsumer consumer) {
        int value = (rgb >> shift) & 255;
        return slider(g, x, y, w, label, Integer.toString(value), value, 0, 255, consumer);
    }

    private int slider(GuiGraphics g, int x, int y, int w, String label, String valueText, double value, double min, double max, DoubleConsumer consumer) {
        text(g, label, x, y, TEXT);
        rightText(g, valueText, x + w, y, MUTED);
        int trackY = y + 19;
        round(g, x, trackY, w, 3, 1, 0xFF332D40);
        int fill = (int) Math.round((value - min) / (max - min) * (w - 4));
        round(g, x, trackY, Math.max(2, fill), 3, 1, accent());
        circle(g, x + fill + 2, trackY + 1, 4, 0xFFE5DAFF);
        if (trackY >= viewportY && trackY + 3 < viewportY + viewportH) {
            int hitY = Math.max(trackY - 6, viewportY);
            int hitBottom = Math.min(trackY + 9, viewportY + viewportH);
            hits.add(new Hit(x, hitY, w, hitBottom - hitY, null, consumer, min, max, value));
        }
        return y + 37;
    }

    private void toggle(GuiGraphics g, int x, int y, boolean state, Runnable action, boolean clipped) {
        round(g, x, y, 28, 14, 7, state ? accent() : 0xFF39323F);
        circle(g, x + (state ? 21 : 7), y + 7, 5, state ? 0xFFF2EEFF : 0xFF9B92AA);
        if (clipped) hitVisible(x, y, 28, 14, action);
        else hit(x, y, 28, 14, action);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, Runnable action, boolean selected) {
        round(g, x, y, w, h, 4, selected ? 0xFF332542 : hover(x, y, w, h) ? 0xFF332C41 : 0xFF231E2D);
        text(g, label, x + Math.max(3, (w - font.width(label)) / 2), y + (h - 8) / 2, selected ? accent() : TEXT);
        hit(x, y, w, h, action);
    }

    private void visibleButton(GuiGraphics g, int x, int y, int w, int h, String label, Runnable action, boolean selected) {
        int before = hits.size();
        button(g, x, y, w, h, label, action, selected);
        if (y < viewportY || y + h > viewportY + viewportH) hits.remove(before);
    }

    private void pill(GuiGraphics g, int x, int y, int w, int h, String label, int background, int color) {
        round(g, x, y, w, h, 4, background);
        text(g, label, x + Math.max(3, (w - font.width(label)) / 2), y + (h - 8) / 2, color);
    }

    private void hit(int x, int y, int w, int h, Runnable action) {
        hits.add(new Hit(x, y, w, h, action, null, 0, 0, 0));
    }

    private void hitVisible(int x, int y, int w, int h, Runnable action) {
        int top = Math.max(y, viewportY);
        int bottom = Math.min(y + h, viewportY + viewportH);
        if (bottom > top) hit(x, top, w, bottom - top, action);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (hit.contains(event.x(), event.y())) {
                    focused = i;
                    if (hit.slider != null) { dragging = hit; drag(event.x()); }
                    else hit.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) { drag(event.x()); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    private void drag(double x) {
        double progress = Math.max(0, Math.min(1, (x - dragging.x) / Math.max(1, dragging.w - 1)));
        dragging.slider.accept(dragging.min + progress * (dragging.max - dragging.min));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) { dragging = null; return true; }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= viewportX && x <= viewportX + viewportW && y >= viewportY && y <= viewportY + viewportH) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - vertical * 24));
            focused = -1;
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        if (event.key() == GLFW.GLFW_KEY_TAB && !hits.isEmpty()) {
            focused = Math.floorMod(focused + ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1), hits.size());
            return true;
        }
        if (focused >= 0 && focused < hits.size()) {
            Hit hit = hits.get(focused);
            if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) && hit.action != null) {
                hit.action.run();
                return true;
            }
            if (hit.slider != null && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT)) {
                double step = selectedModule == 3 && !appearance ? (hit.max == 5 ? 0.1 : 0.05)
                        : hit.max == 5 ? 0.25 : hit.max >= 16 ? 1 : 0.01;
                hit.slider.accept(Math.max(hit.min, Math.min(hit.max, hit.value + (event.key() == GLFW.GLFW_KEY_RIGHT ? step : -step))));
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        config.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private int accent() { return 0xFF000000 | (config.accent & 0xFFFFFF); }
    private boolean hover(int x, int y, int w, int h) { return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h; }
    private void text(GuiGraphics g, String text, int x, int y, int color) { g.drawString(font, text, x, y, color, false); }
    private int paragraph(GuiGraphics g, String value, int x, int y, int width, int color) {
        for (var line : font.split(Component.literal(value), Math.max(40, width))) {
            g.drawString(font, line, x, y, color, false);
            y += 12;
        }
        return y + 5;
    }

    private void rightText(GuiGraphics g, String text, int x, int y, int color) { text(g, text, x - font.width(text), y, color); }

    private static void round(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.min(radius, Math.min(w, h) / 2);
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            int inset = r - (int) Math.sqrt(r * r - (r - row - 0.5) * (r - row - 0.5));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + 1, y + h - 2, color);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
    }

    private static void circle(GuiGraphics g, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            g.fill(x - dx, y + dy, x + dx + 1, y + dy + 1, color);
        }
    }

    private static void diamond(GuiGraphics g, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = radius - Math.abs(dy);
            g.fill(x - dx, y + dy, x + dx + 1, y + dy + 1, color);
        }
    }

    private static void boxIcon(GuiGraphics g, int x, int y, int size, int color) {
        outline(g, x, y + 3, size - 3, size - 3, color);
        g.fill(x + 3, y, x + size, y + 1, color);
        g.fill(x + size - 1, y, x + size, y + size - 4, color);
        g.fill(x + 3, y + 6, x + size - 4, y + 7, color);
    }

    private record Hit(int x, int y, int w, int h, Runnable action, DoubleConsumer slider, double min, double max, double value) {
        boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
}
