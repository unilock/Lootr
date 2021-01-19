package noobanidus.mods.lootr.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import noobanidus.mods.lootr.init.ModBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Random;

@Mixin(StructurePiece.class)
public class MixinStructurePiece {
  @ModifyVariable(
      method = "Lnet/minecraft/world/gen/feature/structure/StructurePiece;generateChest(Lnet/minecraft/world/IServerWorld;Lnet/minecraft/util/math/MutableBoundingBox;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/block/BlockState;)Z",
      at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/gen/feature/structure/StructurePiece;correctFacing(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;")
  )
  private BlockState generateChest(BlockState original, IServerWorld worldIn, MutableBoundingBox boundsIn, Random rand, BlockPos posIn, ResourceLocation resourceLocationIn, @Nullable BlockState p_191080_6_) {
    return StructurePiece.correctFacing(worldIn, posIn, ModBlocks.CHEST.getDefaultState());
  }
}