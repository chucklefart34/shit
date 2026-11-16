package com.dropzone.network;

import com.dropzone.storage.BoxEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Full contents of an open storage box (at most 100 entries). Counts are VarInts, so 10k+ is fine. */
public record S2CBoxContents(int containerId, List<BoxEntry> entries) {
    public static void encode(S2CBoxContents msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeVarInt(msg.entries.size());
        for (BoxEntry e : msg.entries) {
            buf.writeItem(e.proto);
            buf.writeVarInt(e.count);
        }
    }

    public static S2CBoxContents decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int n = buf.readVarInt();
        List<BoxEntry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new BoxEntry(buf.readItem(), buf.readVarInt()));
        }
        return new S2CBoxContents(id, list);
    }

    public static void handle(S2CBoxContents msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.dropzone.client.ClientBoxHandler.contents(msg.containerId, msg.entries));
    }
}
