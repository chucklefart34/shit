package com.dropzone.network;

import com.dropzone.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** "I pulled the trigger." The server decides everything else. */
public record C2SFireGun() {
    public static void encode(C2SFireGun msg, FriendlyByteBuf buf) {}

    public static C2SFireGun decode(FriendlyByteBuf buf) {
        return new C2SFireGun();
    }

    public static void handle(C2SFireGun msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem gun) gun.tryFire(player, stack);
    }
}
