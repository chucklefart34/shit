package com.dropzone.storage;

import net.minecraft.world.item.ItemStack;

/** One stored item type: a single-count prototype stack and how many are stored. */
public final class BoxEntry {
    public final ItemStack proto;
    public int count;

    public BoxEntry(ItemStack proto, int count) {
        this.proto = proto.copyWithCount(1);
        this.count = count;
    }

    public boolean matches(ItemStack stack) {
        return ItemStack.isSameItemSameTags(proto, stack);
    }
}
