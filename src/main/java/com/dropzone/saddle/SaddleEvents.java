package com.dropzone.saddle;

import com.dropzone.DZConfig;
import com.dropzone.DropZone;
import com.dropzone.entity.CannonFireballEntity;
import com.dropzone.mixin.LivingEntityAccessor;
import com.dropzone.network.DZNetwork;
import com.dropzone.network.S2CSaddleState;
import com.dropzone.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = DropZone.MODID)
public final class SaddleEvents {

    // ---------------------------------------------------------------- saddling, mounting, unsaddling

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.isSpectator()) return;
        Entity target = CannonSaddle.root(event.getTarget());
        if (!(target instanceof LivingEntity) || target instanceof Player) return;
        ItemStack held = event.getItemStack();
        Level level = player.level();

        // Put a saddle on.
        if (held.is(ModItems.CANNON_SADDLE.get()) && !CannonSaddle.has(target)) {
            if (!level.isClientSide) {
                CannonSaddle.set(target, true, DZConfig.CANNON_ALLOW_DESTRUCTION.get());
                if (target instanceof net.minecraft.world.entity.Mob mob) mob.setPersistenceRequired();
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
            cancel(event, level);
            return;
        }

        if (!CannonSaddle.has(target) || event.getHand() != InteractionHand.MAIN_HAND || !held.isEmpty()) return;

        if (player.isShiftKeyDown()) { // take the saddle back off
            if (!level.isClientSide) {
                target.ejectPassengers();
                CannonSaddle.set(target, false, false);
                target.spawnAtLocation(new ItemStack(ModItems.CANNON_SADDLE.get()), 1.0F);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.NEUTRAL, 1.0F, 0.8F);
            }
            cancel(event, level);
        } else if (!target.isVehicle() && player.getVehicle() != target) { // climb on
            if (!level.isClientSide) player.startRiding(target, true);
            cancel(event, level);
        }
    }

    private static void cancel(PlayerInteractEvent.EntityInteract event, Level level) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.level().isClientSide && !(e instanceof Player) && CannonSaddle.has(e)) {
            e.spawnAtLocation(new ItemStack(ModItems.CANNON_SADDLE.get()), 1.0F);
            CannonSaddle.set(e, false, false);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (event.getEntity() instanceof ServerPlayer viewer && CannonSaddle.has(target)) {
            DZNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                    new S2CSaddleState(target.getId(), true, CannonSaddle.destructive(target)));
        }
    }

    // ---------------------------------------------------------------- safety: your cannon and your mount don't hurt you

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getExploder() instanceof CannonFireballEntity ball) || ball.getOwner() == null) return;
        Entity owner = ball.getOwner();
        Entity mount = owner.getRootVehicle();
        event.getAffectedEntities().removeIf(e -> {
            Entity r = CannonSaddle.root(e);
            return r == owner || r == mount || r.getRootVehicle() == mount;
        });
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || !victim.isPassenger()) return;
        Entity mount = victim.getRootVehicle();
        if (CannonSaddle.has(mount) && CannonSaddle.root(attacker) == mount) {
            event.setCanceled(true); // e.g. the dragon's own wing buffets
        }
    }

    // ---------------------------------------------------------------- the Ender Dragon is flown server-side

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || dragon.level().isClientSide) return;
        if (!CannonSaddle.has(dragon) || !(dragon.getFirstPassenger() instanceof ServerPlayer rider)) return;

        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
        dragon.getPhaseManager().getPhase(EnderDragonPhase.HOVERING).begin(); // forget the old hover spot

        Vec3 look = rider.getLookAngle();
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        double speed = 1.2D;
        Vec3 move = look.scale(rider.zza * speed).add(side.scale(-rider.xxa * speed * 0.5D));
        if (((LivingEntityAccessor) rider).dropzone$isJumping()) move = move.add(0, 0.5D, 0);
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.setPos(dragon.getX() + move.x, dragon.getY() + move.y, dragon.getZ() + move.z);
        dragon.setYRot(rider.getYRot() + 180.0F); // the dragon model faces backwards relative to yRot
        dragon.yBodyRot = dragon.getYRot();
    }

    // ---------------------------------------------------------------- day counter

    private static long lastDay = -1;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !DZConfig.DAY_TITLE.get()) return;
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        long day = overworld.getDayTime() / 24000L;
        if (lastDay < 0) {
            lastDay = day;
            return;
        }
        if (day == lastDay) return;
        lastDay = day;
        Component title = Component.translatable("title.dropzone.day", day + 1).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        Component subtitle = Component.translatable("title.dropzone.day.sub").withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.6F, 0.7F);
        }
    }

    public static void resetDay() {
        lastDay = -1;
    }

    private SaddleEvents() {}
}
