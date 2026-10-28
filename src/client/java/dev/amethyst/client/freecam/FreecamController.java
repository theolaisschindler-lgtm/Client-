package dev.amethyst.client.freecam;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.config.FreecamSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/** A local-only camera entity. The real player's simulation and networking remain vanilla. */
public final class FreecamController {
    private static FreecamCamera camera;
    private static LocalPlayer body;
    private static ClientLevel world;
    private static Entity previousCamera;
    private static CameraType previousPerspective;

    private FreecamController() { }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(FreecamController::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(FreecamController::moveCamera);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> disable());
    }

    public static boolean isActive() { return camera != null; }
    public static String status() { return isActive() ? "Freecam active / F6 to return" : "F6 toggles Freecam in-world"; }

    private static void startTick(Minecraft client) {
        FreecamSettings settings = AmethystClient.config().freecam;
        if (camera != null) {
            if (!settings.enabled || client.level != world || client.player != body || !body.isAlive()
                    || body.isRemoved() || body.isPassenger() || client.getCameraEntity() != camera) {
                disable();
                return;
            }
            client.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (settings.enabled) {
            if (client.level == null || client.player == null || !client.player.isAlive()
                    || client.player.isRemoved() || client.player.isPassenger()
                    || client.getCameraEntity() != client.player) {
                settings.enabled = false;
                return;
            }
            enable(client);
        }
    }

    private static void enable(Minecraft client) {
        body = client.player;
        world = client.level;
        previousCamera = client.getCameraEntity();
        previousPerspective = client.options.getCameraType();
        camera = new FreecamCamera(world, body.getGameProfile());
        camera.copyPosition(body);
        // Starting eyes coincide even if the player is crouching, swimming, or sleeping.
        camera.setPos(body.getX(), body.getEyeY() - camera.getEyeHeight(), body.getZ());
        camera.setOldPosAndRot();
        camera.noPhysics = true;
        camera.setNoGravity(true);
        camera.setDeltaMovement(Vec3.ZERO);
        body.input.keyPresses = Input.EMPTY;
        body.setSprinting(false);
        if (client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
            if (body.isUsingItem()) client.gameMode.releaseUsingItem(body);
        }
        client.options.setCameraType(CameraType.FIRST_PERSON);
        client.setCameraEntity(camera);
    }

    private static void moveCamera(Minecraft client) {
        if (!isActive() || client.player != body || client.level != world) return;
        camera.setOldPosAndRot();
        if (client.screen != null || client.getOverlay() != null || !client.isWindowActive() || client.isPaused()) return;
        var options = client.options;
        FreecamSettings settings = AmethystClient.config().freecam;
        int forward = (options.keyUp.isDown() ? 1 : 0) - (options.keyDown.isDown() ? 1 : 0);
        int left = (options.keyLeft.isDown() ? 1 : 0) - (options.keyRight.isDown() ? 1 : 0);
        int vertical = (options.keyJump.isDown() ? 1 : 0) - (options.keyShift.isDown() ? 1 : 0);
        double speed = settings.speed * (options.keySprint.isDown() ? settings.boostMultiplier : 1);
        FreecamMovement.Offset movement = FreecamMovement.calculate(camera.getYRot(), camera.getXRot(), forward, left, vertical, speed);
        camera.setPos(camera.getX() + movement.x(), camera.getY() + movement.y(), camera.getZ() + movement.z());
    }

    /** Called by the mouse hook after vanilla sensitivity and inversion have been applied. */
    public static boolean turnCamera(LocalPlayer player, double yaw, double pitch) {
        if (camera == null || player != body) return false;
        camera.turn(yaw, pitch);
        return true;
    }

    public static boolean controlsBody(LocalPlayer player) { return camera != null && player == body; }

    public static void disable() {
        Minecraft client = Minecraft.getInstance();
        if (AmethystClient.config() != null) AmethystClient.config().freecam.enabled = false;
        if (camera != null) {
            boolean sessionChanged = client.level != world || client.player != body || !body.isAlive() || body.isRemoved();
            // Preserve a camera installed by vanilla respawn/spectating or another mod.
            if (client.getCameraEntity() == camera) {
                Entity restore = client.level == world && client.player == body && previousCamera != null
                        && !previousCamera.isRemoved() ? previousCamera : client.player;
                client.setCameraEntity(restore);
                if (previousPerspective != null) client.options.setCameraType(previousPerspective);
            } else if (sessionChanged && previousPerspective != null) {
                // Vanilla may replace the camera before its dimension/respawn callback reaches us.
                client.options.setCameraType(previousPerspective);
            }
            camera = null;
            body = null;
            world = null;
            previousCamera = null;
            previousPerspective = null;
        }
    }
}
