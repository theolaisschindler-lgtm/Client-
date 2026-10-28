# Amethyst

A Fabric **client mod for Minecraft Java 1.21.11** with a dark, violet click GUI inspired by the supplied reference.

## Features

- **Storage ESP:** configurable outlines for chests (including trapped and ender chests), hoppers, and shulker boxes. Barrels can be enabled separately.
- **Spawner Finder:** configurable outlines for spawners in loaded chunks.
- **Chunk Finder:** surveys loaded chunks for amethyst/calcite geode candidates and combinations of possible activity clues. Outlines mark the 16×16×16 chunk section containing a representative clue. Geodes and activity use separate configurable colors.
- **Freecam:** a detached, no-clip camera with adjustable speed and sprint boost. Toggle it under Visuals or with **F6** (rebindable in Controls).
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

Surveying is incremental, so signals can take time to appear and update, especially at larger distances. The inspector shows survey progress. Natural structures, including villages, can match the activity heuristics; a candidate is not proof of a player base. The scan does not detect chunk age, ownership, or players themselves.

### Freecam

Select **Visuals → Freecam** or press **F6** in-game to detach the camera. Use your configured movement keys (normally **WASD**), **Jump** to rise, **Sneak** to descend, and **Sprint** for the adjustable boost. Look around with the mouse. Speed is measured in blocks per tick (20 ticks per second). Camera movement pauses while a GUI or chat is open.

Your real player stays in the world, with its movement controls suppressed. Gravity, server corrections, and damage still apply to the body. Camera movement does not teleport the player, request distant chunks, or let you interact with blocks from the detached camera. Disable Freecam to return to the player's current location and restore your previous perspective. It resets on death, respawn, world/dimension changes, and disconnect, and never activates automatically after restarting the game. Dismount vehicles before enabling it. Speed and boost preferences are saved normally.

## Install

Use Minecraft **1.21.11**, **Java 21**, Fabric Loader **0.18.2 or newer**, and Fabric API **0.141.6+1.21.11**. The development build pins Fabric Loader **0.20.1**.

Build the project, then copy the main JAR from `build/libs/` into your Minecraft instance's `mods/` folder alongside Fabric API. Do not install the `-sources.jar` file. This is a client-only mod; the server does not need it.

## Develop

Use the existing checkout; each cloud task already has an isolated workspace.

```sh
cd /workspace/Client-
./gradlew build
```

`build` compiles and packages the mod and runs the configuration tests. On Windows, use `gradlew.bat build`.

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

In this cloud instance, select the installed full JDK with `export JAVA_HOME=/workspace/.toolchains/jdk-21.0.9+10`.
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

## Cloud network requirements

The build downloads dependencies through the environment's configured HTTPS proxy with TLS verification enabled. It needs the usual Gradle/Maven/GitHub package destinations plus `maven.fabricmc.net`, `meta.fabricmc.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, and `resources.download.minecraft.net`.

If the proxy rejects one of these destinations, review and save the domain additions in the cloud environment settings, then retry the build. Saving an environment draft alone does not activate its network changes or publish a prepared environment.
