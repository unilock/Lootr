package noobanidus.mods.lootr.block.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import noobanidus.mods.lootr.init.ModBlockEntities;

public class LootrTrappedChestBlockEntity extends LootrChestBlockEntity {
  public LootrTrappedChestBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
    super(ModBlockEntities.LOOTR_TRAPPED_CHEST.get(), pWorldPosition, pBlockState);
  }

  @Override
  protected void signalOpenCount(Level level, BlockPos pos, BlockState state, int p_155868_, int p_155869_) {
    super.signalOpenCount(level, pos, state, p_155868_, p_155869_);
    if (p_155868_ != p_155869_) {
      Block block = state.getBlock();
      level.updateNeighborsAt(pos, block);
      level.updateNeighborsAt(pos.below(), block);
    }
  }
}
