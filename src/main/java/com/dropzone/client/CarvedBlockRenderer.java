package com.dropzone.client;

import com.dropzone.hammer.CarvedBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.client.model.data.ModelData;

/** Draws each remaining eighth as a half-size copy of the source block. */
public class CarvedBlockRenderer implements BlockEntityRenderer<CarvedBlockEntity> {
    public CarvedBlockRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CarvedBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        int mask = be.mask();
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) == 0) continue;
            ps.pushPose();
            ps.translate((i & 1) * 0.5D, ((i >> 1) & 1) * 0.5D, ((i >> 2) & 1) * 0.5D);
            ps.scale(0.5F, 0.5F, 0.5F);
            dispatcher.renderSingleBlock(be.source(), ps, buffers, light, overlay, ModelData.EMPTY, null);
            ps.popPose();
        }
    }
}
