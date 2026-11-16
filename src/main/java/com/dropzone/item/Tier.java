package com.dropzone.item;

import net.minecraft.ChatFormatting;

/** Loot rarity shown as the item name colour. */
public enum Tier {
    COMMON(ChatFormatting.WHITE),
    UNCOMMON(ChatFormatting.GREEN),
    RARE(ChatFormatting.BLUE),
    EPIC(ChatFormatting.LIGHT_PURPLE),
    LEGENDARY(ChatFormatting.GOLD);

    public final ChatFormatting color;

    Tier(ChatFormatting color) {
        this.color = color;
    }

    public String translationKey() {
        return "tier.dropzone." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
