package com.dropzone.entity;

import com.dropzone.DZConfig;
import com.dropzone.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

/** Saddle cannon shot. Destroy mode: big fiery block-breaking blast. Damage mode: hurts mobs, leaves the world alone. */
public class CannonFireballEntity extends Fireball {
    private static final EntityDataAccessor<Boolean> DESTROY =
            SynchedEntityData.defineId(CannonFireballEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_LIFE = 160;

    public CannonFireballEntity(EntityType<? extends CannonFireballEntity> type, Level level) {
        super(type, level);
    }

    public CannonFireballEntity(Level level, LivingEntity owner, Vec3 dir, boolean destroy) {
        super(ModEntities.CANNON_FIREBALL.get(), owner, dir.x, dir.y, dir.z, level);
        this.entityData.set(DESTROY, destroy);
        Vec3 n = dir.normalize();
        this.xPower = n.x * 0.15D;
        this.yPower = n.y * 0.15D;
        this.zPower = n.z * 0.15D;
        this.setDeltaMovement(n.scale(1.6D));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DESTROY, false);
    }

    public boolean isDestructive() {
        return entityData.get(DESTROY);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(i == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                        getX() + (random.nextDouble() - 0.5D) * 0.8D, getY() + 0.4D + (random.nextDouble() - 0.5D) * 0.8D,
                        getZ() + (random.nextDouble() - 0.5D) * 0.8D, 0, 0, 0);
            }
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4D, getZ(), 0, 0.02, 0);
        } else if (tickCount > MAX_LIFE) {
            detonate();
        }
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        Entity owner = getOwner();
        if (owner == null) return true;
        Entity t = target instanceof PartEntity<?> part ? part.getParent() : target;
        Entity mount = owner.getRootVehicle();
        return t != mount && t.getRootVehicle() != mount;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        target.hurt(damageSources().fireball(this, getOwner()), DZConfig.CANNON_DAMAGE.get().floatValue());
        target.setSecondsOnFire(isDestructive() ? 8 : 3);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && isAlive()) detonate();
    }

    private void detonate() {
        boolean destroy = isDestructive();
        float power = DZConfig.CANNON_POWER.get().floatValue();
        level().explode(this, getX(), getY(), getZ(), destroy ? power : power * 0.85F, destroy,
                destroy ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Destroy", isDestructive());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DESTROY, tag.getBoolean("Destroy"));
    }
}
