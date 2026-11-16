package com.dropzone.emote;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Emote list. Dances play a looping note-block beat; melody values are semitones from F#3
 * (note block pitch 0..24), -1 is a rest. Poses are in client.PlayerPoses.
 */
public enum Emote {
    WAVE(false, null),
    FLEX(false, null),
    CHEER(false, null),
    SALUTE(false, null),
    FACEPALM(false, null),
    ROBOT(true, new int[]{6, -1, 6, -1, 9, -1, 6, -1, 4, -1, 4, -1, 1, -1, 4, 6}),
    DISCO(true, new int[]{13, 13, 16, -1, 13, 11, 13, -1, 18, 16, 13, -1, 11, 13, 9, -1}),
    SWING(true, new int[]{8, 11, 13, 11, 8, -1, 6, 8, 11, -1, 13, 15, 16, 15, 13, -1});

    private final boolean dance;
    private final int[] melody;

    Emote(boolean dance, int[] melody) {
        this.dance = dance;
        this.melody = melody;
    }

    public boolean isDance() {
        return dance;
    }

    /** Null for non-dance emotes. */
    public int[] melody() {
        return melody;
    }

    public int id() {
        return ordinal();
    }

    public String translationKey() {
        return "emote.dropzone." + name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Emote byId(int id) {
        Emote[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
