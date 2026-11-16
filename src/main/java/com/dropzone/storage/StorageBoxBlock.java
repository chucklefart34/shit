package com.dropzone.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Like a shulker box: keeps its contents when broken. Right-click with the next tier's upgrade to upgrade in place. */
public class StorageBoxBlock extends BaseEntityBlock {
    private final BoxTier tier;
    private final Supplier<? extends Block> nextTier;

    public StorageBoxBlock(BoxTier tier, @Nullable Supplier<? extends Block> nextTier, Properties props) {
        super(props);
        this.tier = tier;
        this.nextTier = nextTier;
    }

    public BoxTier tier() {
        return tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBoxBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof BoxUpgradeItem upgrade) {
            return tryUpgrade(upgrade, held, level, pos, player);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof StorageBoxBlockEntity box) {
            NetworkHooks.openScreen(sp, box, buf -> {
                buf.writeBlockPos(pos);
                buf.writeByte(tier.ordinal());
            });
            level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6F, 1.1F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult tryUpgrade(BoxUpgradeItem upgrade, ItemStack held, Level level, BlockPos pos, Player player) {
        if (upgrade.from() != tier || nextTier == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.dropzone.box_upgrade_wrong",
                        Component.translatable("tier.dropzone.box." + upgrade.from().id)).withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StorageBoxBlockEntity old) {
            CompoundTag data = old.saveWithoutMetadata();
            level.setBlock(pos, nextTier.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof StorageBoxBlockEntity fresh) {
                fresh.load(data);
                fresh.setChanged();
            }
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Creative players breaking a full box still get it back, like shulker boxes. Survival drops come from the loot table. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof StorageBoxBlockEntity box && !box.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            box.saveToItem(stack);
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StorageBoxBlockEntity box ? box.comparatorSignal() : 0;
    }
}
