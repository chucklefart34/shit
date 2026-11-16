package com.dropzone.storage;

import net.minecraft.ChatFormatting;

/** Storage box tiers: how many different item types, and how many of each. */
public enum BoxTier {
    COPPER("copper", 25, 1_000, ChatFormatting.GOLD),
    IRON("iron", 50, 2_500, ChatFormatting.WHITE),
    GOLD("gold", 75, 5_000, ChatFormatting.YELLOW),
    DIAMOND("diamond", 100, 10_000, ChatFormatting.AQUA);

    public final String id;
    public final int types;
    public final int perType;
    public final ChatFormatting color;

    BoxTier(String id, int types, int perType, ChatFormatting color) {
        this.id = id;
        this.types = types;
        this.perType = perType;
        this.color = color;
    }

    public BoxTier next() {
        return this == DIAMOND ? null : values()[ordinal() + 1];
    }

    public static BoxTier byOrdinal(int i) {
        BoxTier[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }
}
