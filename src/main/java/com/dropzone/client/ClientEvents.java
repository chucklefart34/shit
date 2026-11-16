package com.dropzone.client;

import com.dropzone.DropZone;
import com.dropzone.emote.Emote;
import com.dropzone.item.GliderItem;
import com.dropzone.item.GunItem;
import com.dropzone.network.C2SEmote;
import com.dropzone.network.C2SFireGun;
import com.dropzone.network.C2SGlider;
import com.dropzone.network.C2SReload;
import com.dropzone.network.DZNetwork;
import com.dropzone.registry.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = DropZone.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static int clientTicks;
    private static long nextShotTick;
    private static boolean wasAttackDown;
    private static boolean wasJumpDown;

    // Client-side magazine prediction so the counter and trigger react before the server confirms.
    private static int lastSeenAmmo = -1;
    private static int shotsSinceSync;
    private static int lastShotTick;
    private static int lastSlot = -1;

    // ---------------------------------------------------------------- helpers

    public static boolean isAiming(Player p) {
        return p.isUsingItem() && p.getUseItem().getItem() instanceof GunItem;
    }

    public static boolean isScoped() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.options.getCameraType().isFirstPerson() && isAiming(mc.player)
                && ((GunItem) mc.player.getUseItem().getItem()).stats().zoom() < 0.5F;
    }

    public static int displayedAmmo(ItemStack stack, GunItem gun) {
        return Math.max(0, gun.getAmmo(stack) - shotsSinceSync);
    }

    private static void play(Minecraft mc, Player p, SoundEvent sound, float volume, float pitch) {
        if (mc.level != null) {
            mc.level.playLocalSound(p.getX(), p.getEyeY(), p.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
        }
    }

    // ---------------------------------------------------------------- input / tick

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            wasAttackDown = false;
            wasJumpDown = false;
            return;
        }
        clientTicks++;

        while (Keys.RELOAD.consumeClick()) {
            if (player.getMainHandItem().getItem() instanceof GunItem) DZNetwork.sendToServer(new C2SReload());
        }
        while (Keys.EMOTE_WHEEL.consumeClick()) {
            if (mc.screen == null && !player.isSpectator()) mc.setScreen(new EmoteWheelScreen());
        }

        handleGun(mc, player);
        handleGliderInput(mc, player);
        handleOwnEmote(player);
        playDanceBeats(mc);
    }

    private static void handleGun(Minecraft mc, LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        boolean attackDown = mc.screen == null && mc.mouseHandler.isMouseGrabbed() && mc.options.keyAttack.isDown();
        boolean pressed = attackDown && !wasAttackDown;
        wasAttackDown = attackDown;

        if (!(stack.getItem() instanceof GunItem gun) || player.isSpectator()) {
            lastSeenAmmo = -1;
            shotsSinceSync = 0;
            return;
        }

        // Re-sync the prediction whenever the server's value arrives or we swap slots.
        int nbtAmmo = gun.getAmmo(stack);
        int slot = player.getInventory().selected;
        if (nbtAmmo != lastSeenAmmo || slot != lastSlot || clientTicks - lastShotTick > 10) {
            lastSeenAmmo = nbtAmmo;
            lastSlot = slot;
            shotsSinceSync = 0;
        }

        if (!attackDown || clientTicks < nextShotTick) return;
        if (!gun.stats().automatic() && !pressed) return;
        if (GunItem.isReloading(stack)) return;

        if (displayedAmmo(stack, gun) > 0) {
            DZNetwork.sendToServer(new C2SFireGun());
            nextShotTick = clientTicks + gun.stats().cooldown();
            shotsSinceSync++;
            lastShotTick = clientTicks;
            fireEffects(mc, player, gun);
        } else if (pressed) {
            play(mc, player, ModSounds.DRY_FIRE.get(), 0.7F, 1.0F);
            DZNetwork.sendToServer(new C2SReload());
            nextShotTick = clientTicks + 8;
        }
    }

    private static void fireEffects(Minecraft mc, LocalPlayer player, GunItem gun) {
        float pitch = 0.95F + player.getRandom().nextFloat() * 0.1F;
        play(mc, player, gun.stats().sound().get(), gun.stats().soundVolume(), pitch);

        float recoil = gun.stats().recoil() * (isAiming(player) ? 0.6F : 1.0F);
        player.setXRot(Mth.clamp(player.getXRot() - recoil, -90.0F, 90.0F));
        player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * recoil * 0.5F);

        Vec3 look = player.getViewVector(1.0F);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 muzzle = player.getEyePosition().add(look.scale(0.9D)).add(right.scale(0.25D)).subtract(0, 0.18D, 0);
        if (mc.level != null) {
            mc.level.addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, look.x * 0.05, 0.02, look.z * 0.05);
        }
    }

    private static void handleGliderInput(Minecraft mc, LocalPlayer player) {
        boolean jumpDown = mc.screen == null && mc.options.keyJump.isDown();
        boolean pressed = jumpDown && !wasJumpDown;
        wasJumpDown = jumpDown;
        if (!pressed) return;

        if (ClientState.isGliding(player.getId())) {
            DZNetwork.sendToServer(new C2SGlider(false));
        } else if (canDeploy(player)) {
            DZNetwork.sendToServer(new C2SGlider(true));
        }
    }

    private static boolean canDeploy(LocalPlayer p) {
        return !p.onGround() && p.getDeltaMovement().y < -0.2D && p.fallDistance > 1.5F
                && !p.isFallFlying() && !p.isInWater() && !p.isInLava() && !p.onClimbable()
                && !p.getAbilities().flying && !p.isPassenger() && !p.isSpectator()
                && !p.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(p) // elytra wins
                && !GliderItem.find(p).isEmpty();
    }

    private static void handleOwnEmote(LocalPlayer player) {
        if (ClientState.getEmote(player.getId()) == null) return;
        if (player.input.forwardImpulse != 0 || player.input.leftImpulse != 0 || player.input.jumping) {
            DZNetwork.sendToServer(new C2SEmote(-1));
            ClientState.setEmote(player.getId(), -1);
        }
    }

    private static void playDanceBeats(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        for (Map.Entry<Integer, ClientState.EmoteInstance> e : ClientState.emotes().entrySet()) {
            Emote emote = e.getValue().emote();
            int[] melody = emote.melody();
            if (!emote.isDance() || melody == null) continue;
            Entity ent = mc.level.getEntity(e.getKey());
            if (!(ent instanceof Player dancer) || dancer.distanceToSqr(mc.player) > 24 * 24) continue;
            int t = dancer.tickCount - e.getValue().startTick();
            if (t < 0) continue;
            double x = dancer.getX(), y = dancer.getY() + 1.0D, z = dancer.getZ();
            if (t % 10 == 0) {
                mc.level.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 0.5F, 1.0F, false);
            } else if (t % 10 == 5) {
                mc.level.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35F, 1.0F, false);
            }
            if (t % 5 == 0) {
                int note = melody[(t / 5) % melody.length];
                if (note >= 0) {
                    float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
                    SoundEvent instrument = switch (emote) {
                        case ROBOT -> SoundEvents.NOTE_BLOCK_BIT.value();
                        case DISCO -> SoundEvents.NOTE_BLOCK_BELL.value();
                        default -> SoundEvents.NOTE_BLOCK_PLING.value();
                    };
                    mc.level.playLocalSound(x, y, z, instrument, SoundSource.PLAYERS, 0.45F, pitch, false);
                }
            }
        }
    }

    // ---------------------------------------------------------------- glider physics (local player only)

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.CLIENT) return;
        if (!(event.player instanceof LocalPlayer player) || player != Minecraft.getInstance().player) return;
        if (!ClientState.isGliding(player.getId())) return;
        if (player.onGround() || player.isInWater() || player.isFallFlying() || player.getAbilities().flying) return;

        Vec3 v = player.getDeltaMovement();
        final double maxFall = 0.13D;
        double vy = v.y < -maxFall ? Math.max(v.y, Math.min(-maxFall, v.y * 0.7D)) : v.y;

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        if (forward.lengthSqr() > 1.0E-4D) forward = forward.normalize();
        float input = player.input.forwardImpulse;
        double push = 0.045D + (input > 0 ? 0.03D : 0) - (input < 0 ? 0.04D : 0);
        Vec3 h = new Vec3(v.x, 0, v.z).add(forward.scale(push));
        double maxSpeed = 0.62D;
        if (h.length() > maxSpeed) h = h.normalize().scale(maxSpeed);

        player.setDeltaMovement(h.x, vy, h.z);
        player.resetFallDistance();
    }

    // ---------------------------------------------------------------- guns don't punch or mine

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && event.isAttack() && player.getItemInHand(event.getHand()).getItem() instanceof GunItem) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    // ---------------------------------------------------------------- aim zoom, scope, hand

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Player p = event.getPlayer();
        if (isAiming(p)) {
            GunItem gun = (GunItem) p.getUseItem().getItem();
            event.setNewFovModifier(event.getFovModifier() * gun.stats().zoom());
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isScoped()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()) && isScoped()) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- glider rendering

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Integer itemId = ClientState.gliderItem(player.getId());
        if (itemId == null) return;
        Item item = Item.byId(itemId);
        if (!(item instanceof GliderItem glider)) return;

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        ps.translate(0.0D, 3.0D, 0.0D);
        ps.mulPose(Axis.XP.rotationDegrees(8.0F)); // nose slightly down, into the glide
        ps.scale(-1.0F, -1.0F, 1.0F);
        ClientSetup.gliderModel().renderToBuffer(ps,
                event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(glider.texture())),
                event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        ps.popPose();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.clear();
        lastSeenAmmo = -1;
        shotsSinceSync = 0;
    }

    private ClientEvents() {}
}
