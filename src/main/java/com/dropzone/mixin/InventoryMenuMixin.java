package com.dropzone.mixin;

import com.dropzone.trash.TrashHolder;
import com.dropzone.trash.TrashSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Appends the trash slot after every vanilla slot, so vanilla slot indices (0-45) don't move. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    protected InventoryMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void dropzone$addTrash(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        this.addSlot(new TrashSlot(((TrashHolder) owner).dropzone$getTrash(), owner));
    }
}
