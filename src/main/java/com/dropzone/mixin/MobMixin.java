package com.dropzone.mixin;

import com.dropzone.saddle.CannonSaddle;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A mob wearing a Cannon Saddle is steered by the player riding it, unless the mob already has its own
 * rider controls (tamed mounts, modded dragons), which are left untouched.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "getControllingPassenger", at = @At("RETURN"), cancellable = true)
    private void dropzone$saddleRider(CallbackInfoReturnable<LivingEntity> cir) {
        if (cir.getReturnValue() != null) return;
        Mob self = (Mob) (Object) this;
        if (CannonSaddle.usesGenericControl(self) && CannonSaddle.has(self) && self.getFirstPassenger() instanceof Player player) {
            cir.setReturnValue(player);
        }
    }

    /** While a player rides a saddled mob, its own AI stands down (no wandering off, no attacking the rider). */
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void dropzone$pauseAi(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (CannonSaddle.usesGenericControl(self) && CannonSaddle.has(self) && self.getFirstPassenger() instanceof Player) {
            ci.cancel();
        }
    }
}
