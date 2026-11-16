package com.dropzone.hammer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A block carved into eighths by the Hammer. Has no item of its own: it looks, sounds, breaks and drops
 * like the block it was carved from.
 */
public class CarvedBlock extends BaseEntityBlock {
    private static final VoxelShape[] SHAPES = new VoxelShape[256];

    public CarvedBlock(Properties props) {
        super(props);
    }

    public static VoxelShape shapeFor(int mask) {
        mask &= 0xFF;
        VoxelShape s = SHAPES[mask];
        if (s == null) {
            s = Shapes.empty();
            for (int i = 0; i < 8; i++) {
                if ((mask & (1 << i)) == 0) continue;
                double x = (i & 1) * 8, y = ((i >> 1) & 1) * 8, z = ((i >> 2) & 1) * 8;
                s = Shapes.joinUnoptimized(s, Block.box(x, y, z, x + 8, y + 8, z + 8), BooleanOp.OR);
            }
            s = s.optimize();
            SHAPES[mask] = s;
        }
        return s;
    }

    private static CarvedBlockEntity be(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CarvedBlockEntity c ? c : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        CarvedBlockEntity c = be(level, pos);
        return c != null ? shapeFor(c.mask()) : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // drawn by CarvedBlockRenderer from the source block's model
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CarvedBlockEntity(pos, state);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        CarvedBlockEntity c = be(level, pos);
        return c != null ? c.source().getDestroyProgress(player, level, pos) : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        CarvedBlockEntity c = be(level, pos);
        return c != null ? c.source().getSoundType() : super.getSoundType(state, level, pos, entity);
    }

    /** Breaking a carved block gives back the original block. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity b = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (b instanceof CarvedBlockEntity c && c.source().getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
            return List.of(new ItemStack(c.source().getBlock()));
        }
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        CarvedBlockEntity c = be(level, pos);
        return c != null ? new ItemStack(c.source().getBlock()) : ItemStack.EMPTY;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
