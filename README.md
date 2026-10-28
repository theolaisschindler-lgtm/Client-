# Amethyst

A Fabric **client mod for Minecraft Java 1.21.11** with a dark, violet click GUI inspired by the supplied reference.

## Features

- **Storage ESP:** configurable outlines for chests (including trapped and ender chests), hoppers, and shulker boxes. Barrels can be enabled separately.
- **Spawner Finder:** configurable outlines for spawners in loaded chunks.
- Individual target colors, RGB controls, opacity, distance (16–256 blocks), line width (1–5), through-wall rendering, rainbow colors, and pulsing outlines.
- Settings persist in `config/amethyst.json` inside your Minecraft instance.

Press **Right Shift** in-game to open the GUI. The binding can be changed under **Options → Controls → Key Binds**. Enable a module, select the block types you want, and adjust its appearance in the settings panel. Close the GUI to save your changes.

The mod reads blocks the client already knows about. It does not load distant chunks, search world seeds, or discover blocks the server has withheld. Use it only where the server's rules allow it.

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

In this cloud instance, select the installed full JDK with `export JAVA_HOME=/workspace/.toolchains/jdk-21.0.9+10`.
Java needs explicit proxy and CA settings here. Add these arguments to Gradle commands:

```sh
-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
```

To start the development client on a machine with a graphical display:

```sh
./gradlew runClient
```

The development client uses a separate `run/` instance. A successful build does not establish in-game rendering compatibility: check the GUI and both modules in Minecraft using the steps below. Launching a playable client requires a display and downloaded Minecraft assets; multiplayer authentication may require a normal authenticated launcher session.

## In-game verification

1. Enter a creative test world and place a chest, trapped chest, ender chest, hopper, shulker box, barrel, and spawner.
2. Open the GUI with Right Shift. Enable Storage ESP and confirm its default storage types are outlined; enable barrels and check that they appear too.
3. Enable Spawner Finder and confirm the spawner uses its own color.
4. Change individual colors, opacity, line width, distance, rainbow, pulse, and through-wall rendering. Check the result both in front of and behind a wall.
5. Close and reopen the GUI, then restart the client, to confirm settings persist. Break a highlighted block and leave the area to check stale outlines disappear.

## Cloud network requirements

The build downloads dependencies through the environment's configured HTTPS proxy with TLS verification enabled. It needs the usual Gradle/Maven/GitHub package destinations plus `maven.fabricmc.net`, `meta.fabricmc.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, and `resources.download.minecraft.net`.

If the proxy rejects one of these destinations, review and save the domain additions in the cloud environment settings, then retry the build. Saving an environment draft alone does not activate its network changes or publish a prepared environment.
