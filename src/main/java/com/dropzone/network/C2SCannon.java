package com.dropzone.network;

import com.dropzone.DZConfig;
import com.dropzone.entity.CannonFireballEntity;
import com.dropzone.saddle.CannonSaddle;
import com.dropzone.server.ServerState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Rider asks the saddle cannon to fire (action 0) or to switch destroy/damage mode (action 1). */
public record C2SCannon(int action) {
    public static final int FIRE = 0;
    public static final int TOGGLE_MODE = 1;

    public static void encode(C2SCannon msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.action);
    }

    public static C2SCannon decode(FriendlyByteBuf buf) {
        return new C2SCannon(buf.readByte());
    }

    public static void handle(C2SCannon msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        Entity mount = player.getVehicle();
        if (mount == null || !CannonSaddle.has(mount) || mount.getFirstPassenger() != player) return;

        if (msg.action == TOGGLE_MODE) {
            boolean destroy = !CannonSaddle.destructive(mount) && DZConfig.CANNON_ALLOW_DESTRUCTION.get();
            CannonSaddle.set(mount, true, destroy);
            player.displayClientMessage(Component.translatable(destroy ? "message.dropzone.cannon_destroy" : "message.dropzone.cannon_damage")
                    .withStyle(destroy ? ChatFormatting.RED : ChatFormatting.GOLD), true);
            return;
        }

        long now = player.level().getGameTime();
        if (!ServerState.canFireCannon(player, now)) return;
        ServerState.setNextCannon(player, now + DZConfig.CANNON_COOLDOWN.get());

        ServerLevel level = player.serverLevel();
        boolean destroy = CannonSaddle.destructive(mount) && DZConfig.CANNON_ALLOW_DESTRUCTION.get();
        Vec3 look = player.getViewVector(1.0F);
        // Spawn clear of the mount's hitbox, however big it is.
        double clearance = Math.max(mount.getBbWidth(), mount.getBbHeight()) * 0.6D + 1.5D;
        Vec3 origin = player.getEyePosition().add(look.scale(clearance));
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        int count = DZConfig.CANNON_VOLLEY.get();
        for (int i = 0; i < count; i++) {
            double offset = count == 1 ? 0 : (i - (count - 1) / 2.0D) * 0.08D;
            Vec3 dir = look.add(right.scale(offset)).add(0, (player.getRandom().nextDouble() - 0.5D) * 0.03D, 0).normalize();
            CannonFireballEntity ball = new CannonFireballEntity(level, player, dir, destroy);
            ball.setPos(origin.x + right.x * offset * 6, origin.y, origin.z + right.z * offset * 6);
            level.addFreshEntity(ball);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.7F);
    }
}
