package com.dropzone.entity;

import com.dropzone.DZConfig;
import com.dropzone.registry.ModEntities;
import com.dropzone.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RocketEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE = 200;

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public RocketEntity(Level level, LivingEntity owner) {
        super(ModEntities.ROCKET.get(), owner, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ROCKET.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 v = getDeltaMovement();
            for (int i = 0; i < 2; i++) {
                double f = i * 0.5D;
                level().addParticle(ParticleTypes.FLAME, getX() - v.x * f, getY() - v.y * f, getZ() - v.z * f, 0, 0, 0);
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() - v.x * (f + 0.4D), getY() - v.y * (f + 0.4D),
                        getZ() - v.z * (f + 0.4D), 0, 0.02, 0);
            }
        } else if (tickCount > MAX_LIFE) {
            explode();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && isAlive()) {
            explode();
        }
    }

    private void explode() {
        level().explode(this, getX(), getY(), getZ(), DZConfig.ROCKET_POWER.get().floatValue(), false,
                DZConfig.EXPLOSIONS_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        discard();
    }
}
