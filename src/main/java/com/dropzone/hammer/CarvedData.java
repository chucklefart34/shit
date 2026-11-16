package com.dropzone.hammer;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelProperty;

/** What the carved block model needs: the original block and which of the 2x2x2 pieces remain. */
public record CarvedData(BlockState source, int mask) {
    public static final ModelProperty<CarvedData> PROPERTY = new ModelProperty<>();
}
