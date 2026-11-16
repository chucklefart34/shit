package com.dropzone.network;

import com.dropzone.item.GliderItem;
import com.dropzone.registry.ModSounds;
import com.dropzone.server.ServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SGlider(boolean deploy) {
    public static void encode(C2SGlider msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.deploy);
    }

    public static C2SGlider decode(FriendlyByteBuf buf) {
        return new C2SGlider(buf.readBoolean());
    }

    public static void handle(C2SGlider msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (!msg.deploy) {
            ServerState.setGliding(player, false, 0);
            return;
        }
        ItemStack glider = GliderItem.find(player);
        boolean allowed = !glider.isEmpty() && !player.onGround() && !player.isFallFlying() && !player.isInWater()
                && !player.isInLava() && !player.getAbilities().flying && !player.isPassenger() && !player.isSpectator();
        if (!allowed) {
            ServerState.setGliding(player, false, 0);
            return;
        }
        ServerState.setEmote(player, -1);
        ServerState.setGliding(player, true, Item.getId(glider.getItem()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GLIDER_DEPLOY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
