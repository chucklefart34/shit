package com.dropzone.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CGliderState(int entityId, boolean gliding, int gliderItemId) {
    public static void encode(S2CGliderState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.gliding);
        buf.writeVarInt(msg.gliderItemId);
    }

    public static S2CGliderState decode(FriendlyByteBuf buf) {
        return new S2CGliderState(buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(S2CGliderState msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dropzone.client.ClientState.setGlider(msg.entityId, msg.gliding, msg.gliderItemId));
    }
}
