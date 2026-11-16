package com.dropzone.client;

import com.dropzone.storage.BoxEntry;
import com.dropzone.storage.StorageBoxMenu;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ClientBoxHandler {
    public static void contents(int containerId, List<BoxEntry> entries) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof StorageBoxMenu menu && menu.containerId == containerId) {
            menu.setClientEntries(entries);
        }
    }

    private ClientBoxHandler() {}
}
