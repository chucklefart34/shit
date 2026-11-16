package com.dropzone.trash;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Terraria-style trash: drop an item in and it waits there (you can still take it back) until something
 * else goes in on top, at which point the old item is deleted for good instead of swapping into your hand.
 */
public class TrashSlot extends Slot {
    public static final int X = 152;
    public static final int Y = 62;

    private final Player owner;

    public TrashSlot(Container trash, Player owner) {
        super(trash, 0, X, Y);
        this.owner = owner;
    }

    @Override
    public boolean mayPlace(ItemStack incoming) {
        ItemStack current = getItem();
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, incoming)) {
            // The click handler already holds a reference to this stack and would swap it into the cursor.
            // Emptying it here means the swap hands back nothing: the old item is gone forever.
            current.setCount(0);
            set(ItemStack.EMPTY);
        }
        return true;
    }

    /** Creative mode has its own destroy slot, and its inventory tab would put this on top of the hotbar. */
    @Override
    public boolean isActive() {
        return !owner.getAbilities().instabuild;
    }
}
