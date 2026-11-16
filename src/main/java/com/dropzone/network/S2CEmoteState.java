package com.dropzone.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CEmoteState(int entityId, int emote) {
    public static void encode(S2CEmoteState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.emote + 1);
    }

    public static S2CEmoteState decode(FriendlyByteBuf buf) {
        return new S2CEmoteState(buf.readVarInt(), buf.readVarInt() - 1);
    }

    public static void handle(S2CEmoteState msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dropzone.client.ClientState.setEmote(msg.entityId, msg.emote));
    }
}
