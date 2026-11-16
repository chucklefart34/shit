package com.dropzone.network;

import com.dropzone.DropZone;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DZNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DropZone.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(C2SFireGun.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SFireGun::encode).decoder(C2SFireGun::decode).consumerMainThread(C2SFireGun::handle).add();
        CHANNEL.messageBuilder(C2SReload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SReload::encode).decoder(C2SReload::decode).consumerMainThread(C2SReload::handle).add();
        CHANNEL.messageBuilder(C2SGlider.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SGlider::encode).decoder(C2SGlider::decode).consumerMainThread(C2SGlider::handle).add();
        CHANNEL.messageBuilder(C2SEmote.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SEmote::encode).decoder(C2SEmote::decode).consumerMainThread(C2SEmote::handle).add();
        CHANNEL.messageBuilder(S2CGliderState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CGliderState::encode).decoder(S2CGliderState::decode).consumerMainThread(S2CGliderState::handle).add();
        CHANNEL.messageBuilder(S2CEmoteState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CEmoteState::encode).decoder(S2CEmoteState::decode).consumerMainThread(S2CEmoteState::handle).add();
        CHANNEL.messageBuilder(C2SCannon.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SCannon::encode).decoder(C2SCannon::decode).consumerMainThread(C2SCannon::handle).add();
        CHANNEL.messageBuilder(S2CSaddleState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSaddleState::encode).decoder(S2CSaddleState::decode).consumerMainThread(S2CSaddleState::handle).add();
        CHANNEL.messageBuilder(C2SBoxClick.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SBoxClick::encode).decoder(C2SBoxClick::decode).consumerMainThread(C2SBoxClick::handle).add();
        CHANNEL.messageBuilder(S2CBoxContents.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBoxContents::encode).decoder(S2CBoxContents::decode).consumerMainThread(S2CBoxContents::handle).add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private DZNetwork() {}
}
