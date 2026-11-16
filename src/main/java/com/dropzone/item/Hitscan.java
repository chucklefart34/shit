package com.dropzone.item;

import com.dropzone.DZConfig;
import com.dropzone.registry.ModDamage;
import com.dropzone.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Server-side instant-hit bullet tracing. Pellet damage is summed per target so shotguns hit once. */
public final class Hitscan {
    private static final DustParticleOptions TRACER = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.45F), 0.5F);

    public static void fire(ServerPlayer shooter, GunStats stats, float spreadDegrees) {
        ServerLevel level = shooter.serverLevel();
        RandomSource random = shooter.getRandom();
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getViewVector(1.0F);

        Map<Entity, Float> damage = new LinkedHashMap<>();
        Set<Entity> headshots = new HashSet<>();

        for (int i = 0; i < stats.pellets(); i++) {
            Vec3 dir = applySpread(look, spreadDegrees, random);
            Vec3 end = eye.add(dir.scale(stats.range()));

            BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            Vec3 blockEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

            Target target = findTarget(shooter, eye, blockEnd);
            Vec3 impact;
            if (target != null) {
                impact = target.pos;
                float dmg = stats.damageAt(eye.distanceTo(impact));
                if (isHeadshot(target.entity, impact)) {
                    dmg *= stats.headshotMult();
                    headshots.add(target.entity);
                }
                damage.merge(target.entity, dmg, Float::sum);
                level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 3, 0.05, 0.05, 0.05, 0.15);
            } else if (blockHit.getType() == HitResult.Type.BLOCK) {
                impact = blockHit.getLocation();
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            impact.x, impact.y, impact.z, 5, 0.04, 0.04, 0.04, 0.12);
                }
                level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 1, 0.0, 0.0, 0.0, 0.01);
                tryShatterGlass(shooter, level, pos, state);
            } else {
                impact = end;
            }
            drawTracer(level, eye, impact, dir, stats.pellets() > 1);
        }

        if (damage.isEmpty()) return;

        DamageSource source = ModDamage.bullet(level, shooter);
        float multiplier = DZConfig.DAMAGE_MULTIPLIER.get().floatValue();
        boolean anyHit = false;
        boolean anyHead = false;
        for (Map.Entry<Entity, Float> e : damage.entrySet()) {
            Entity target = e.getKey();
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            if (target.hurt(source, e.getValue() * multiplier)) {
                anyHit = true;
                anyHead |= headshots.contains(target);
                // Scale the vanilla knockback so automatic weapons do not juggle targets.
                Vec3 kb = target.getDeltaMovement().subtract(before).scale(stats.knockback());
                target.setDeltaMovement(before.add(kb));
                target.hurtMarked = true;
            }
        }
        if (anyHit) {
            shooter.playNotifySound(anyHead ? ModSounds.HEADSHOT.get() : ModSounds.HITMARKER.get(),
                    SoundSource.PLAYERS, anyHead ? 0.9F : 0.6F, 1.0F);
        }
    }

    private record Target(Entity entity, Vec3 pos) {}

    private static Target findTarget(ServerPlayer shooter, Vec3 from, Vec3 to) {
        AABB sweep = new AABB(from, to).inflate(1.0D);
        Entity rootVehicle = shooter.getRootVehicle();
        Target best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : shooter.level().getEntities(shooter, sweep,
                e -> e.isAlive() && e.isPickable() && !e.isSpectator() && e.getRootVehicle() != rootVehicle)) {
            AABB box = e.getBoundingBox().inflate(Math.max(0.1D, e.getPickRadius()));
            if (box.contains(from)) {
                return new Target(e, from);
            }
            Optional<Vec3> hit = box.clip(from, to);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = new Target(e, hit.get());
                }
            }
        }
        return best;
    }

    private static boolean isHeadshot(Entity entity, Vec3 impact) {
        if (!(entity instanceof LivingEntity living)) return false;
        double eyeY = living.getEyeY();
        double top = living.getBoundingBox().maxY;
        double headBottom = eyeY - Math.max(0.2D, (top - eyeY) * 1.25D);
        return impact.y >= headBottom;
    }

    private static void tryShatterGlass(ServerPlayer shooter, ServerLevel level, BlockPos pos, BlockState state) {
        if (!DZConfig.BULLETS_BREAK_GLASS.get()) return;
        if (!(state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES))) return;
        if (!shooter.mayBuild() || !level.mayInteract(shooter, pos)) return;
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, shooter))) return;
        level.destroyBlock(pos, false, shooter);
    }

    private static void drawTracer(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir, boolean light) {
        double length = from.distanceTo(to);
        double step = light ? 2.5D : 1.5D;
        int max = light ? 6 : 40;
        int n = 0;
        for (double d = 2.0D; d < length && n < max; d += step, n++) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(TRACER, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static Vec3 applySpread(Vec3 dir, float degrees, RandomSource random) {
        if (degrees <= 0.0F) return dir;
        double maxAngle = Math.toRadians(degrees);
        double angle = Math.sqrt(random.nextDouble()) * maxAngle;
        double roll = random.nextDouble() * Math.PI * 2.0D;
        Vec3 up = Math.abs(dir.y) > 0.99D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 up2 = right.cross(dir).normalize();
        double t = Math.tan(angle);
        return dir.add(right.scale(Math.cos(roll) * t)).add(up2.scale(Math.sin(roll) * t)).normalize();
    }

    private Hitscan() {}
}
