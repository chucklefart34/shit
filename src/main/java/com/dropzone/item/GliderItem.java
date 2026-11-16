package com.dropzone.item;

import com.dropzone.DropZone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Carry anywhere in your inventory. Press jump while falling to deploy, again to stow. */
public class GliderItem extends Item {
    private final ResourceLocation texture;
    private final Tier tier;

    public GliderItem(String color, Tier tier, Properties props) {
        super(props.stacksTo(1));
        this.texture = new ResourceLocation(DropZone.MODID, "textures/entity/glider/" + color + ".png");
        this.tier = tier;
    }

    public ResourceLocation texture() {
        return texture;
    }

    /** Offhand first, then hotbar and main inventory. */
    public static ItemStack find(Player player) {
        if (player.getOffhandItem().getItem() instanceof GliderItem) return player.getOffhandItem();
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) {
            if (s.getItem() instanceof GliderItem) return s;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(tier.color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tier.translationKey()).withStyle(tier.color));
        tooltip.add(Component.translatable("item.dropzone.glider.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.dropzone.glider.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
