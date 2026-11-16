package com.dropzone.item;

import com.dropzone.DZConfig;
import com.dropzone.entity.RocketEntity;
import com.dropzone.registry.ModSounds;
import com.dropzone.server.ServerState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Left-click fires (handled client-side, validated on the server), right-click aims down sights,
 * R reloads. Magazine contents and reload timer live in the stack's NBT.
 */
public class GunItem extends Item {
    public static final String TAG_AMMO = "Ammo";
    public static final String TAG_RELOAD_END = "ReloadEnd";
    public static final String TAG_RELOAD_LEN = "ReloadLen";
    public static final String TAG_RESERVE = "Reserve";

    /** Set by the client at startup so item bars can show reload progress. */
    public static LongSupplier clientGameTime = () -> 0L;

    private final GunStats stats;
    private final Tier tier;

    public GunItem(GunStats stats, Tier tier, Properties props) {
        super(props.stacksTo(1));
        this.stats = stats;
        this.tier = tier;
    }

    public GunStats stats() {
        return stats;
    }

    public Tier tier() {
        return tier;
    }

    // ---------------------------------------------------------------- magazine state

    public int getAmmo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_AMMO) ? tag.getInt(TAG_AMMO) : stats.magSize();
    }

    public void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt(TAG_AMMO, Mth.clamp(ammo, 0, stats.magSize()));
    }

    public static boolean isReloading(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_RELOAD_END);
    }

    private static void cancelReload(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_RELOAD_END);
            tag.remove(TAG_RELOAD_LEN);
        }
    }

    public static boolean hasInfiniteAmmo(Player player) {
        return !DZConfig.REQUIRE_AMMO.get() || (player.getAbilities().instabuild && DZConfig.CREATIVE_INFINITE_AMMO.get());
    }

    // ---------------------------------------------------------------- reserve ammo stored inside the gun

    public static int capacity() {
        return DZConfig.GUN_AMMO_CAPACITY.get();
    }

    public int getReserve(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_RESERVE) : 0;
    }

    public void setReserve(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt(TAG_RESERVE, Math.max(0, amount));
    }

    /** Puts up to {@code amount} rounds into this gun's reserve; returns how many fit. */
    public int store(ItemStack gunStack, int amount) {
        int fit = Math.min(amount, capacity() - getReserve(gunStack));
        if (fit <= 0) return 0;
        setReserve(gunStack, getReserve(gunStack) + fit);
        return fit;
    }

    /** Pulls loose ammo items of the right type out of the inventory into this gun. */
    public void absorbLooseAmmo(Player player, ItemStack gunStack) {
        Item ammo = stats.ammo().get();
        Inventory inv = player.getInventory();
        boolean changed = false;
        for (int i = 0; i < inv.getContainerSize() && getReserve(gunStack) < capacity(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ammo)) {
                int stored = store(gunStack, s.getCount());
                if (stored > 0) {
                    s.shrink(stored);
                    changed = true;
                }
            }
        }
        if (changed) inv.setChanged();
    }

    /** Total rounds available for reloading: this gun's reserve plus any loose ammo items. */
    public int countReserve(Player player, ItemStack gunStack) {
        Item ammo = stats.ammo().get();
        Inventory inv = player.getInventory();
        int total = getReserve(gunStack);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ammo)) total += s.getCount();
        }
        return total;
    }

    private int takeReserve(Player player, ItemStack gunStack, int wanted) {
        int fromGun = Math.min(wanted, getReserve(gunStack));
        setReserve(gunStack, getReserve(gunStack) - fromGun);
        int taken = fromGun;
        Item ammo = stats.ammo().get();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && taken < wanted; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ammo)) {
                int n = Math.min(wanted - taken, s.getCount());
                s.shrink(n);
                taken += n;
            }
        }
        if (taken > 0) inv.setChanged();
        return taken;
    }

    // ---------------------------------------------------------------- server actions

    /** Called from the fire packet. Validates rate of fire, ammo and reload state. */
    public void tryFire(ServerPlayer player, ItemStack stack) {
        if (player.isSpectator() || isReloading(stack)) return;
        long now = player.level().getGameTime();
        if (!ServerState.canShoot(player, now)) return;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            tryStartReload(player, stack);
            return;
        }
        ServerState.setNextShot(player, now + stats.cooldown());
        setAmmo(stack, ammo - 1);
        shoot(player, stack);
        if (ammo - 1 <= 0) {
            tryStartReload(player, stack);
        }
    }

    private void shoot(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        boolean aiming = player.isUsingItem() && player.getUseItem() == stack;
        float spread = aiming ? stats.adsSpread() : stats.hipSpread();
        if (!player.onGround()) spread *= 1.6F;
        else if (player.isSprinting()) spread *= 1.3F;
        else if (player.isCrouching()) spread *= 0.75F;

        Vec3 look = player.getViewVector(1.0F);
        if (stats.projectile()) {
            RocketEntity rocket = new RocketEntity(level, player);
            Vec3 start = player.getEyePosition().add(look.scale(0.8D)).subtract(0.0D, 0.15D, 0.0D);
            rocket.setPos(start.x, start.y, start.z);
            rocket.shoot(look.x, look.y, look.z, 2.0F, spread);
            level.addFreshEntity(rocket);
        } else {
            Hitscan.fire(player, stats, spread);
        }

        // Everyone except the shooter hears the server copy; the shooter's client plays it instantly.
        level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), stats.sound().get(),
                SoundSource.PLAYERS, stats.soundVolume(), 0.95F + player.getRandom().nextFloat() * 0.1F);
        Vec3 muzzle = player.getEyePosition().add(look.scale(1.0D));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y - 0.1D, muzzle.z, 2, 0.02, 0.02, 0.02, 0.01);
        level.gameEvent(player, GameEvent.PROJECTILE_SHOOT, player.position());
    }

    public boolean tryStartReload(ServerPlayer player, ItemStack stack) {
        if (isReloading(stack) || getAmmo(stack) >= stats.magSize()) return false;
        if (!hasInfiniteAmmo(player) && countReserve(player, stack) <= 0) {
            player.displayClientMessage(Component.translatable("message.dropzone.no_ammo",
                    stats.ammo().get().getDescription()).withStyle(ChatFormatting.RED), true);
            return false;
        }
        long now = player.level().getGameTime();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TAG_RELOAD_END, now + stats.reloadTicks());
        tag.putInt(TAG_RELOAD_LEN, stats.reloadTicks());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RELOAD_START.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    private void finishReload(Player player, ItemStack stack) {
        cancelReload(stack);
        int ammo = getAmmo(stack);
        int need = stats.magSize() - ammo;
        int got = hasInfiniteAmmo(player) ? need : takeReserve(player, stack, need);
        setAmmo(stack, ammo + got);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RELOAD_END.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (player.tickCount % 5 == 0) absorbLooseAmmo(player, stack);
        if (!isReloading(stack)) return;
        if (player.getMainHandItem() != stack) {
            cancelReload(stack); // swapping weapons cancels the reload, like the real thing
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && level.getGameTime() >= tag.getLong(TAG_RELOAD_END)) {
            finishReload(player, stack);
        }
    }

    // ---------------------------------------------------------------- aiming (right click)

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // ---------------------------------------------------------------- polish

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_RELOAD_END)) {
            long remaining = tag.getLong(TAG_RELOAD_END) - clientGameTime.getAsLong();
            int len = Math.max(1, tag.getInt(TAG_RELOAD_LEN));
            float progress = 1.0F - Mth.clamp(remaining / (float) len, 0.0F, 1.0F);
            return Math.round(13.0F * progress);
        }
        return Math.round(13.0F * getAmmo(stack) / (float) stats.magSize());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (isReloading(stack)) return 0xFFAA00;
        float f = getAmmo(stack) / (float) stats.magSize();
        return f <= 0.25F ? 0xFF4040 : 0xE8E8E8;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(tier.color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tier.translationKey()).withStyle(tier.color));
        if (stats.projectile()) {
            tooltip.add(stat("explosive", Component.translatable("tooltip.dropzone.yes")));
        } else {
            String dmg = fmt(stats.damage()) + (stats.pellets() > 1 ? " x" + stats.pellets() : "");
            tooltip.add(stat("damage", Component.literal(dmg)));
            tooltip.add(stat("headshot", Component.literal("x" + fmt(stats.headshotMult()))));
        }
        tooltip.add(stat("fire_rate", Component.literal(fmt(stats.shotsPerSecond()) + "/s"
                + (stats.automatic() ? " " : "")).append(stats.automatic()
                ? Component.translatable("tooltip.dropzone.auto") : Component.empty())));
        tooltip.add(stat("magazine", Component.literal(getAmmo(stack) + " / " + stats.magSize())));
        tooltip.add(stat("reserve", Component.literal(getReserve(stack) + " / " + capacity())));
        tooltip.add(stat("reload", Component.literal(fmt(stats.reloadTicks() / 20.0F) + "s")));
        tooltip.add(stat("ammo", stats.ammo().get().getDescription().copy()));
        tooltip.add(Component.translatable("tooltip.dropzone.gun_controls").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component stat(String key, Component value) {
        return Component.translatable("tooltip.dropzone." + key).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    private static String fmt(float f) {
        return f == (int) f ? Integer.toString((int) f) : String.format(java.util.Locale.ROOT, "%.1f", f);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return hand == InteractionHand.MAIN_HAND ? HumanoidModel.ArmPose.CROSSBOW_HOLD : HumanoidModel.ArmPose.ITEM;
            }
        });
    }
}
