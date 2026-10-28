# Amethyst

A Fabric **client mod for Minecraft Java 1.21.11** with a dark, violet click GUI inspired by the supplied reference.

## Features

- **Storage ESP:** configurable outlines for chests (including trapped and ender chests), hoppers, and shulker boxes. Barrels can be enabled separately.
- **Spawner Finder:** configurable outlines for spawners in loaded chunks.
- **Chunk Finder:** surveys loaded chunks for amethyst/calcite geode candidates and combinations of possible activity clues. Outlines mark the 16×16×16 chunk section containing a representative clue. Geodes and activity use separate configurable colors.
- **Freecam:** a detached, no-clip camera with adjustable speed and sprint boost. Toggle it under Visuals or with **F6** (rebindable in Controls).
- **Ore ESP:** individual outlines for coal, copper, iron, gold, redstone, lapis, diamond, emerald, Nether quartz, and ancient debris, including deepslate/Nether variants. Configure each color and target, scan range (16–128 blocks), and outline limit (128–4096). Diamond, emerald, gold, and ancient debris are selected initially.
- **Player ESP:** interpolated player bounding boxes with color, range, invisible/spectator filters, and optional team colors. Your own player is excluded.
- **PvP:** ten independently implemented combat modules plus Safe Anchor, Crystal ESP, and Totem Warning. See the controls and limits below. All new modules start disabled.
- Individual target colors, RGB controls, opacity, distance (16–256 blocks), line width (1–5), through-wall rendering, rainbow colors, and pulsing outlines.
- Settings persist in `config/amethyst.json` inside your Minecraft instance.
- **GUI shaders:** an animated accent-colored aura and a soft window glow, with native background blur. Toggle blur, glow, and animation and adjust glow intensity under **Appearance → GUI Shaders**. No external shader pack is required.

Press **Right Shift** in-game to open the GUI. The binding can be changed under **Options → Controls → Key Binds**. Enable a module, select the block types you want, and adjust its appearance in the settings panel. Close the GUI to save your changes.

The background blur uses Minecraft's **Menu Background Blur** strength setting and respects zero/off there. The aura uses a dedicated GLSL shader, follows the interface accent color, and runs only while the Amethyst GUI is open. Turning animations off freezes the effect; disabling glow skips its draw entirely. Other screens and the world's shaders are unchanged.

The mod reads blocks the client already knows about. It does not load distant chunks, search world seeds, or discover blocks the server has withheld. Use it only where the server's rules allow it.

### Chunk Finder

Select **Visuals → Chunk Finder** and enable the module. It starts disabled, so upgrading from 1.0.0 preserves your existing module choices.

- **Sensitivity (1–100):** higher values lower the evidence threshold. Activity requires at least two different clue categories: torches, crafting tables, beds, or redstone wiring/components. Geode candidates require amethyst plus calcite. A geode alone never counts as player activity.
- **Simulation distance (1–16 chunks):** the radius of the square survey around your current chunk. This is a client scan limit; it does not change Minecraft's or a server's actual simulation/view distance. Only chunks the client already has can be inspected.
- **Geodes / Activity clues:** independent target switches and RGB colors, plus the existing opacity, width, through-wall, rainbow, and pulse options.
- **Discovery sound:** an adjustable-volume amethyst chime when a new chunk/category candidate appears. Repeated survey results are remembered, and discoveries are grouped with a three-second cooldown. Muting consumes discoveries without replaying them later; disabling the module or changing worlds starts a new discovery session.

Surveying is incremental, so signals can take time to appear and update, especially at larger distances. The inspector shows survey progress. Natural structures, including villages, can match the activity heuristics; a candidate is not proof of a player base. The scan does not detect chunk age, ownership, or players themselves.

### Freecam

Select **Visuals → Freecam** or press **F6** in-game to detach the camera. Use your configured movement keys (normally **WASD**), **Jump** to rise, **Sneak** to descend, and **Sprint** for the adjustable boost. Look around with the mouse. Speed is measured in blocks per tick (20 ticks per second). Camera movement pauses while a GUI or chat is open.

Your real player stays in the world, with its movement controls suppressed. Gravity, server corrections, and damage still apply to the body. Camera movement does not teleport the player, request distant chunks, or let you interact with blocks from the detached camera. Disable Freecam to return to the player's current location and restore your previous perspective. It resets on death, respawn, world/dimension changes, and disconnect, and never activates automatically after restarting the game. Dismount vehicles before enabling it. Speed and boost preferences are saved normally.

### PvP

