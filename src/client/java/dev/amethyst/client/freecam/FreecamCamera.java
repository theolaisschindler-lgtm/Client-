package dev.amethyst.client.freecam;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

/** A camera reads mouse rotation directly, never a remote player's head-animation state. */
public final class FreecamCamera extends RemotePlayer {
    public FreecamCamera(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public float getViewYRot(float partialTick) {
        return getYRot();
    }

    @Override
    public float getViewXRot(float partialTick) {
        return getXRot();
    }
}
