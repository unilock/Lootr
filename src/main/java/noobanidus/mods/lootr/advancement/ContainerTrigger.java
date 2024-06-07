package noobanidus.mods.lootr.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import noobanidus.mods.lootr.data.DataStorage;

import java.util.Optional;
import java.util.UUID;

public class ContainerTrigger extends SimpleCriterionTrigger<ContainerTrigger.TriggerInstance> {
  public void trigger(ServerPlayer player, UUID condition) {
    this.trigger(player, (instance) -> instance.test(player, condition));
  }

  @Override
  public Codec<TriggerInstance> codec() {
    return TriggerInstance.CODEC;
  }

  public record TriggerInstance(
      Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
    public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(codec -> codec.group(ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)).apply(codec, TriggerInstance::new));

    public boolean test(ServerPlayer player, UUID container) {
      if (DataStorage.isAwarded(player.getUUID(), container)) {
        return false;
      } else {
        DataStorage.award(player.getUUID(), container);
        return true;
      }
    }
  }
}