Open **PvP** in the existing violet GUI and use the page arrows to browse its 13 modules. The functions below were implemented for this project from the requested module names; they do not contain Prestige Client code or claim identical behavior.

| Module | Behavior and settings |
| --- | --- |
| Auto Crystal | Places crystals from the hotbar on valid obsidian/bedrock bases and optionally breaks existing crystals. Configure placement/break delays, range, crosshair-only placement, use-key requirement, and slot restoration. It does not place obsidian or calculate explosion damage. |
| Auto Hit Crystal | Hits existing loaded crystals. Configure delay, range, crosshair-only targeting, attack-key requirement, and line of sight. It does not place crystals. |
| Auto Totem | Moves a totem from the inventory into the offhand with a vanilla SWAP transaction; the old offhand stack returns to its source slot. Configure delay and an optional health threshold including absorption. Pauses for open screens/containers and a nonempty cursor stack. |
| Anchor Macro | Right-click a block to place an anchor, charge it once, and detonate it, or use an existing anchor. Requires an anchor and glowstone in the hotbar and a non-glowstone detonation slot. Configure action delay and slot restoration; release use before starting again. It waits for block-state updates and respects Safe Anchor. |
| Trigger Bot | Attacks the entity under the crosshair when the configured cooldown and tick delay permit. Players-only is the default; holding attack can be required. |
| W Tap | Briefly releases forward input and sprint after a sprinting hit on a living entity, then returns control to the current movement input. Configure the reset duration. |
| Auto Shield Break | While holding attack by default, equips a hotbar axe to hit a blocking player. Configure delay, attack-key requirement, and slot restoration. It uses normal attacks; shield disabling remains server-controlled. |
| Silent Aim | Redirects a manual attack toward a reachable player inside the configured FOV using ordered rotation packets, then restores the prior server rotation while keeping the local view unchanged. Configure range, FOV, line of sight, and cooldown requirement. |
| Aim Assist | Smoothly turns the visible view toward a reachable player. Configure range, FOV, degrees per tick, line of sight, and attack-key requirement. |
| Auto Mace | Equips a hotbar mace and attacks a crosshair player once per fall after the minimum fall distance. Configure range, fall threshold, attack-key requirement, and slot restoration. |
| Safe Anchor | Prevents accidental glowstone-hand use from detonating a fully charged anchor outside the Nether. Optional strict mode blocks all charged-anchor detonations. Normal charging and Nether use remain available. This is click protection, not damage prediction or a promise of survival. Strict protection also stops Anchor Macro detonation. |
| Crystal ESP | Customizable, interpolated outlines around loaded end crystals, with the same color and through-wall options as the other ESP modules. |
| Totem Warning | Local sound and action-bar warning below the chosen health threshold when neither hand holds a totem. Configure volume and repeat cooldown. It does not equip items. |

Ranges are capped by the player's actual vanilla interaction attributes; a six-block slider does not grant extra reach. Automatic combat pauses in screens, while using an item, while detached in Freecam, when inactive, or without a valid living player. A shared action budget prevents multiple automated attacks/uses in the same tick. Auto Totem has defensive priority; staged anchor/weapon sequences reserve their hotbar operation so other modules do not attack with the wrong item. Slot restoration yields to a manual slot change. The server remains authoritative for placement, reach, damage, cooldowns, and shield disabling. No anti-cheat compatibility is claimed.

ESP only displays information already sent to the client. Server-side anti-xray can hide ores from Ore ESP. Ore scanning runs incrementally with a bounded cache, so large areas take time. The nearest cached candidates are outlined up to the configured limit; dense ore chunks can exceed the per-chunk cache of 512 candidates and omit other ores.

## Install

