package noobanidus.mods.lootr.common.mixins;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;
import noobanidus.mods.lootr.common.block.entity.BlockEntityTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public class MixinLevelChunk {
  @Inject(method = "updateBlockEntityTicker", at = @At(value = "HEAD"))
  private void LootrUpdateBlockEntityTicker(BlockEntity entity, CallbackInfo cir) {
    if (LootrAPI.isDisabled()) {
      return;
    }

    if (entity instanceof RandomizableContainerBlockEntity && !(LootrAPI.resolveBlockEntity(entity) instanceof ILootrBlockEntity)) {
      LevelChunk level = (LevelChunk) (Object) this;
      if (level.getLevel().isClientSide()) {
        return;
      }
      // By default block entities outside of the world border are
      // not converted. When the world border changes, you will
      // need to restart the server.
      if (LootrAPI.isWorldBorderSafe(level.getLevel(), entity.getBlockPos())) {
        BlockEntityTicker.addEntry(level.getLevel(), entity.getBlockPos());
      }
    }
  }
}