package com.dropzone.hammer;

import com.dropzone.DropZone;
import com.dropzone.network.C2SHammerSelect;
import com.dropzone.network.DZNetwork;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DropZone.MODID)
public final class HammerEvents {
    private static long lastSelect;

    /**
     * In TWEAK mode, left-click selects the next property instead of mining. The client sends its own packet
     * (debounced, since a held click fires every tick) and both sides cancel the normal block attack.
     */
    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof HammerItem) || HammerItem.mode(stack) != HammerItem.Mode.TWEAK) return;
        event.setCanceled(true);
        if (event.getLevel().isClientSide) {
            long now = System.currentTimeMillis();
            if (now - lastSelect > 250L) {
                lastSelect = now;
                DZNetwork.sendToServer(new C2SHammerSelect(event.getPos()));
            }
        }
    }

    private HammerEvents() {}
}
