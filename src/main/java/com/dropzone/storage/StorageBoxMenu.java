package com.dropzone.storage;

import com.dropzone.network.DZNetwork;
import com.dropzone.network.S2CBoxContents;
import com.dropzone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Only the player's inventory uses real slots. The box grid is virtual: counts go far past what vanilla
 * slot packets can carry, so contents are synced with S2CBoxContents and clicks come back as C2SBoxClick.
 */
public class StorageBoxMenu extends AbstractContainerMenu {
    public static final int ROWS = 6;
    public static final int COLS = 9;

    public static final int LEFT = 0, RIGHT = 1, SHIFT = 2, DEPOSIT_ALL = 3;

    @Nullable
    private final StorageBoxBlockEntity box; // server only
    private final Player player;
    private final BlockPos pos;
    private final BoxTier tier;
    private int sentVersion = -1;
    private List<BoxEntry> clientEntries = new ArrayList<>();

    /** Client constructor (from the open-screen packet). */
    public StorageBoxMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(), BoxTier.byOrdinal(buf.readByte()));
    }

    /** Server constructor. */
    public StorageBoxMenu(int id, Inventory inv, StorageBoxBlockEntity box) {
        this(id, inv, box, box.getBlockPos(), box.tier());
    }

    private StorageBoxMenu(int id, Inventory inv, @Nullable StorageBoxBlockEntity box, BlockPos pos, BoxTier tier) {
        super(ModBlocks.STORAGE_BOX_MENU.get(), id);
        this.box = box;
        this.player = inv.player;
        this.pos = pos;
        this.tier = tier;
        int yOff = (ROWS - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOff));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 161 + yOff));
        }
    }

    public BoxTier tier() {
        return tier;
    }

    public List<BoxEntry> clientEntries() {
        return clientEntries;
    }

    public void setClientEntries(List<BoxEntry> entries) {
        this.clientEntries = entries;
    }

    @Override
    public boolean stillValid(Player p) {
        if (box == null) return true;
        return !box.isRemoved() && p.level().getBlockEntity(pos) == box
                && p.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    /** Shift-click in your inventory sends the stack into the box. */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (box == null) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int n = box.insert(stack, false);
        if (n > 0) {
            stack.shrink(n);
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (box != null && player instanceof ServerPlayer sp && box.version() != sentVersion) {
            sentVersion = box.version();
            DZNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new S2CBoxContents(containerId, box.entries()));
        }
    }

    /** Server: a click on the virtual grid. {@code index} is the entry clicked, or -1 for an empty cell. */
    public void handleBoxClick(ServerPlayer p, int index, int action) {
        if (box == null) return;
        ItemStack carried = getCarried();
        boolean valid = index >= 0 && index < box.entries().size();

        switch (action) {
            case LEFT -> {
                if (!carried.isEmpty()) {
                    carried.shrink(box.insert(carried, false));
                    setCarried(carried);
                } else if (valid) {
                    int max = box.entries().get(index).proto.getMaxStackSize();
                    setCarried(box.extract(index, max, false));
                }
            }
            case RIGHT -> {
                if (!carried.isEmpty()) {
                    if (box.insert(carried.copyWithCount(1), false) == 1) carried.shrink(1);
                    setCarried(carried);
                } else if (valid) {
                    BoxEntry e = box.entries().get(index);
                    int half = (Math.min(e.count, e.proto.getMaxStackSize()) + 1) / 2;
                    setCarried(box.extract(index, half, false));
                }
            }
            case SHIFT -> {
                if (valid) {
                    int max = box.entries().get(index).proto.getMaxStackSize();
                    ItemStack taken = box.extract(index, max, false);
                    if (!p.getInventory().add(taken) && !taken.isEmpty()) {
                        int back = box.insert(taken, false); // whatever didn't fit goes back in
                        taken.shrink(back);
                        if (!taken.isEmpty()) p.drop(taken, false);
                    }
                }
            }
            case DEPOSIT_ALL -> {
                // Quick-stack: move every inventory item whose type is already in the box (hotbar excluded).
                for (int i = 9; i < 36; i++) {
                    ItemStack s = p.getInventory().getItem(i);
                    if (s.isEmpty()) continue;
                    boolean known = box.entries().stream().anyMatch(e -> e.matches(s));
                    if (known) s.shrink(box.insert(s, false));
                }
                p.getInventory().setChanged();
            }
            default -> { }
        }
        broadcastChanges();
    }
}
