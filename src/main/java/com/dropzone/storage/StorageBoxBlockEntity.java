package com.dropzone.storage;

import com.dropzone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds up to {@code tier.types} item types with up to {@code tier.perType} of each.
 * Saved under "Storage" so the break loot table can copy it onto the dropped item.
 */
public class StorageBoxBlockEntity extends BlockEntity implements MenuProvider {
    public static final String TAG_STORAGE = "Storage";

    private final List<BoxEntry> entries = new ArrayList<>();
    private int version;
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler(this));

    public StorageBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORAGE_BOX_BE.get(), pos, state);
    }

    public BoxTier tier() {
        return getBlockState().getBlock() instanceof StorageBoxBlock box ? box.tier() : BoxTier.COPPER;
    }

    public List<BoxEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** Bumped on every change so open screens know to refresh. */
    public int version() {
        return version;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public static boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof StorageBoxItem); // no box-in-box
    }

    /** @return how many of {@code stack} were (or would be) accepted. Does not modify {@code stack}. */
    public int insert(ItemStack stack, boolean simulate) {
        if (!canStore(stack)) return 0;
        BoxTier tier = tier();
        for (BoxEntry e : entries) {
            if (e.matches(stack)) {
                int n = Math.min(stack.getCount(), tier.perType - e.count);
                if (n > 0 && !simulate) {
                    e.count += n;
                    changed();
                }
                return Math.max(0, n);
            }
        }
        if (entries.size() >= tier.types) return 0;
        int n = Math.min(stack.getCount(), tier.perType);
        if (!simulate) {
            entries.add(new BoxEntry(stack, n));
            changed();
        }
        return n;
    }

    /** Takes up to {@code amount} of entry {@code index}. */
    public ItemStack extract(int index, int amount, boolean simulate) {
        if (index < 0 || index >= entries.size() || amount <= 0) return ItemStack.EMPTY;
        BoxEntry e = entries.get(index);
        int n = Math.min(amount, e.count);
        ItemStack out = e.proto.copyWithCount(n);
        if (!simulate) {
            e.count -= n;
            if (e.count <= 0) entries.remove(index);
            changed();
        }
        return out;
    }

    public long totalItems() {
        long t = 0;
        for (BoxEntry e : entries) t += e.count;
        return t;
    }

    public int comparatorSignal() {
        BoxTier tier = tier();
        if (entries.isEmpty()) return 0;
        double fill = totalItems() / (double) ((long) tier.types * tier.perType);
        return 1 + (int) Math.floor(fill * 14.0D);
    }

    private void changed() {
        version++;
        setChanged();
    }

    // ---------------------------------------------------------------- save / load

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!entries.isEmpty()) tag.put(TAG_STORAGE, writeEntries(entries));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        entries.clear();
        entries.addAll(readEntries(tag.getList(TAG_STORAGE, Tag.TAG_COMPOUND)));
        version++;
    }

    public static ListTag writeEntries(List<BoxEntry> list) {
        ListTag out = new ListTag();
        for (BoxEntry e : list) {
            CompoundTag t = new CompoundTag();
            t.put("Item", e.proto.save(new CompoundTag()));
            t.putInt("Count", e.count);
            out.add(t);
        }
        return out;
    }

    public static List<BoxEntry> readEntries(ListTag list) {
        List<BoxEntry> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ItemStack proto = ItemStack.of(t.getCompound("Item"));
            int count = t.getInt("Count");
            if (!proto.isEmpty() && count > 0) out.add(new BoxEntry(proto, count));
        }
        return out;
    }

    // ---------------------------------------------------------------- menu

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StorageBoxMenu(id, inv, this);
    }

    // ---------------------------------------------------------------- hoppers & pipes

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    /** One slot per stored type plus one empty slot. Inserts go to the matching type wherever it is. */
    private record Handler(StorageBoxBlockEntity box) implements IItemHandler {
        @Override
        public int getSlots() {
            return box.entries.size() + 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= box.entries.size()) return ItemStack.EMPTY;
            BoxEntry e = box.entries.get(slot);
            return e.proto.copyWithCount(e.count);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int n = box.insert(stack, simulate);
            return n >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= box.entries.size()) return ItemStack.EMPTY;
            int max = Math.min(amount, box.entries.get(slot).proto.getMaxStackSize());
            return box.extract(slot, max, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return box.tier().perType;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canStore(stack);
        }
    }
}
