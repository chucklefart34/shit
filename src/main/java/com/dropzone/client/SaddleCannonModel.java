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

/** Leather saddle pad with a swivel cannon on the rider's right. Model space: 16 units/block, +Y down, -Z forward. */
public class SaddleCannonModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(DropZone.MODID, "saddle_cannon"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(DropZone.MODID, "textures/entity/saddle_cannon.png");

    private final ModelPart root;
    private final ModelPart barrel;

    public SaddleCannonModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.barrel = root.getChild("mount").getChild("barrel");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("saddle", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -1.0F, -6.0F, 10, 2, 12), PartPose.ZERO);
        root.addOrReplaceChild("strap_left", CubeListBuilder.create().texOffs(0, 14).addBox(-0.5F, 0.0F, -1.0F, 1, 8, 2), PartPose.offset(-5.5F, 1.0F, 0.0F));
        root.addOrReplaceChild("strap_right", CubeListBuilder.create().texOffs(0, 14).addBox(-0.5F, 0.0F, -1.0F, 1, 8, 2), PartPose.offset(5.5F, 1.0F, 0.0F));
        PartDefinition mount = root.addOrReplaceChild("mount", CubeListBuilder.create().texOffs(0, 26)
                .addBox(-2.0F, -3.0F, -2.0F, 4, 3, 4), PartPose.offset(8.0F, -1.0F, 0.0F));
        mount.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-1.5F, -1.5F, -12.0F, 3, 3, 14), PartPose.offset(0.0F, -4.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setPitch(float radians) {
        barrel.xRot = radians;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
