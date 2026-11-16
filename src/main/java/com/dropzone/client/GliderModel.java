package com.dropzone.client;

import com.dropzone.DropZone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Canopy with two drooping wings, two struts and two cords down to the hands.
 * Model space follows entity models: 16 units per block, +Y is down.
 */
public class GliderModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(DropZone.MODID, "glider"), "main");

    private final ModelPart root;

    public GliderModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12.0F, -1.0F, -10.0F, 24, 2, 20), PartPose.ZERO);
        root.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-14.0F, -1.0F, -10.0F, 14, 2, 20), PartPose.offsetAndRotation(-12.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.38F));
        root.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(0, 22).mirror()
                .addBox(0.0F, -1.0F, -10.0F, 14, 2, 20), PartPose.offsetAndRotation(12.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.38F));

        // Struts: from each hand straight up to the canopy edge.
        root.addOrReplaceChild("strut_left", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-0.5F, -14.5F, -0.5F, 1, 15, 1), PartPose.offsetAndRotation(-7.0F, 15.0F, 0.0F, 0.0F, 0.0F, -0.21F));
        root.addOrReplaceChild("strut_right", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-0.5F, -14.5F, -0.5F, 1, 15, 1), PartPose.offsetAndRotation(7.0F, 15.0F, 0.0F, 0.0F, 0.0F, 0.21F));
        // Cords: from each hand out to the wing tips.
        root.addOrReplaceChild("cord_left", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-0.25F, -20.0F, -0.25F, 0.5F, 20.0F, 0.5F), PartPose.offsetAndRotation(-7.0F, 15.0F, 0.0F, 0.0F, 0.0F, -1.0F));
        root.addOrReplaceChild("cord_right", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-0.25F, -20.0F, -0.25F, 0.5F, 20.0F, 0.5F), PartPose.offsetAndRotation(7.0F, 15.0F, 0.0F, 0.0F, 0.0F, 1.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
