package net.zestyblaze.lootr.registry;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.Entity;
import net.zestyblaze.lootr.api.LootrAPI;
import net.zestyblaze.lootr.config.LootrModConfig;
import net.zestyblaze.lootr.entity.LootrChestMinecartEntity;
import net.zestyblaze.lootr.network.NetworkConstants;

public class LootrNetworkingInit {
    public static void registerClientNetwork() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.CLOSE_CART_CHANNEL, (client, handler, buf, responseSender) -> client.execute(() -> {
            int entityId = buf.readVarInt();
            if (client.player != null && client.player.level != null) {
                Entity potential = client.player.level.getEntity(entityId);
                if (potential instanceof LootrChestMinecartEntity cart) {
                    cart.setClosed();
                }
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.OPEN_CART_CHANNEL, (client, handler, buf, responseSender) -> client.execute(() -> {
            int entityId = buf.readVarInt();
            if (client.player != null && client.player.level != null) {
                Entity potential = client.player.level.getEntity(entityId);
                if (potential instanceof LootrChestMinecartEntity cart) {
                    cart.setOpened();
                }
            }
        }));

        if(LootrModConfig.get().debug.debugMode) {
            LootrAPI.LOG.info("Lootr: Client Registry - Networking Registered");
        }
    }
}
