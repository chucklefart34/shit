package com.dropzone;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DZConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_AMMO;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_INFINITE_AMMO;
    public static final ForgeConfigSpec.IntValue GUN_AMMO_CAPACITY;
    public static final ForgeConfigSpec.BooleanValue BULLETS_BREAK_GLASS;
    public static final ForgeConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue ROCKET_POWER;
    public static final ForgeConfigSpec.DoubleValue GRENADE_POWER;
    public static final ForgeConfigSpec.BooleanValue CHEST_LOOT;
    public static final ForgeConfigSpec.BooleanValue CANNON_ALLOW_DESTRUCTION;
    public static final ForgeConfigSpec.IntValue CANNON_COOLDOWN;
    public static final ForgeConfigSpec.IntValue CANNON_VOLLEY;
    public static final ForgeConfigSpec.DoubleValue CANNON_POWER;
    public static final ForgeConfigSpec.DoubleValue CANNON_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue DAY_TITLE;
    public static final ForgeConfigSpec.BooleanValue DAY_HUD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Gun and explosive settings").push("weapons");
        DAMAGE_MULTIPLIER = b.comment("Multiplier applied to all bullet damage.")
                .defineInRange("damageMultiplier", 1.0D, 0.0D, 100.0D);
        REQUIRE_AMMO = b.comment("If false, guns reload for free without ammo items.")
                .define("requireAmmo", true);
        CREATIVE_INFINITE_AMMO = b.comment("Creative-mode players reload without using ammo items.")
                .define("creativeInfiniteAmmo", true);
        GUN_AMMO_CAPACITY = b.comment("How many reserve rounds each gun stores internally. Matching ammo is pulled out of your inventory into the gun automatically.")
                .defineInRange("gunAmmoCapacity", 1000, 0, 100000);
        BULLETS_BREAK_GLASS = b.comment("Bullets shatter glass blocks and panes (respects claim/protection mods).")
                .define("bulletsBreakGlass", true);
        EXPLOSIONS_BREAK_BLOCKS = b.comment("Rockets and grenades destroy blocks.")
                .define("explosionsBreakBlocks", true);
        ROCKET_POWER = b.comment("Rocket explosion power (TNT is 4.0).")
                .defineInRange("rocketPower", 3.0D, 0.5D, 10.0D);
        GRENADE_POWER = b.comment("Frag grenade explosion power (TNT is 4.0).")
                .defineInRange("grenadePower", 2.5D, 0.5D, 10.0D);
        b.pop();

        b.comment("Cannon Saddle").push("cannon");
        CANNON_ALLOW_DESTRUCTION = b.comment("Allow the cannon's Destroy mode (block-breaking, fire). If false it's always Damage mode.")
                .define("allowDestroyMode", true);
        CANNON_COOLDOWN = b.comment("Ticks between cannon volleys (20 = 1 second).")
                .defineInRange("cooldownTicks", 20, 1, 1200);
        CANNON_VOLLEY = b.comment("Fireballs per shot.")
                .defineInRange("fireballsPerShot", 3, 1, 9);
        CANNON_POWER = b.comment("Explosion power of each fireball (TNT is 4.0).")
                .defineInRange("explosionPower", 3.5D, 0.5D, 12.0D);
        CANNON_DAMAGE = b.comment("Extra damage on a direct hit.")
                .defineInRange("directHitDamage", 10.0D, 0.0D, 1000.0D);
        b.pop();

        b.comment("Day counter").push("days");
        DAY_TITLE = b.comment("Show a big 'Day N' title to everyone when a new day starts.")
                .define("dayTitle", true);
        DAY_HUD = b.comment("Show the current day in the top-left corner of the screen.")
                .define("dayHud", true);
        b.pop();

        b.comment("World generation").push("loot");
        CHEST_LOOT = b.comment("Add Drop Zone gear to vanilla dungeon, mineshaft, outpost and similar chests.")
                .define("chestLoot", true);
        b.pop();

        SPEC = b.build();
    }

    private DZConfig() {}
}
