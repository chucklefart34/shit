package com.dropzone.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Right-click a placed box of tier {@code from} to turn it into the next tier, contents and all. */
public class BoxUpgradeItem extends Item {
    private final BoxTier from;

    public BoxUpgradeItem(BoxTier from, Properties props) {
        super(props);
        this.from = from;
    }

    public BoxTier from() {
        return from;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BoxTier to = from.next();
        tooltip.add(Component.translatable("tooltip.dropzone.upgrade_use",
                Component.translatable("tier.dropzone.box." + from.id), Component.translatable("tier.dropzone.box." + to.id)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dropzone.box_capacity", to.types, String.format(java.util.Locale.ROOT, "%,d", to.perType))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
