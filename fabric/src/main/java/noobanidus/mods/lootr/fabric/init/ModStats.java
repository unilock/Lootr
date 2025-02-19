package noobanidus.mods.lootr.fabric.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import noobanidus.mods.lootr.common.api.LootrAPI;

public class ModStats {
  public static ResourceLocation LOOTED_LOCATION = LootrAPI.rl("looted_stat");
  public static Stat<ResourceLocation> LOOTED_STAT;

  public static void registerStats() {
    Registry.register(BuiltInRegistries.CUSTOM_STAT, LOOTED_LOCATION, LOOTED_LOCATION);
  }

  public static void load () {
    LOOTED_STAT = Stats.CUSTOM.get(LOOTED_LOCATION);
  }
}
