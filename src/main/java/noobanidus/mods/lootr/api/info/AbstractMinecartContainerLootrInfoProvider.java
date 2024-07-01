package noobanidus.mods.lootr.api.info;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record AbstractMinecartContainerLootrInfoProvider(
    AbstractMinecartContainer minecart) implements ILootrInfoProvider {

  @Override
  public LootrInfoType getInfoType() {
    return LootrInfoType.MINECART_ENTITY;
  }

  @Override
  public Vec3 getInfoVec() {
    return minecart.position();
  }

  @Override
  public BlockPos getInfoPos() {
    return minecart.blockPosition();
  }

  @Override
  public ResourceKey<LootTable> getInfoLootTable() {
    return minecart.getLootTable();
  }

  @Override
  public @Nullable Component getInfoDisplayName() {
    return minecart.getName();
  }

  @Override
  public @NotNull ResourceKey<Level> getInfoDimension() {
    return minecart.level().dimension();
  }

  @Override
  public int getInfoContainerSize() {
    return minecart.getContainerSize();
  }

  @Override
  public long getInfoLootSeed() {
    return minecart.getLootTableSeed();
  }

  @Override
  public Level getInfoLevel() {
    return minecart.level();
  }

  @Override
  public Optional<AbstractMinecartContainer> asBaseMinecartEntity() {
    return Optional.of(minecart);
  }
}
