package com.dropzone.mixin;

import com.dropzone.trash.TrashHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTrashMixin implements TrashHolder {
    @Unique
    private SimpleContainer dropzone$trash;

    @Override
    public SimpleContainer dropzone$getTrash() {
        if (dropzone$trash == null) dropzone$trash = new SimpleContainer(1);
        return dropzone$trash;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void dropzone$saveTrash(CompoundTag tag, CallbackInfo ci) {
        ItemStack s = dropzone$getTrash().getItem(0);
        if (!s.isEmpty()) tag.put("DropZoneTrash", s.save(new CompoundTag()));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void dropzone$loadTrash(CompoundTag tag, CallbackInfo ci) {
        dropzone$getTrash().setItem(0, tag.contains("DropZoneTrash") ? ItemStack.of(tag.getCompound("DropZoneTrash")) : ItemStack.EMPTY);
    }
}
