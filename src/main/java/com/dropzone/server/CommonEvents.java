package com.dropzone.server;

import com.dropzone.DropZone;
import com.dropzone.item.GliderItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DropZone.MODID)
public final class CommonEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        if (ServerState.isGliding(player)) {
            player.resetFallDistance();
            if (player.onGround() || player.isInWater() || player.isInLava() || player.isFallFlying()
                    || player.getAbilities().flying || player.isPassenger() || player.isSpectator()
                    || GliderItem.find(player).isEmpty()) {
                ServerState.setGliding(player, false, 0);
            }
        }

        ServerState.EmoteState emote = ServerState.getEmote(player);
        if (emote != null) {
            Vec3 p = player.position();
            double dx = p.x - emote.start().x;
            double dz = p.z - emote.start().z;
            if (dx * dx + dz * dz > 0.04D || Math.abs(p.y - emote.start().y) > 0.6D
                    || player.isUsingItem() || player.isShiftKeyDown() || player.isPassenger()) {
                ServerState.setEmote(player, -1);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerState.getEmote(player) != null) {
            ServerState.setEmote(player, -1);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerState.clearPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            ServerState.syncTo(viewer, target);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ServerState.clearPlayer(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ServerState.forget(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerState.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ServerState.clearAll();
    }

    private CommonEvents() {}
}
