package com.dropzone.saddle;

import com.dropzone.network.DZNetwork;
import com.dropzone.network.S2CSaddleState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Cannon Saddle lives on the mob as persistent NBT on the server, and is mirrored to clients by entity id.
 * Common code: no client classes here.
 */
public final class CannonSaddle {
    public static final String TAG = "dropzone_cannon_saddle";
    public static final String TAG_DESTROY = "dropzone_cannon_destroy";

    /** Client mirror, filled by S2CSaddleState. */
    public static final Set<Integer> CLIENT_SADDLED = ConcurrentHashMap.newKeySet();
    public static final Set<Integer> CLIENT_DESTROY = ConcurrentHashMap.newKeySet();

    public static Entity root(Entity e) {
        return e instanceof PartEntity<?> part ? part.getParent() : e;
    }

    public static boolean has(Entity e) {
        if (e.level().isClientSide) return CLIENT_SADDLED.contains(e.getId());
        return e.getPersistentData().getBoolean(TAG);
    }

    public static boolean destructive(Entity e) {
        if (e.level().isClientSide) return CLIENT_DESTROY.contains(e.getId());
        return e.getPersistentData().getBoolean(TAG_DESTROY);
    }

    /** Server only. */
    public static void set(Entity e, boolean saddled, boolean destroy) {
        if (saddled) {
            e.getPersistentData().putBoolean(TAG, true);
            e.getPersistentData().putBoolean(TAG_DESTROY, destroy);
        } else {
            e.getPersistentData().remove(TAG);
            e.getPersistentData().remove(TAG_DESTROY);
        }
        DZNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> e), new S2CSaddleState(e.getId(), saddled, destroy));
    }

    /** Mobs that should be flown with the camera instead of walked. */
    public static boolean isFlyer(Entity e) {
        return e instanceof FlyingAnimal || e instanceof Ghast || e instanceof Phantom
                || (e instanceof Mob && e.isNoGravity());
    }

    /** Vanilla's dragon has its own movement code and is steered server-side instead (see SaddleEvents). */
    public static boolean usesGenericControl(Entity e) {
        return !(e instanceof EnderDragon);
    }

    public static void clearClient() {
        CLIENT_SADDLED.clear();
        CLIENT_DESTROY.clear();
    }

    private CannonSaddle() {}
}
