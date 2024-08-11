package noobanidus.mods.lootr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import noobanidus.mods.lootr.api.LootrAPI;
import noobanidus.mods.lootr.api.registry.LootrRegistry;
import noobanidus.mods.lootr.config.LootrConfigInit;
import noobanidus.mods.lootr.event.LootrEventsInit;
import noobanidus.mods.lootr.impl.LootrAPIImpl;
import noobanidus.mods.lootr.impl.LootrRegistryImpl;
import noobanidus.mods.lootr.init.*;
import noobanidus.mods.lootr.network.to_client.PacketCloseCart;
import noobanidus.mods.lootr.network.to_client.PacketCloseContainer;
import noobanidus.mods.lootr.network.to_client.PacketOpenCart;
import noobanidus.mods.lootr.network.to_client.PacketOpenContainer;

public class Lootr implements ModInitializer {
  @Override
  public void onInitialize() {
    LootrAPI.INSTANCE = new LootrAPIImpl();
    LootrRegistry.INSTANCE = new LootrRegistryImpl();

    PayloadTypeRegistry.playS2C().register(PacketOpenCart.TYPE, PacketOpenCart.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(PacketCloseCart.TYPE, PacketCloseCart.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(PacketOpenContainer.TYPE, PacketOpenContainer.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(PacketCloseContainer.TYPE, PacketCloseContainer.STREAM_CODEC);

    LootrConfigInit.registerConfig();
    ModItems.registerItems();
    ModBlocks.registerBlocks();
    ModTabs.registerTabs();
    ModBlockEntities.registerBlockEntities();
    ModEntities.registerEntities();
    ModLoot.registerLoot();
    LootrEventsInit.registerEvents();
    ModStats.registerStats();
    ModAdvancements.registerAdvancements();
  }
}
