package com.dropzone.registry;

import com.dropzone.DropZone;
import com.dropzone.item.AmmoItem;
import com.dropzone.item.CannonSaddleItem;
import com.dropzone.item.GliderItem;
import com.dropzone.item.GrenadeItem;
import com.dropzone.item.GunItem;
import com.dropzone.item.GunStats;
import com.dropzone.item.HealingItem;
import com.dropzone.item.Tier;
import com.dropzone.hammer.HammerItem;
import com.dropzone.storage.BoxTier;
import com.dropzone.storage.BoxUpgradeItem;
import com.dropzone.storage.StorageBoxBlock;
import com.dropzone.storage.StorageBoxItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DropZone.MODID);

    // ------------------------------------------------------------------ crafting parts
    public static final RegistryObject<Item> GUN_BARREL = simple("gun_barrel");
    public static final RegistryObject<Item> GUN_STOCK = simple("gun_stock");
    public static final RegistryObject<Item> FIRING_MECHANISM = simple("firing_mechanism");

    // ------------------------------------------------------------------ ammo
    public static final RegistryObject<Item> LIGHT_AMMO = ammo("light_ammo");
    public static final RegistryObject<Item> MEDIUM_AMMO = ammo("medium_ammo");
    public static final RegistryObject<Item> HEAVY_AMMO = ammo("heavy_ammo");
    public static final RegistryObject<Item> SHELLS = ammo("shells");
    public static final RegistryObject<Item> ROCKET = ITEMS.register("rocket", () -> new AmmoItem(new Item.Properties().stacksTo(16)));

    // ------------------------------------------------------------------ guns
    public static final RegistryObject<GunItem> PISTOL = gun("pistol", Tier.COMMON, GunStats.builder()
            .damage(5.0F).cooldown(4).mag(16).reload(26).range(64)
            .spread(2.5F, 0.8F).headshot(2.0F).falloff(24, 0.6F)
            .recoil(1.6F).knockback(0.25F).zoom(0.85F)
            .ammo(LIGHT_AMMO).sound(ModSounds.PISTOL_SHOT, 2.0F));

    public static final RegistryObject<GunItem> TACTICAL_SMG = gun("tactical_smg", Tier.UNCOMMON, GunStats.builder()
            .damage(3.5F).cooldown(2).mag(30).reload(40).range(48)
            .spread(4.0F, 2.0F).headshot(1.75F).falloff(16, 0.55F).automatic()
            .recoil(0.9F).knockback(0.15F).zoom(0.85F)
            .ammo(LIGHT_AMMO).sound(ModSounds.SMG_SHOT, 1.8F));

    public static final RegistryObject<GunItem> PUMP_SHOTGUN = gun("pump_shotgun", Tier.UNCOMMON, GunStats.builder()
            .damage(2.4F).pellets(9).cooldown(20).mag(5).reload(90).range(28)
            .spread(7.0F, 5.0F).headshot(1.5F).falloff(6, 0.25F)
            .recoil(6.0F).knockback(0.5F).zoom(0.9F)
            .ammo(SHELLS).sound(ModSounds.SHOTGUN_SHOT, 3.0F));

    public static final RegistryObject<GunItem> ASSAULT_RIFLE = gun("assault_rifle", Tier.RARE, GunStats.builder()
            .damage(6.5F).cooldown(4).mag(30).reload(46).range(96)
            .spread(3.0F, 0.6F).headshot(1.5F).falloff(40, 0.7F).automatic()
            .recoil(1.3F).knockback(0.2F).zoom(0.7F)
            .ammo(MEDIUM_AMMO).sound(ModSounds.RIFLE_SHOT, 2.5F));

    public static final RegistryObject<GunItem> BOLT_SNIPER = gun("bolt_sniper", Tier.EPIC, GunStats.builder()
            .damage(22.0F).cooldown(30).mag(1).reload(50).range(200)
            .spread(6.0F, 0.0F).headshot(2.0F).falloff(120, 0.8F)
            .recoil(7.0F).knockback(0.6F).zoom(0.22F)
            .ammo(HEAVY_AMMO).sound(ModSounds.SNIPER_SHOT, 5.0F));

    public static final RegistryObject<GunItem> ROCKET_LAUNCHER = gun("rocket_launcher", Tier.LEGENDARY, GunStats.builder()
            .cooldown(25).mag(1).reload(60).range(120)
            .spread(1.5F, 0.3F).projectile()
            .recoil(5.0F).zoom(0.75F)
            .ammo(ROCKET).sound(ModSounds.ROCKET_LAUNCH, 3.0F));

    // ------------------------------------------------------------------ throwables
    public static final RegistryObject<Item> FRAG_GRENADE = ITEMS.register("frag_grenade",
            () -> new GrenadeItem(new Item.Properties().stacksTo(8)));

    // ------------------------------------------------------------------ healing & shields
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage", () -> new HealingItem(
            HealingItem.Kind.HEALTH, 4.0F, 0.75F, 70, UseAnim.BRUSH, Tier.COMMON, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MED_KIT = ITEMS.register("med_kit", () -> new HealingItem(
            HealingItem.Kind.HEALTH, 40.0F, 1.0F, 200, UseAnim.BRUSH, Tier.UNCOMMON, new Item.Properties().stacksTo(3)));
    public static final RegistryObject<Item> MINI_SHIELD = ITEMS.register("mini_shield", () -> new HealingItem(
            HealingItem.Kind.SHIELD, 5.0F, 10.0F, 40, UseAnim.DRINK, Tier.UNCOMMON, new Item.Properties().stacksTo(6)));
    public static final RegistryObject<Item> SHIELD_POTION = ITEMS.register("shield_potion", () -> new HealingItem(
            HealingItem.Kind.SHIELD, 10.0F, 20.0F, 100, UseAnim.DRINK, Tier.RARE, new Item.Properties().stacksTo(3)));

    // ------------------------------------------------------------------ gliders
    public static final RegistryObject<Item> GLIDER_AZURE = glider("glider_azure", "azure", Tier.UNCOMMON);
    public static final RegistryObject<Item> GLIDER_CRIMSON = glider("glider_crimson", "crimson", Tier.RARE);
    public static final RegistryObject<Item> GLIDER_EMERALD = glider("glider_emerald", "emerald", Tier.RARE);
    public static final RegistryObject<Item> GLIDER_SUNSET = glider("glider_sunset", "sunset", Tier.EPIC);

    // ------------------------------------------------------------------ mounts
    public static final RegistryObject<Item> CANNON_SADDLE = ITEMS.register("cannon_saddle",
            () -> new CannonSaddleItem(new Item.Properties().stacksTo(1)));

    // ------------------------------------------------------------------ storage boxes
    public static final RegistryObject<Item> COPPER_STORAGE_BOX = boxItem("copper_storage_box", ModBlocks.COPPER_BOX);
    public static final RegistryObject<Item> IRON_STORAGE_BOX = boxItem("iron_storage_box", ModBlocks.IRON_BOX);
    public static final RegistryObject<Item> GOLD_STORAGE_BOX = boxItem("gold_storage_box", ModBlocks.GOLD_BOX);
    public static final RegistryObject<Item> DIAMOND_STORAGE_BOX = boxItem("diamond_storage_box", ModBlocks.DIAMOND_BOX);
    public static final RegistryObject<Item> IRON_BOX_UPGRADE = ITEMS.register("iron_box_upgrade", () -> new BoxUpgradeItem(BoxTier.COPPER, new Item.Properties()));
    public static final RegistryObject<Item> GOLD_BOX_UPGRADE = ITEMS.register("gold_box_upgrade", () -> new BoxUpgradeItem(BoxTier.IRON, new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_BOX_UPGRADE = ITEMS.register("diamond_box_upgrade", () -> new BoxUpgradeItem(BoxTier.GOLD, new Item.Properties()));

    public static final RegistryObject<Item> HAMMER = ITEMS.register("hammer", () -> new HammerItem(new Item.Properties()));

    private static RegistryObject<Item> boxItem(String name, RegistryObject<StorageBoxBlock> block) {
        return ITEMS.register(name, () -> new StorageBoxItem(block.get(), new Item.Properties()));
    }

    /** Creative tab order: weapons first, then ammo, consumables, gliders, parts. */
    public static List<Supplier<? extends Item>> creativeOrder() {
        return List.of(PISTOL, TACTICAL_SMG, PUMP_SHOTGUN, ASSAULT_RIFLE, BOLT_SNIPER, ROCKET_LAUNCHER, FRAG_GRENADE,
                LIGHT_AMMO, MEDIUM_AMMO, HEAVY_AMMO, SHELLS, ROCKET,
                BANDAGE, MED_KIT, MINI_SHIELD, SHIELD_POTION,
                GLIDER_AZURE, GLIDER_CRIMSON, GLIDER_EMERALD, GLIDER_SUNSET, CANNON_SADDLE,
                COPPER_STORAGE_BOX, IRON_STORAGE_BOX, GOLD_STORAGE_BOX, DIAMOND_STORAGE_BOX,
                IRON_BOX_UPGRADE, GOLD_BOX_UPGRADE, DIAMOND_BOX_UPGRADE, HAMMER,
                GUN_BARREL, GUN_STOCK, FIRING_MECHANISM);
    }

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> ammo(String name) {
        return ITEMS.register(name, () -> new AmmoItem(new Item.Properties()));
    }

    private static RegistryObject<GunItem> gun(String name, Tier tier, GunStats.Builder stats) {
        return ITEMS.register(name, () -> new GunItem(stats.build(), tier, new Item.Properties()));
    }

    private static RegistryObject<Item> glider(String name, String color, Tier tier) {
        return ITEMS.register(name, () -> new GliderItem(color, tier, new Item.Properties()));
    }

    private ModItems() {}
}
