package com.dropzone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Bandages and med kits restore health up to a cap; shield potions grant absorption up to a cap. */
public class HealingItem extends Item {
    public enum Kind { HEALTH, SHIELD }

    private final Kind kind;
    private final float amount;
    private final float cap;
    private final int useTicks;
    private final UseAnim anim;
    private final Tier tier;

    /**
     * @param cap for HEALTH, the fraction of max health it can heal up to; for SHIELD, the absorption ceiling in half-hearts
     */
    public HealingItem(Kind kind, float amount, float cap, int useTicks, UseAnim anim, Tier tier, Properties props) {
        super(props);
        this.kind = kind;
        this.amount = amount;
        this.cap = cap;
        this.useTicks = useTicks;
        this.anim = anim;
        this.tier = tier;
    }

    private boolean canUse(Player player) {
        return kind == Kind.HEALTH
                ? player.getHealth() < player.getMaxHealth() * cap - 0.01F
                : player.getAbsorptionAmount() < cap - 0.01F;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canUse(player)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable(kind == Kind.HEALTH
                        ? "message.dropzone.health_full" : "message.dropzone.shield_full").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && kind == Kind.HEALTH && remaining % 10 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER,
                    SoundSource.PLAYERS, 0.6F, 1.1F + level.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (!level.isClientSide) {
            if (kind == Kind.HEALTH) {
                float target = player.getMaxHealth() * cap;
                float missing = target - player.getHealth();
                if (missing > 0) player.heal(Math.min(amount, missing));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.35F, 1.8F);
            } else {
                player.setAbsorptionAmount(Math.min(cap, player.getAbsorptionAmount() + amount));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (kind == Kind.SHIELD) {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) return bottle;
                if (!player.getInventory().add(bottle)) player.drop(bottle, false);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return useTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return anim;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(tier.color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tier.translationKey()).withStyle(tier.color));
        String hearts = amount / 2.0F == (int) (amount / 2.0F) ? Integer.toString((int) (amount / 2.0F)) : Float.toString(amount / 2.0F);
        if (kind == Kind.HEALTH) {
            tooltip.add(Component.translatable("tooltip.dropzone.heals", hearts, Math.round(cap * 100) + "%").withStyle(ChatFormatting.RED));
        } else {
            String max = Integer.toString((int) (cap / 2.0F));
            tooltip.add(Component.translatable("tooltip.dropzone.shields", hearts, max).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.dropzone.use_time", String.format(java.util.Locale.ROOT, "%.1f", useTicks / 20.0F))
                .withStyle(ChatFormatting.GRAY));
    }
}
