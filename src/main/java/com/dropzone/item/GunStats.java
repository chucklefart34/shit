package com.dropzone.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Tuning values for one gun. Damage is in half-hearts (a player has 20).
 * Spread is a cone half-angle in degrees.
 */
public record GunStats(
        float damage,
        int cooldown,
        int magSize,
        int reloadTicks,
        double range,
        float hipSpread,
        float adsSpread,
        int pellets,
        float headshotMult,
        double falloffStart,
        float minFalloff,
        boolean automatic,
        boolean projectile,
        float recoil,
        float knockback,
        float zoom,
        Supplier<? extends Item> ammo,
        Supplier<SoundEvent> sound,
        float soundVolume
) {
    public float damageAt(double distance) {
        if (distance <= falloffStart || range <= falloffStart) return damage;
        double t = Mth.clamp((distance - falloffStart) / (range - falloffStart), 0.0D, 1.0D);
        return (float) (damage * Mth.lerp(t, 1.0D, minFalloff));
    }

    public float shotsPerSecond() {
        return 20.0F / Math.max(1, cooldown);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float damage = 5;
        private int cooldown = 4;
        private int magSize = 10;
        private int reloadTicks = 30;
        private double range = 64;
        private float hipSpread = 2;
        private float adsSpread = 0.5F;
        private int pellets = 1;
        private float headshotMult = 1.5F;
        private double falloffStart = 24;
        private float minFalloff = 0.6F;
        private boolean automatic;
        private boolean projectile;
        private float recoil = 1;
        private float knockback = 0.3F;
        private float zoom = 0.8F;
        private Supplier<? extends Item> ammo;
        private Supplier<SoundEvent> sound;
        private float soundVolume = 2;

        public Builder damage(float v) { damage = v; return this; }
        public Builder cooldown(int ticks) { cooldown = ticks; return this; }
        public Builder mag(int v) { magSize = v; return this; }
        public Builder reload(int ticks) { reloadTicks = ticks; return this; }
        public Builder range(double v) { range = v; return this; }
        public Builder spread(float hip, float ads) { hipSpread = hip; adsSpread = ads; return this; }
        public Builder pellets(int v) { pellets = v; return this; }
        public Builder headshot(float v) { headshotMult = v; return this; }
        public Builder falloff(double start, float min) { falloffStart = start; minFalloff = min; return this; }
        public Builder automatic() { automatic = true; return this; }
        public Builder projectile() { projectile = true; return this; }
        public Builder recoil(float v) { recoil = v; return this; }
        public Builder knockback(float v) { knockback = v; return this; }
        public Builder zoom(float v) { zoom = v; return this; }
        public Builder ammo(Supplier<? extends Item> v) { ammo = v; return this; }
        public Builder sound(Supplier<SoundEvent> v, float volume) { sound = v; soundVolume = volume; return this; }

        public GunStats build() {
            return new GunStats(damage, cooldown, magSize, reloadTicks, range, hipSpread, adsSpread, pellets,
                    headshotMult, falloffStart, minFalloff, automatic, projectile, recoil, knockback, zoom,
                    ammo, sound, soundVolume);
        }
    }
}
