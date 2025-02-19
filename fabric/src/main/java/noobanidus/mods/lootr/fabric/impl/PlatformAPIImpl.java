package noobanidus.mods.lootr.fabric.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.LockCode;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import noobanidus.mods.lootr.common.api.DataToCopy;
import noobanidus.mods.lootr.common.api.IPlatformAPI;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;
import noobanidus.mods.lootr.common.api.data.entity.ILootrCart;
import noobanidus.mods.lootr.common.mixins.MixinBaseContainerBlockEntity;
import noobanidus.mods.lootr.fabric.network.to_client.*;

public class PlatformAPIImpl implements IPlatformAPI {
  @Override
  public void performCartOpen(ILootrCart cart, ServerPlayer player) {
    ServerPlayNetworking.send(player, new PacketOpenCart(cart.asEntity().getId()));
  }

  @Override
  public void performCartOpen(ILootrCart cart) {
    if (cart.getInfoLevel() instanceof ServerLevel serverLevel) {
      Packet<?> packet = ServerPlayNetworking.createS2CPacket(new PacketOpenCart(cart.asEntity().getId()));
      serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(cart.asEntity().blockPosition()), false).forEach(player -> player.connection.send(packet));
    }
  }

  @Override
  public void performCartClose(ILootrCart cart, ServerPlayer player) {
    ServerPlayNetworking.send(player, new PacketCloseCart(cart.asEntity().getId()));
  }

  @Override
  public void performCartClose(ILootrCart cart) {
    if (cart.getInfoLevel() instanceof ServerLevel serverLevel) {
      Packet<?> packet = ServerPlayNetworking.createS2CPacket(new PacketCloseCart(cart.asEntity().getId()));
      serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(cart.asEntity().blockPosition()), false).forEach(player -> player.connection.send(packet));
    }
  }

  @Override
  public void performBlockOpen(ILootrBlockEntity blockEntity, ServerPlayer player) {
    ServerPlayNetworking.send(player, new PacketOpenContainer(blockEntity.asBlockEntity().getBlockPos()));
  }

  @Override
  public void performBlockOpen(ILootrBlockEntity blockEntity) {
    if (blockEntity.getInfoLevel() instanceof ServerLevel serverLevel) {
      Packet<?> packet = ServerPlayNetworking.createS2CPacket(new PacketOpenContainer(blockEntity.asBlockEntity().getBlockPos()));
      serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(blockEntity.asBlockEntity().getBlockPos()), false).forEach(player -> player.connection.send(packet));
    }
  }

  @Override
  public void performBlockClose(ILootrBlockEntity blockEntity, ServerPlayer player) {
    ServerPlayNetworking.send(player, new PacketCloseContainer(blockEntity.asBlockEntity().getBlockPos()));
  }

  @Override
  public void performBlockClose(ILootrBlockEntity blockEntity) {
    if (blockEntity.getInfoLevel() instanceof ServerLevel serverLevel) {
      Packet<?> packet = ServerPlayNetworking.createS2CPacket(new PacketCloseContainer(blockEntity.asBlockEntity().getBlockPos()));
      serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(blockEntity.asBlockEntity().getBlockPos()), false).forEach(player -> player.connection.send(packet));
    }
  }

  @Override
  public DataToCopy copySpecificData(BlockEntity oldBlockEntity) {
    LockCode key = LockCode.NO_LOCK;
    if (oldBlockEntity instanceof BaseContainerBlockEntity baseContainer) {
      key = ((MixinBaseContainerBlockEntity) baseContainer).getLockKey();
    }
    return new DataToCopy(null, key);
  }

  @Override
  public void restoreSpecificData(DataToCopy data, BlockEntity newBlockEntity) {
    if (newBlockEntity instanceof BaseContainerBlockEntity baseContainer) {
      ((MixinBaseContainerBlockEntity) baseContainer).setLockKey(data.lockCode());
    }
  }

  @Override
  public void refreshPlayerSection(ServerPlayer player) {
    ServerPlayNetworking.send(player, PacketRefreshSection.INSTANCE);
  }
}
