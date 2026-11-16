package com.dropzone.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Overrides the vanilla player pose after setupAnim for emotes and gliding.
 * Angles are radians. Hanging arm: +zRot swings the right arm outward, -zRot the left.
 * Raised arm (xRot near -PI): the sign flips, so -zRot spreads the right arm outward.
 */
public final class PlayerPoses {
    private static final float PI = (float) Math.PI;

    public static void apply(PlayerModel<?> m, LivingEntity entity, float ageInTicks) {
        if (!(entity instanceof Player player)) return;

        ClientState.EmoteInstance emote = ClientState.getEmote(player.getId());
        if (emote != null) {
            float t = Math.max(0.0F, ageInTicks - emote.startTick());
            poseEmote(m, emote, t);
        } else if (ClientState.isGliding(player.getId())) {
            poseGlide(m, ageInTicks);
        } else {
            return;
        }
        syncOuterLayers(m);
    }

    private static void poseGlide(PlayerModel<?> m, float age) {
        float sway = Mth.sin(age * 0.12F) * 0.04F;
        set(m.rightArm, -PI + 0.15F, 0.0F, -0.22F + sway);
        set(m.leftArm, -PI + 0.15F, 0.0F, 0.22F + sway);
        set(m.rightLeg, 0.25F + Mth.sin(age * 0.1F) * 0.08F, 0.0F, 0.05F);
        set(m.leftLeg, 0.15F - Mth.sin(age * 0.1F) * 0.08F, 0.0F, -0.05F);
    }

    private static void poseEmote(PlayerModel<?> m, ClientState.EmoteInstance emote, float t) {
        // Start from a neutral stance so walking/attack/crossbow poses don't leak through.
        set(m.rightArm, 0, 0, 0.05F);
        set(m.leftArm, 0, 0, -0.05F);
        set(m.rightLeg, 0, 0, 0);
        set(m.leftLeg, 0, 0, 0);
        m.body.xRot = 0;
        m.body.yRot = 0;
        m.body.zRot = 0;

        switch (emote.emote()) {
            case WAVE -> {
                set(m.rightArm, -2.75F, 0, -0.35F + Mth.sin(t * 0.55F) * 0.4F);
                m.head.zRot = Mth.sin(t * 0.275F) * 0.06F;
            }
            case FLEX -> {
                float pulse = Mth.sin(t * 0.35F) * 0.15F;
                set(m.rightArm, 0, 0, 2.3F + pulse);
                set(m.leftArm, 0, 0, -2.3F - pulse);
                m.head.xRot = -0.15F;
            }
            case CHEER -> {
                set(m.rightArm, -2.9F + Mth.sin(t * 0.6F) * 0.25F, 0, -0.3F);
                set(m.leftArm, -2.9F + Mth.sin(t * 0.6F + PI) * 0.25F, 0, 0.3F);
                m.head.xRot = -0.35F;
            }
            case SALUTE -> {
                set(m.rightArm, -2.7F, 0, 0.25F);
                m.head.xRot = 0;
                m.head.yRot = 0;
            }
            case FACEPALM -> {
                set(m.rightArm, -2.54F, 0, 0.58F);
                m.head.xRot = 0.35F;
                m.head.yRot = 0;
                m.head.zRot = Mth.sin(t * 0.15F) * 0.08F;
            }
            case ROBOT -> {
                int phase = Mth.floor(t / 5.0F) % 4;
                boolean a = phase % 2 == 0;
                set(m.rightArm, a ? -PI / 2 : -0.2F, 0, 0);
                set(m.leftArm, a ? -0.2F : -PI / 2, 0, 0);
                m.head.xRot = 0;
                m.head.yRot = phase < 2 ? 0.5F : -0.5F;
                m.rightLeg.xRot = a ? -0.15F : 0;
                m.leftLeg.xRot = a ? 0 : -0.15F;
            }
            case DISCO -> {
                boolean up = Mth.floor(t / 10.0F) % 2 == 0;
                if (up) set(m.rightArm, -2.8F, 0, -0.5F);
                else set(m.rightArm, -0.4F, 0, -0.6F);
                set(m.leftArm, 0, 0, -0.5F);
                float step = Mth.sin(t * PI / 10.0F) * 0.25F;
                m.rightLeg.xRot = step;
                m.leftLeg.xRot = -step;
                m.head.xRot = up ? -0.25F : 0.2F;
                m.head.yRot = up ? -0.3F : 0.3F;
            }
            case SWING -> {
                float s = Mth.sin(t * 0.4F);
                set(m.rightArm, -0.5F, 0, s * 0.8F);
                set(m.leftArm, -0.5F, 0, s * 0.8F);
                m.rightLeg.zRot = -s * 0.12F;
                m.leftLeg.zRot = -s * 0.12F;
                m.body.yRot = s * 0.2F;
                m.head.yRot = -s * 0.3F;
                m.head.xRot = 0;
            }
        }
    }

    private static void set(ModelPart part, float x, float y, float z) {
        part.xRot = x;
        part.yRot = y;
        part.zRot = z;
    }

    private static void syncOuterLayers(PlayerModel<?> m) {
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
    }

    private PlayerPoses() {}
}
