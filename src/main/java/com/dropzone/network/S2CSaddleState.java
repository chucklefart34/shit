package com.dropzone.network;

import com.dropzone.saddle.CannonSaddle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CSaddleState(int entityId, boolean saddled, boolean destroy) {
    public static void encode(S2CSaddleState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.saddled);
        buf.writeBoolean(msg.destroy);
    }

    public static S2CSaddleState decode(FriendlyByteBuf buf) {
        return new S2CSaddleState(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(S2CSaddleState msg, Supplier<NetworkEvent.Context> ctx) {
        // Plain sets in common code, so no client class loading is needed here.
        if (msg.saddled) CannonSaddle.CLIENT_SADDLED.add(msg.entityId);
        else CannonSaddle.CLIENT_SADDLED.remove(msg.entityId);
        if (msg.saddled && msg.destroy) CannonSaddle.CLIENT_DESTROY.add(msg.entityId);
        else CannonSaddle.CLIENT_DESTROY.remove(msg.entityId);
    }
}
