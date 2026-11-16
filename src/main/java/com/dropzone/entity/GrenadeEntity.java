package com.dropzone.entity;

import com.dropzone.DZConfig;
import com.dropzone.registry.ModEntities;
import com.dropzone.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Bouncing frag grenade with a 2.5 second fuse. */
public class GrenadeEntity extends ThrowableItemProjectile {
    private int fuse = 50;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(Level level, LivingEntity owner) {
        super(ModEntities.GRENADE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FRAG_GRENADE.get();
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.15D, getZ(), 0, 0.01, 0);
            }
        } else if (--fuse <= 0) {
            level().explode(this, getX(), getY(0.0625D), getZ(), DZConfig.GRENADE_POWER.get().floatValue(), false,
                    DZConfig.EXPLOSIONS_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        Vec3 v = getDeltaMovement();
        double speed = v.length();
        v = switch (result.getDirection().getAxis()) {
            case X -> new Vec3(-v.x * 0.45D, v.y * 0.7D, v.z * 0.7D);
            case Y -> new Vec3(v.x * 0.6D, -v.y * 0.35D, v.z * 0.6D);
            case Z -> new Vec3(v.x * 0.7D, v.y * 0.7D, -v.z * 0.45D);
        };
        if (v.lengthSqr() < 0.0016D) v = Vec3.ZERO;
        setDeltaMovement(v);
        if (!level().isClientSide && speed > 0.12D) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 0.5F, 1.6F);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        setDeltaMovement(getDeltaMovement().scale(-0.3D));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", fuse);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Fuse")) fuse = tag.getInt("Fuse");
    }
}
