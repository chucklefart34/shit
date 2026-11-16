package com.dropzone.network;

import com.dropzone.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SReload() {
    public static void encode(C2SReload msg, FriendlyByteBuf buf) {}

    public static C2SReload decode(FriendlyByteBuf buf) {
        return new C2SReload();
    }

    public static void handle(C2SReload msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem gun) gun.tryStartReload(player, stack);
    }
}
