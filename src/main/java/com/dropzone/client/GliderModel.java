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
 * Hang glider: arched five-panel sail, keel, A-frame and a control bar at hand height.
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

        // Arched sail: a flat centre panel, inner wings and outer wings that droop more at each joint.
        root.addOrReplaceChild("sail_center", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -0.5F, -11.0F, 10, 1, 22), PartPose.ZERO);
        PartDefinition innerL = root.addOrReplaceChild("sail_inner_left", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-10.0F, -0.5F, -11.0F, 10, 1, 22), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.22F));
        PartDefinition innerR = root.addOrReplaceChild("sail_inner_right", CubeListBuilder.create().texOffs(64, 0).mirror()
                .addBox(0.0F, -0.5F, -11.0F, 10, 1, 22), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.22F));
        innerL.addOrReplaceChild("sail_outer_left", CubeListBuilder.create().texOffs(0, 23)
                .addBox(-9.0F, -0.5F, -10.0F, 9, 1, 20), PartPose.offsetAndRotation(-10.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        innerR.addOrReplaceChild("sail_outer_right", CubeListBuilder.create().texOffs(0, 23).mirror()
                .addBox(0.0F, -0.5F, -10.0F, 9, 1, 20), PartPose.offsetAndRotation(10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.35F));

        // Keel running nose to tail under the sail.
        root.addOrReplaceChild("keel", CubeListBuilder.create().texOffs(64, 23)
                .addBox(-0.5F, 0.5F, -13.0F, 1, 1, 26), PartPose.ZERO);

        // A-frame down-tubes from the keel to the ends of the control bar.
        root.addOrReplaceChild("tube_left", CubeListBuilder.create().texOffs(0, 46)
                .addBox(-0.5F, 0.0F, -0.5F, 1, 16, 1), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.52F));
        root.addOrReplaceChild("tube_right", CubeListBuilder.create().texOffs(0, 46)
                .addBox(-0.5F, 0.0F, -0.5F, 1, 16, 1), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -0.52F));
        // Control bar the rider holds.
        root.addOrReplaceChild("control_bar", CubeListBuilder.create().texOffs(8, 46)
                .addBox(-9.0F, 14.5F, -0.5F, 18, 1, 1), PartPose.ZERO);

        // Rigging from the bar ends up to the wing tips.
        root.addOrReplaceChild("cord_left", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.25F, -15.8F, -0.25F, 0.5F, 15.8F, 0.5F), PartPose.offsetAndRotation(-9.0F, 15.0F, 0.0F, 0.0F, 0.0F, -1.0F));
        root.addOrReplaceChild("cord_right", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.25F, -15.8F, -0.25F, 0.5F, 15.8F, 0.5F), PartPose.offsetAndRotation(9.0F, 15.0F, 0.0F, 0.0F, 0.0F, 1.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
