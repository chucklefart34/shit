package com.dropzone.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StorageBoxItem extends BlockItem {
    public StorageBoxItem(StorageBoxBlock block, Properties props) {
        super(block, props.stacksTo(1));
    }

    private BoxTier tier() {
        return ((StorageBoxBlock) getBlock()).tier();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BoxTier tier = tier();
        tooltip.add(Component.translatable("tooltip.dropzone.box_capacity", tier.types, String.format(java.util.Locale.ROOT, "%,d", tier.perType))
                .withStyle(ChatFormatting.GRAY));
        CompoundTag be = stack.getTagElement("BlockEntityTag");
        if (be != null && be.contains(StorageBoxBlockEntity.TAG_STORAGE)) {
            List<BoxEntry> entries = StorageBoxBlockEntity.readEntries(be.getList(StorageBoxBlockEntity.TAG_STORAGE, Tag.TAG_COMPOUND));
            long total = entries.stream().mapToLong(e -> e.count).sum();
            tooltip.add(Component.translatable("tooltip.dropzone.box_contents", entries.size(), String.format(java.util.Locale.ROOT, "%,d", total))
                    .withStyle(ChatFormatting.YELLOW));
            entries.stream().sorted((a, b) -> Integer.compare(b.count, a.count)).limit(5).forEach(e ->
                    tooltip.add(Component.literal("  ").append(e.proto.getHoverName()).append(" x" + String.format(java.util.Locale.ROOT, "%,d", e.count))
                            .withStyle(ChatFormatting.DARK_GRAY)));
            if (entries.size() > 5) {
                tooltip.add(Component.translatable("tooltip.dropzone.box_more", entries.size() - 5).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.dropzone.box_empty").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (tier.next() != null) {
            tooltip.add(Component.translatable("tooltip.dropzone.box_upgrade_hint",
                    Component.translatable("item.dropzone." + tier.next().id + "_box_upgrade")).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false; // no nesting inside shulker boxes / bundles
    }
}
