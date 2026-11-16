package com.dropzone.hammer;

import com.dropzone.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Three modes, switched with Sneak + right-click:
 * TWEAK - survival debug stick (left-click picks a property, right-click cycles it),
 * SHAPE - right-click turns a block into its wall / stairs / slab form,
 * CARVE - right-click chips the clicked eighth off a block.
 */
public class HammerItem extends Item {
    public enum Mode { TWEAK, SHAPE, CARVE }

    private static final String TAG_MODE = "HammerMode";
    private static final String TAG_PROPS = "HammerProps";

    /** Properties that would duplicate items or skip progression in survival. */
    private static final Set<String> BLOCKED = Set.of("age", "honey_level", "level", "charges", "eye", "bites", "pickles",
            "candles", "eggs", "layers", "flower_amount", "hatch", "stage", "has_book", "has_record", "dusted", "berries",
            "has_bottle_0", "has_bottle_1", "has_bottle_2", "occupied", "extended");

    public HammerItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static Mode mode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int i = tag != null ? tag.getInt(TAG_MODE) : 0;
        return Mode.values()[Math.floorMod(i, Mode.values().length)];
    }

    private static void cycleMode(Player player, ItemStack stack) {
        Mode next = Mode.values()[(mode(stack).ordinal() + 1) % Mode.values().length];
        stack.getOrCreateTag().putInt(TAG_MODE, next.ordinal());
        player.displayClientMessage(Component.translatable("message.dropzone.hammer_mode",
                Component.translatable(modeKey(next)).withStyle(ChatFormatting.GOLD)), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    private static String modeKey(Mode m) {
        return "hammer.dropzone.mode." + m.name().toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- right click

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) cycleMode(player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) cycleMode(player, stack);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0 || !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        boolean done = switch (mode(stack)) {
            case TWEAK -> tweak(player, level, pos, state, stack, false);
            case SHAPE -> reshape(player, level, pos, state, ctx);
            case CARVE -> carve(level, pos, state, ctx);
        };
        return done ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    // ---------------------------------------------------------------- TWEAK (debug stick)

    /** Left-click in TWEAK mode (from HammerEvents): choose the next editable property. */
    public static void selectNextProperty(ServerPlayer player, BlockState state, ItemStack stack) {
        List<Property<?>> props = editable(state);
        if (props.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dropzone.hammer_no_props", state.getBlock().getName()), true);
            return;
        }
        String blockId = blockId(state);
        CompoundTag sel = stack.getOrCreateTagElement(TAG_PROPS);
        Property<?> current = state.getBlock().getStateDefinition().getProperty(sel.getString(blockId));
        Property<?> next = Util.findNextInIterable(props, props.contains(current) ? current : null);
        sel.putString(blockId, next.getName());
        player.displayClientMessage(Component.translatable("item.minecraft.debug_stick.select", next.getName(), valueName(state, next)), true);
    }

    private static boolean tweak(Player player, Level level, BlockPos pos, BlockState state, ItemStack stack, boolean backwards) {
        List<Property<?>> props = editable(state);
        if (props.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dropzone.hammer_no_props", state.getBlock().getName()), true);
            return false;
        }
        CompoundTag sel = stack.getOrCreateTagElement(TAG_PROPS);
        Property<?> prop = state.getBlock().getStateDefinition().getProperty(sel.getString(blockId(state)));
        if (prop == null || !props.contains(prop)) prop = props.get(0);
        BlockState next = cycle(state, prop, backwards);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        player.displayClientMessage(Component.translatable("item.minecraft.debug_stick.update", prop.getName(), valueName(next, prop)), true);
        level.playSound(null, pos, SoundEvents.ANVIL_STEP, SoundSource.BLOCKS, 0.4F, 1.6F);
        return true;
    }

    private static List<Property<?>> editable(BlockState state) {
        Collection<Property<?>> all = state.getBlock().getStateDefinition().getProperties();
        // Two-block structures (doors, beds, tall plants) would tear in half, so their halves are off-limits too.
        return all.stream().filter(p -> !BLOCKED.contains(p.getName())
                && p != BlockStateProperties.DOUBLE_BLOCK_HALF && p != BlockStateProperties.BED_PART).toList();
    }

    private static String blockId(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id == null ? "?" : id.toString();
    }

    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> prop, boolean backwards) {
        Collection<T> values = prop.getPossibleValues();
        T current = state.getValue(prop);
        return state.setValue(prop, backwards ? Util.findPreviousInIterable(values, current) : Util.findNextInIterable(values, current));
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> prop) {
        return prop.getName(state.getValue(prop));
    }

    // ---------------------------------------------------------------- SHAPE (full -> wall -> stairs -> slab)

    private static boolean reshape(Player player, Level level, BlockPos pos, BlockState state, UseOnContext ctx) {
        Block block = state.getBlock();
        String path = path(block);
        String ns = namespace(block);
        Block wall, stairs, slab;
        boolean isFull = !(block instanceof SlabBlock) && !(block instanceof StairBlock) && !(block instanceof WallBlock);

        if (isFull) {
            if (!state.isCollisionShapeFullBlock(level, pos)) return fail(player);
            wall = variant(ns, path, "_wall");
            stairs = variant(ns, path, "_stairs");
            slab = variant(ns, path, "_slab");
        } else {
            String base = path.replaceAll("_(wall|stairs|slab)$", "");
            wall = null;
            stairs = block instanceof WallBlock ? find(ns, base + "_stairs") : null;
            slab = block instanceof SlabBlock ? null : find(ns, base + "_slab");
        }

        Direction facing = ctx.getHorizontalDirection();
        boolean top = ctx.getClickedFace() == Direction.DOWN
                || (ctx.getClickedFace() != Direction.UP && ctx.getClickLocation().y - pos.getY() > 0.5D);
        BlockState result = null;
        if (wall != null) {
            result = wall.defaultBlockState();
        } else if (stairs != null) {
            result = stairs.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
        } else if (slab != null) {
            result = slab.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
            if (isFull && !player.getAbilities().instabuild) {
                ItemStack refund = new ItemStack(slab); // a full block is two slabs; keep the other half
                if (!player.getInventory().add(refund)) player.drop(refund, false);
            }
        }
        if (result == null) return fail(player);
        if (result.hasProperty(BlockStateProperties.WATERLOGGED) && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            result = result.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
        }
        result = Block.updateFromNeighbourShapes(result, level, pos);
        level.setBlock(pos, result, Block.UPDATE_ALL);
        level.playSound(null, pos, result.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    private static boolean fail(Player player) {
        player.displayClientMessage(Component.translatable("message.dropzone.hammer_cant_shape").withStyle(ChatFormatting.RED), true);
        return false;
    }

    private static String path(Block b) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
        return id == null ? "" : id.getPath();
    }

    private static String namespace(Block b) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
        return id == null ? "minecraft" : id.getNamespace();
    }

    @Nullable
    private static Block find(String ns, String path) {
        ResourceLocation id = ResourceLocation.tryBuild(ns, path);
        return id != null && ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
    }

    /** oak_planks -> oak_stairs, bricks -> brick_stairs, quartz_block -> quartz_stairs, deepslate_tiles -> deepslate_tile_stairs ... */
    @Nullable
    private static Block variant(String ns, String path, String suffix) {
        String[] bases = {
                path,
                path.replaceAll("_planks$", ""),
                path.replaceAll("_block$", ""),
                path.replaceAll("s$", ""),
                path.replace("bricks", "brick").replace("tiles", "tile"),
        };
        for (String b : bases) {
            Block found = find(ns, b + suffix);
            if (found != null) return found;
        }
        return null;
    }

    // ---------------------------------------------------------------- CARVE (eighths)

    private static boolean carve(Level level, BlockPos pos, BlockState state, UseOnContext ctx) {
        Vec3 hit = ctx.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction face = ctx.getClickedFace();
        // nudge into the block so we pick the eighth that was actually clicked
        Vec3 p = hit.subtract(face.getStepX() * 0.01D, face.getStepY() * 0.01D, face.getStepZ() * 0.01D);
        int octant = (p.x >= 0.5D ? 1 : 0) | (p.y >= 0.5D ? 2 : 0) | (p.z >= 0.5D ? 4 : 0);

        if (level.getBlockEntity(pos) instanceof CarvedBlockEntity carved) {
            int mask = carved.mask() & ~(1 << octant);
            if (mask == carved.mask()) return false;
            playChip(level, pos, carved.source());
            if (mask == 0) level.removeBlock(pos, false);
            else carved.setMask(mask);
            return true;
        }

        if (!canCarve(level, pos, state)) {
            ctx.getPlayer().displayClientMessage(Component.translatable("message.dropzone.hammer_cant_carve").withStyle(ChatFormatting.RED), true);
            return false;
        }
        level.setBlock(pos, ModBlocks.CARVED_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof CarvedBlockEntity carved) {
            carved.init(state, 0xFF & ~(1 << octant));
        }
        playChip(level, pos, state);
        return true;
    }

    private static boolean canCarve(Level level, BlockPos pos, BlockState state) {
        return !state.hasBlockEntity() && state.isCollisionShapeFullBlock(level, pos) && state.getFluidState().isEmpty()
                && state.getBlock().asItem() != net.minecraft.world.item.Items.AIR;
    }

    private static void playChip(Level level, BlockPos pos, BlockState source) {
        level.playSound(null, pos, source.getSoundType().getHitSound(), SoundSource.BLOCKS, 1.0F, 0.7F);
        level.levelEvent(2001, pos, Block.getId(source)); // break particles
    }

    // ---------------------------------------------------------------- misc

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return mode(player.getMainHandItem()) != Mode.TWEAK;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).append(Component.literal(" (")
                .append(Component.translatable(modeKey(mode(stack)))).append(")")).withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dropzone.hammer.desc." + mode(stack).name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.dropzone.hammer.desc.switch").withStyle(ChatFormatting.DARK_GRAY));
    }
}
