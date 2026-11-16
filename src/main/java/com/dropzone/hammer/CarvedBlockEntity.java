package com.dropzone.hammer;

import com.dropzone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Remembers which block was carved and which of its 8 octants are still there (bit i = octant i). */
public class CarvedBlockEntity extends BlockEntity {
    private BlockState source = Blocks.STONE.defaultBlockState();
    private int mask = 0xFF;

    public CarvedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CARVED_BE.get(), pos, state);
    }

    public BlockState source() {
        return source;
    }

    public int mask() {
        return mask;
    }

    public void init(BlockState source, int mask) {
        this.source = source;
        this.mask = mask & 0xFF;
        sync();
    }

    public void setMask(int mask) {
        this.mask = mask & 0xFF;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            requestModelDataUpdate();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Source", NbtUtils.writeBlockState(source));
        tag.putInt("Mask", mask);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        source = NbtUtils.readBlockState(level != null ? level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup(),
                tag.getCompound("Source"));
        mask = tag.contains("Mask") ? tag.getInt("Mask") & 0xFF : 0xFF;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