Download [amethyst-1.4.0.jar](https://raw.githubusercontent.com/theolaisschindler-lgtm/Client-/main/downloads/amethyst-1.4.0.jar) and replace the previous Amethyst JAR in your `mods/` folder.

Use Minecraft **1.21.11**, **Java 21**, Fabric Loader **0.18.2 or newer**, and Fabric API **0.141.6+1.21.11**. The development build pins Fabric Loader **0.20.1**.

Build the project, then copy the main JAR from `build/libs/` into your Minecraft instance's `mods/` folder alongside Fabric API. Do not install the `-sources.jar` file. This is a client-only mod; the server does not need it.

## Develop

Use the existing checkout; each cloud task already has an isolated workspace.

```sh
cd /workspace/Client-
./gradlew build
```

`build` compiles and packages the mod and runs the configuration, scanner, geometry, discovery, and combat-rule tests. On Windows, use `gradlew.bat build`.

Configuration and persistence tests can also run independently of Minecraft downloads:

```sh
./gradlew -p config-tests test
```

On a development machine with EGL/OpenGL support, the optional shader check compiles and renders the actual GLSL at wide and compact sizes and verifies intensity, animation, static mode, accent color, and halo bounds:

```sh
python -m pip install moderngl==5.12.0
python tools/validate_gui_shader.py
```

This is a standalone shader check; it does not replace checking the complete GUI in Minecraft.

In this cloud instance, select the installed full JDK with `export JAVA_HOME=/workspace/.toolchains/jdk-21.0.9+10` and a writable Gradle cache with `export GRADLE_USER_HOME=/workspace/.gradle`.
Java needs explicit proxy and CA settings here. Add these arguments to Gradle commands:

```sh
-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
```

To start the development client on a machine with a graphical display:

```sh
./gradlew runClient
```

The development client uses a separate `run/` instance. A successful build does not establish in-game rendering compatibility: check the GUI and all modules in Minecraft using the steps below. Launching a playable client requires a display and downloaded Minecraft assets; multiplayer authentication may require a normal authenticated launcher session.

## In-game verification

1. Enter a creative test world and place a chest, trapped chest, ender chest, hopper, shulker box, barrel, and spawner.
2. Open the GUI with Right Shift. Enable Storage ESP and confirm its default storage types are outlined; enable barrels and check that they appear too.
3. Enable Spawner Finder and confirm the spawner uses its own color.
4. Change individual colors, opacity, line width, distance, rainbow, pulse, and through-wall rendering. Check the result both in front of and behind a wall.
5. Close and reopen the GUI, then restart the client, to confirm settings persist. Break a highlighted block and leave the area to check stale outlines disappear.
6. Enable Chunk Finder. In separate chunks place an amethyst/calcite cluster, torches alone, and a combination of torches, crafting tables, beds, and redstone. Wait for the survey; confirm the geode and activity categories have distinct section outlines and lone torches do not count as activity.
7. Compare sensitivity1 and100 with a weak combination (one torch and one crafting table). Change simulation distance, disable each target, unload a candidate chunk, and change dimensions; confirm old outlines clear. Check the controls and persistence in both wide and compact GUI layouts.
8. Toggle Freecam with F6 and in the GUI. Fly in every direction, look up/down, test sprint boost and passage through walls. Check that your body does not follow the camera or receive its rotation. Open chat/the GUI and confirm camera movement pauses. Return to the body from each perspective and check death, respawn, dimension change, and disconnect cleanup. Verify block attacks/use are suppressed while detached and work again after disabling.
9. Enable Ore ESP near normal/deepslate/Nether ore variants, change each target/color and outline limit, then break an ore and unload the chunk. Check Player ESP on another player while standing, sneaking, invisible, spectating, and moving; test team colors and your own exclusion.
10. Enable Chunk Finder's discovery sound, discover a new candidate, and wait through repeated sweeps. Confirm one grouped chime, no repeated chime for the same chunk/category, and no replay after muting and unmuting.
11. In a disposable test world, test each PvP module individually before combining them. Check crystal placement/breaking on valid and obstructed bases; anchor placement/charge/detonation, held-use latch, missing items, Nether behavior, strict Safe Anchor, and interruption by screens or manual slot changes. Explosions can damage your player and the world.
12. With a second player, check Trigger Bot, W Tap, axe switching against a raised shield, visible Aim Assist, manual Silent Aim, and one mace attack per fall. Verify cooldowns, FOV, line of sight, vanilla reach, spectator/death/menu/Freecam guards, and restoration after manual hotbar changes. Test enabled combinations for duplicate attacks or slot conflicts.
13. Equip Auto Totem from main inventory and hotbar with an occupied offhand, then check health-only mode, absorption, delay, inventory screens, and cursor stacks. Test the warning with and without a held totem, and reconnect/change worlds to verify state cleanup. Inspect all module pages at both large and small GUI scales.

## Cloud network requirements

The build downloads dependencies through the environment's configured HTTPS proxy with TLS verification enabled. It needs the usual Gradle/Maven/GitHub package destinations plus `maven.fabricmc.net`, `meta.fabricmc.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, and `resources.download.minecraft.net`.

If the proxy rejects one of these destinations, review and save the domain additions in the cloud environment settings, then retry the build. Saving an environment draft alone does not activate its network changes or publish a prepared environment.
