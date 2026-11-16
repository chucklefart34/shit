package com.dropzone.registry;

import com.dropzone.DropZone;
import com.dropzone.entity.CannonFireballEntity;
import com.dropzone.entity.GrenadeEntity;
import com.dropzone.entity.RocketEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DropZone.MODID);

    public static final RegistryObject<EntityType<RocketEntity>> ROCKET = ENTITIES.register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(DropZone.MODID + ":rocket"));

    public static final RegistryObject<EntityType<GrenadeEntity>> GRENADE = ENTITIES.register("frag_grenade",
            () -> EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(DropZone.MODID + ":frag_grenade"));

    public static final RegistryObject<EntityType<CannonFireballEntity>> CANNON_FIREBALL = ENTITIES.register("cannon_fireball",
            () -> EntityType.Builder.<CannonFireballEntity>of(CannonFireballEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(DropZone.MODID + ":cannon_fireball"));

    private ModEntities() {}
}
