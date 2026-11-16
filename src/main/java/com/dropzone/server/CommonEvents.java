package com.dropzone.server;

import com.dropzone.DropZone;
import com.dropzone.item.GliderItem;
import com.dropzone.item.GunItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
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

    /** Ammo you walk over goes straight into matching guns, even with a full inventory. */
    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemEntity itemEntity = event.getItem();
        ItemStack loot = itemEntity.getItem();
        if (loot.isEmpty()) return;
        Inventory inv = player.getInventory();
        int absorbed = 0;
        for (int i = 0; i < inv.getContainerSize() && !loot.isEmpty(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof GunItem gun && gun.stats().ammo().get() == loot.getItem()) {
                int stored = gun.store(s, loot.getCount());
                loot.shrink(stored);
                absorbed += stored;
            }
        }
        if (absorbed <= 0) return;
        inv.setChanged();
        player.take(itemEntity, absorbed);
        if (loot.isEmpty()) {
            itemEntity.discard();
            event.setCanceled(true);
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
