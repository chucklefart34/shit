package com.dropzone;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DZConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_AMMO;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_INFINITE_AMMO;
    public static final ForgeConfigSpec.BooleanValue BULLETS_BREAK_GLASS;
    public static final ForgeConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue ROCKET_POWER;
    public static final ForgeConfigSpec.DoubleValue GRENADE_POWER;
    public static final ForgeConfigSpec.BooleanValue CHEST_LOOT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Gun and explosive settings").push("weapons");
        DAMAGE_MULTIPLIER = b.comment("Multiplier applied to all bullet damage.")
                .defineInRange("damageMultiplier", 1.0D, 0.0D, 100.0D);
        REQUIRE_AMMO = b.comment("If false, guns reload for free without ammo items.")
                .define("requireAmmo", true);
        CREATIVE_INFINITE_AMMO = b.comment("Creative-mode players reload without using ammo items.")
                .define("creativeInfiniteAmmo", true);
        BULLETS_BREAK_GLASS = b.comment("Bullets shatter glass blocks and panes (respects claim/protection mods).")
                .define("bulletsBreakGlass", true);
        EXPLOSIONS_BREAK_BLOCKS = b.comment("Rockets and grenades destroy blocks.")
                .define("explosionsBreakBlocks", true);
        ROCKET_POWER = b.comment("Rocket explosion power (TNT is 4.0).")
                .defineInRange("rocketPower", 3.0D, 0.5D, 10.0D);
        GRENADE_POWER = b.comment("Frag grenade explosion power (TNT is 4.0).")
                .defineInRange("grenadePower", 2.5D, 0.5D, 10.0D);
        b.pop();

        b.comment("World generation").push("loot");
        CHEST_LOOT = b.comment("Add Drop Zone gear to vanilla dungeon, mineshaft, outpost and similar chests.")
                .define("chestLoot", true);
        b.pop();

        SPEC = b.build();
    }

    private DZConfig() {}
}
