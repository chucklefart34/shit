package com.dropzone.mixin;

import com.dropzone.client.PlayerPoses;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies emote and glider poses after vanilla (and other mods) have posed the player. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void dropzone$applyPoses(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                     float netHeadYaw, float headPitch, CallbackInfo ci) {
        // First-person arm rendering calls setupAnim with all zeros; leave that alone.
        if (ageInTicks == 0.0F && limbSwing == 0.0F && netHeadYaw == 0.0F && headPitch == 0.0F) return;
        PlayerPoses.apply((PlayerModel<?>) (Object) this, entity, ageInTicks);
    }
}
