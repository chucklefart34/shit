package com.dropzone.trash;

import net.minecraft.world.SimpleContainer;

/** Implemented on Player by PlayerTrashMixin: the one-slot trash can. */
public interface TrashHolder {
    SimpleContainer dropzone$getTrash();
}
