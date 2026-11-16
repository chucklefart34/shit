package com.dropzone.network;

import com.dropzone.emote.Emote;
import com.dropzone.server.ServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Start an emote by id, or stop with -1. */
public record C2SEmote(int emote) {
    public static void encode(C2SEmote msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.emote + 1);
    }

    public static C2SEmote decode(FriendlyByteBuf buf) {
        return new C2SEmote(buf.readVarInt() - 1);
    }

    public static void handle(C2SEmote msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (msg.emote < 0) {
            ServerState.setEmote(player, -1);
            return;
        }
        if (Emote.byId(msg.emote) == null) return;
        if (!player.onGround() || player.isSpectator() || player.isPassenger() || player.isUsingItem()
                || ServerState.isGliding(player)) return;
        ServerState.setEmote(player, msg.emote);
    }
}
