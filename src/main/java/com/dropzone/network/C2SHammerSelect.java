package com.dropzone.network;

import com.dropzone.hammer.HammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SHammerSelect(BlockPos pos) {
    public static void encode(C2SHammerSelect msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static C2SHammerSelect decode(FriendlyByteBuf buf) {
        return new C2SHammerSelect(buf.readBlockPos());
    }

    public static void handle(C2SHammerSelect msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof HammerItem) || HammerItem.mode(stack) != HammerItem.Mode.TWEAK) return;
        if (player.distanceToSqr(msg.pos.getX() + 0.5D, msg.pos.getY() + 0.5D, msg.pos.getZ() + 0.5D) > 64.0D) return;
        if (!player.level().isLoaded(msg.pos)) return;
        HammerItem.selectNextProperty(player, player.level().getBlockState(msg.pos), stack);
    }
}
