package com.dropzone.network;

import com.dropzone.storage.StorageBoxMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SBoxClick(int containerId, int index, int action) {
    public static void encode(C2SBoxClick msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeVarInt(msg.index + 1);
        buf.writeByte(msg.action);
    }

    public static C2SBoxClick decode(FriendlyByteBuf buf) {
        return new C2SBoxClick(buf.readVarInt(), buf.readVarInt() - 1, buf.readByte());
    }

    public static void handle(C2SBoxClick msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.containerMenu instanceof StorageBoxMenu menu && menu.containerId == msg.containerId
                && menu.stillValid(player)) {
            menu.handleBoxClick(player, msg.index, msg.action);
        }
    }
}
