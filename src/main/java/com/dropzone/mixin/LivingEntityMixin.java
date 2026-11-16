package com.dropzone.mixin;

import com.dropzone.saddle.CannonSaddle;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Generic rider steering for saddled mobs that have no riding code of their own. Mounts that override
 * these methods (horses, pigs, striders, camels, modded mounts) never reach these defaults.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    protected abstract void jumpFromGround();

    private boolean dropzone$saddled() {
        LivingEntity self = (LivingEntity) (Object) this;
        return CannonSaddle.usesGenericControl(self) && CannonSaddle.has(self);
    }

    @Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
    private void dropzone$riddenInput(Player player, Vec3 travel, CallbackInfoReturnable<Vec3> cir) {
        if (!dropzone$saddled()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (CannonSaddle.isFlyer(self)) {
            cir.setReturnValue(Vec3.ZERO); // flyers are moved directly in tickRidden
        } else {
            float forward = player.zza <= 0 ? player.zza * 0.4F : player.zza;
            cir.setReturnValue(new Vec3(player.xxa * 0.5F, 0, forward));
        }
    }

    @Inject(method = "tickRidden", at = @At("HEAD"), cancellable = true)
    private void dropzone$tickRidden(Player player, Vec3 travel, CallbackInfo ci) {
        if (!dropzone$saddled()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        boolean jumping = ((LivingEntityAccessor) player).dropzone$isJumping();
        boolean flyer = CannonSaddle.isFlyer(self);

        self.setYRot(player.getYRot());
        self.setXRot(flyer ? player.getXRot() * 0.5F : 0.0F);
        self.yRotO = self.getYRot();
        self.yBodyRot = self.getYRot();
        self.yHeadRot = self.getYRot();

        if (flyer) {
            self.setNoGravity(true);
            Vec3 look = player.getLookAngle();
            Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
            double speed = 0.7D;
            Vec3 want = look.scale(player.zza * speed).add(side.scale(-player.xxa * speed * 0.5D));
            if (jumping) want = want.add(0, 0.4D, 0);
            self.setDeltaMovement(self.getDeltaMovement().lerp(want, 0.25D));
        } else if (jumping && self.onGround()) {
            jumpFromGround();
        }
        self.fallDistance = 0;
        ci.cancel();
    }

    @Inject(method = "getRiddenSpeed", at = @At("HEAD"), cancellable = true)
    private void dropzone$riddenSpeed(Player player, CallbackInfoReturnable<Float> cir) {
        if (!dropzone$saddled()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        AttributeInstance attr = self.getAttribute(Attributes.MOVEMENT_SPEED);
        double base = attr != null ? attr.getValue() : 0.25D;
        cir.setReturnValue((float) Math.max(0.18D, Math.min(0.45D, base)));
    }
}
