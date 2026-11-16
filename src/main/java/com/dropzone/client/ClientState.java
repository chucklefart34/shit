package com.dropzone.client;

import com.dropzone.emote.Emote;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** What the client knows about gliding and emoting players, keyed by entity id. */
public final class ClientState {
    public record EmoteInstance(Emote emote, int startTick) {}

    private static final Map<Integer, EmoteInstance> EMOTES = new HashMap<>();
    private static final Map<Integer, Integer> GLIDERS = new HashMap<>();
    @Nullable
    private static CameraType savedCamera;
    private static boolean emoteCamera;
    private static boolean glideCamera;

    public static void setGlider(int entityId, boolean gliding, int itemId) {
        if (gliding) GLIDERS.put(entityId, itemId);
        else GLIDERS.remove(entityId);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entityId == mc.player.getId()) {
            glideCamera = gliding;
            updateCamera(mc);
        }
    }

    /** Third person while our own player emotes or glides, then restore whatever the player had. */
    private static void updateCamera(Minecraft mc) {
        boolean want = emoteCamera || glideCamera;
        if (want && savedCamera == null) {
            savedCamera = mc.options.getCameraType();
            if (savedCamera.isFirstPerson()) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!want && savedCamera != null) {
            mc.options.setCameraType(savedCamera);
            savedCamera = null;
        }
    }

    public static boolean isGliding(int entityId) {
        return GLIDERS.containsKey(entityId);
    }

    @Nullable
    public static Integer gliderItem(int entityId) {
        return GLIDERS.get(entityId);
    }

    public static void setEmote(int entityId, int emoteId) {
        Minecraft mc = Minecraft.getInstance();
        Emote emote = Emote.byId(emoteId);
        if (emote == null) {
            EMOTES.remove(entityId);
        } else {
            Entity e = mc.level != null ? mc.level.getEntity(entityId) : null;
            EMOTES.put(entityId, new EmoteInstance(emote, e != null ? e.tickCount : 0));
        }

        if (mc.player != null && entityId == mc.player.getId()) {
            emoteCamera = emote != null;
            updateCamera(mc);
        }
    }

    @Nullable
    public static EmoteInstance getEmote(int entityId) {
        return EMOTES.get(entityId);
    }

    public static Map<Integer, EmoteInstance> emotes() {
        return Collections.unmodifiableMap(EMOTES);
    }

    public static void clear() {
        EMOTES.clear();
        GLIDERS.clear();
        savedCamera = null;
        emoteCamera = false;
        glideCamera = false;
    }

    private ClientState() {}
}
