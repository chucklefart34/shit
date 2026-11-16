package com.dropzone.server;

import com.dropzone.network.DZNetwork;
import com.dropzone.network.S2CEmoteState;
import com.dropzone.network.S2CGliderState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Transient per-player server state: who is gliding, who is emoting, and fire-rate timers. */
public final class ServerState {
    public record EmoteState(int emote, Vec3 start) {}

    private static final Map<UUID, Integer> GLIDING = new HashMap<>();
    private static final Map<UUID, EmoteState> EMOTES = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SHOT = new HashMap<>();

    // ---------------------------------------------------------------- fire rate

    public static boolean canShoot(ServerPlayer player, long now) {
        Long next = NEXT_SHOT.get(player.getUUID());
        return next == null || now + 1 >= next; // 1 tick of tolerance for network jitter
    }

    public static void setNextShot(ServerPlayer player, long tick) {
        NEXT_SHOT.put(player.getUUID(), tick);
    }

    // ---------------------------------------------------------------- gliders

    public static boolean isGliding(ServerPlayer player) {
        return GLIDING.containsKey(player.getUUID());
    }

    /** Always notifies the player and everyone tracking them, so a desynced client gets corrected. */
    public static void setGliding(ServerPlayer player, boolean on, int gliderItemId) {
        if (on) GLIDING.put(player.getUUID(), gliderItemId);
        else GLIDING.remove(player.getUUID());
        DZNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CGliderState(player.getId(), on, gliderItemId));
    }

    // ---------------------------------------------------------------- emotes

    public static EmoteState getEmote(ServerPlayer player) {
        return EMOTES.get(player.getUUID());
    }

    public static void setEmote(ServerPlayer player, int emote) {
        if (emote < 0) {
            if (EMOTES.remove(player.getUUID()) == null) return;
        } else {
            EMOTES.put(player.getUUID(), new EmoteState(emote, player.position()));
        }
        DZNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CEmoteState(player.getId(), emote));
    }

    // ---------------------------------------------------------------- lifecycle

    /** Tell a player who just started seeing {@code target} what it is doing. */
    public static void syncTo(ServerPlayer viewer, ServerPlayer target) {
        Integer glider = GLIDING.get(target.getUUID());
        if (glider != null) {
            DZNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new S2CGliderState(target.getId(), true, glider));
        }
        EmoteState emote = EMOTES.get(target.getUUID());
        if (emote != null) {
            DZNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new S2CEmoteState(target.getId(), emote.emote()));
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (GLIDING.containsKey(player.getUUID())) setGliding(player, false, 0);
        setEmote(player, -1);
    }

    public static void forget(UUID id) {
        GLIDING.remove(id);
        EMOTES.remove(id);
        NEXT_SHOT.remove(id);
    }

    public static void clearAll() {
        GLIDING.clear();
        EMOTES.clear();
        NEXT_SHOT.clear();
    }

    private ServerState() {}
}
