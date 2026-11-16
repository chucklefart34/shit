package com.dropzone.registry;

import com.dropzone.DropZone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DropZone.MODID);

    public static final RegistryObject<SoundEvent> PISTOL_SHOT = register("gun.pistol");
    public static final RegistryObject<SoundEvent> SMG_SHOT = register("gun.smg");
    public static final RegistryObject<SoundEvent> RIFLE_SHOT = register("gun.rifle");
    public static final RegistryObject<SoundEvent> SHOTGUN_SHOT = register("gun.shotgun");
    public static final RegistryObject<SoundEvent> SNIPER_SHOT = register("gun.sniper");
    public static final RegistryObject<SoundEvent> ROCKET_LAUNCH = register("gun.rocket");
    public static final RegistryObject<SoundEvent> RELOAD_START = register("gun.reload_start");
    public static final RegistryObject<SoundEvent> RELOAD_END = register("gun.reload_end");
    public static final RegistryObject<SoundEvent> DRY_FIRE = register("gun.dry_fire");
    public static final RegistryObject<SoundEvent> HITMARKER = register("gun.hitmarker");
    public static final RegistryObject<SoundEvent> HEADSHOT = register("gun.headshot");
    public static final RegistryObject<SoundEvent> GLIDER_DEPLOY = register("glider.deploy");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DropZone.MODID, name)));
    }

    private ModSounds() {}
}
