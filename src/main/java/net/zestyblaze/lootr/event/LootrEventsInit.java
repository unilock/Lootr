package net.zestyblaze.lootr.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.zestyblaze.lootr.block.entities.TileTicker;
import net.zestyblaze.lootr.entity.EntityTicker;
import net.zestyblaze.lootr.network.NetworkConstants;

public class LootrEventsInit {
  public static MinecraftServer serverInstance;

  public static void registerEvents() {
    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      serverInstance = server;
      HandleChunk.onServerStarted();
    });

    ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
      serverInstance = null;
    });

    ServerTickEvents.END_SERVER_TICK.register(server -> {
      EntityTicker.serverTick();
      TileTicker.serverTick();
    });

    ServerChunkEvents.CHUNK_LOAD.register(HandleChunk::onChunkLoad);

    PlayerBlockBreakEvents.BEFORE.register(HandleBreak::beforeBlockBreak);

    PlayerBlockBreakEvents.CANCELED.register(HandleBreak::afterBlockBreak);

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      NetworkConstants.sendSyncDisableBreak(handler.player);
    });
  }
}
