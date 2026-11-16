package com.dropzone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Applied, mounted and removed through SaddleEvents so it works before any mob's own right-click behaviour. */
public class CannonSaddleItem extends Item {
    public CannonSaddleItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(Tier.LEGENDARY.color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(Tier.LEGENDARY.translationKey()).withStyle(Tier.LEGENDARY.color));
        for (int i = 1; i <= 4; i++) {
            tooltip.add(Component.translatable("item.dropzone.cannon_saddle.desc" + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
