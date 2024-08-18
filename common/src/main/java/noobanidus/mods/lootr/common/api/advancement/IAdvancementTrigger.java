package noobanidus.mods.lootr.common.api.advancement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface IAdvancementTrigger extends ITrigger {
  void trigger(ServerPlayer player, ResourceLocation advancementId);
}