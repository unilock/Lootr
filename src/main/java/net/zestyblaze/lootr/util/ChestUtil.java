package net.zestyblaze.lootr.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.zestyblaze.lootr.advancement.GenericTrigger;
import net.zestyblaze.lootr.api.IHasOpeners;
import net.zestyblaze.lootr.api.blockentity.ILootBlockEntity;
import net.zestyblaze.lootr.block.LootrShulkerBlock;
import net.zestyblaze.lootr.block.entities.LootrInventoryBlockEntity;
import net.zestyblaze.lootr.config.ConfigManager;
import net.zestyblaze.lootr.data.DataStorage;
import net.zestyblaze.lootr.entity.LootrChestMinecartEntity;
import net.zestyblaze.lootr.init.ModAdvancements;
import net.zestyblaze.lootr.init.ModStats;
import net.zestyblaze.lootr.network.NetworkConstants;

import java.util.UUID;


@SuppressWarnings("unused")
public class ChestUtil {
  // TODO: The code for handling this should probably go into the API.
  public static void handleLootSneak(Block block, Level level, BlockPos pos, Player player) {
    if (level.isClientSide() || player.isSpectator()) {
      return;
    }

    BlockEntity te = level.getBlockEntity(pos);
    if (te instanceof ILootBlockEntity tile) {
      if (tile.getOpeners().remove(player.getUUID())) {
        te.setChanged();
        tile.updatePacketViaState();
      }
    }
  }

  // TODO: Move to API?
  public static void handleLootCartSneak(Level level, LootrChestMinecartEntity cart, Player player) {
    if (level.isClientSide() || player.isSpectator()) {
      return;
    }

    cart.getOpeners().remove(player.getUUID());
    NetworkConstants.sendCloseCart(cart.getId(), (ServerPlayer) player);
  }

  public static Style getInvalidStyle() {
    return ConfigManager.get().notifications.disable_message_styles ? Style.EMPTY : Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED)).withBold(true);
  }

  public static Style getDecayStyle() {
    return ConfigManager.get().notifications.disable_message_styles ? Style.EMPTY : Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED)).withBold(true);
  }

  public static Style getRefreshStyle() {
    return ConfigManager.get().notifications.disable_message_styles ? Style.EMPTY : Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.BLUE)).withBold(true);
  }

  public static void handleLootChest(Block block, Level level, BlockPos pos, Player player) {
    if (level.isClientSide() || player.isSpectator()) {
      if (player.isSpectator()) {
        player.openMenu(null);
      }
      return;
    }
    BlockEntity te = level.getBlockEntity(pos);
    if (te instanceof ILootBlockEntity tile) {
      UUID tileId = tile.getTileId();
      if (tileId == null) {
        player.displayClientMessage(Component.translatable("lootr.message.invalid_block").setStyle(getInvalidStyle()), true);
        return;
      }
      if (DataStorage.isDecayed(tileId)) {
        level.destroyBlock(pos, true);
        notifyDecay(player, tileId);
        return;
      } else {
        int decayValue = DataStorage.getDecayValue(tileId);
        if (decayValue > 0 && ConfigManager.shouldNotify(decayValue)) {
          player.displayClientMessage(Component.translatable("lootr.message.decay_in", decayValue / 20).setStyle(getDecayStyle()), true);
        } else if (decayValue == -1) {
          if (ConfigManager.isDecaying((ServerLevel) level, tile)) {
            startDecay(player, tileId, decayValue);
          }
        }
      }
      GenericTrigger<UUID> trigger = ModAdvancements.CHEST_PREDICATE;
      if (block instanceof BarrelBlock) {
        trigger = ModAdvancements.BARREL_PREDICATE;
      } else if (block instanceof LootrShulkerBlock) {
        trigger = ModAdvancements.SHULKER_PREDICATE;
      }
      trigger.trigger((ServerPlayer) player, tileId);
      // Generalize refresh check
      if (DataStorage.isRefreshed(tileId)) {
        DataStorage.refreshInventory(level, pos, tileId, (ServerPlayer) player);
        notifyRefresh(player, tileId);
      }
      int refreshValue = DataStorage.getRefreshValue(tileId);
      if (refreshValue > 0 && ConfigManager.shouldNotify(refreshValue)) {
        player.displayClientMessage(Component.translatable("lootr.message.refresh_in", refreshValue / 20).setStyle(getRefreshStyle()), true);
      } else if (refreshValue == -1) {
        if (ConfigManager.isRefreshing((ServerLevel) level, tile)) {
          startRefresh(player, tileId, refreshValue);
        }
      }
      // Check if it already refreshed
      MenuProvider provider = DataStorage.getInventory(level, tileId, pos, (ServerPlayer) player, (RandomizableContainerBlockEntity) te, tile::unpackLootTable);
      if (provider == null) {
        // Error messages are already handled by nested methods in `getInventory`
        return;
      }
      checkScore((ServerPlayer) player, tileId);
      if (addOpener(tile, player)) {
        te.setChanged();
        tile.updatePacketViaState();
      }
      player.openMenu(provider);
      // BlockTag is only checked when the block is broken.
      PiglinAi.angerNearbyPiglins(player, true);
    }
  }

  private static boolean addOpener(IHasOpeners openable, Player player) {
    return openable.getOpeners().add(player.getUUID());
  }

  public static void handleLootCart(Level level, LootrChestMinecartEntity cart, Player player) {
    if (level.isClientSide() || player.isSpectator()) {
      if (player.isSpectator()) {
        player.openMenu(null);
      }
      return;
    }

    ModAdvancements.CART_PREDICATE.trigger((ServerPlayer) player, cart.getUUID());
    UUID tileId = cart.getUUID();
    if (DataStorage.isDecayed(tileId)) {
      cart.destroy(cart.damageSources().fellOutOfWorld());
      notifyDecay(player, tileId);
      return;
    } else {
      int decayValue = DataStorage.getDecayValue(tileId);
      if (decayValue > 0 && ConfigManager.shouldNotify(decayValue)) {
        player.displayClientMessage(Component.translatable("lootr.message.decay_in", decayValue / 20).setStyle(getDecayStyle()), true);
      } else if (decayValue == -1) {
        if (ConfigManager.isDecaying((ServerLevel) level, cart)) {
          startDecay(player, tileId, decayValue);
        }
      }
    }
    addOpener(cart, player);
    checkScore((ServerPlayer) player, cart.getUUID());
    if (DataStorage.isRefreshed(tileId)) {
      DataStorage.refreshInventory(level, cart, (ServerPlayer) player);
      notifyRefresh(player, tileId);
    }
    int refreshValue = DataStorage.getRefreshValue(tileId);
    if (refreshValue > 0 && ConfigManager.shouldNotify(refreshValue)) {
      player.displayClientMessage(Component.translatable("lootr.message.refresh_in", refreshValue / 20).setStyle(getRefreshStyle()), true);
    } else if (refreshValue == -1) {
      if (ConfigManager.isRefreshing((ServerLevel) level, cart)) {
        startRefresh(player, tileId, refreshValue);
      }
    }
    MenuProvider provider = DataStorage.getInventory(level, cart, (ServerPlayer) player, cart::addLoot);
    if (provider == null) {
      // Error messages are already handled by nested methods in `getInventory`
      return;
    }
    player.openMenu(provider);
  }

  public static void handleLootInventory(Block block, Level level, BlockPos pos, Player player) {
    if (level.isClientSide() || player.isSpectator()) {
      if (player.isSpectator()) {
        player.openMenu(null);
      }
      return;
    }
    BlockEntity te = level.getBlockEntity(pos);
    if (te instanceof LootrInventoryBlockEntity tile) {
      ModAdvancements.CHEST_PREDICATE.trigger((ServerPlayer) player, tile.getTileId());
      NonNullList<ItemStack> stacks = null;
      if (tile.getCustomInventory() != null) {
        stacks = copyItemList(tile.getCustomInventory());
      }
      UUID tileId = tile.getTileId();
      if (DataStorage.isRefreshed(tileId)) {
        DataStorage.refreshInventory(level, pos, tile.getTileId(), stacks, (ServerPlayer) player);
        notifyRefresh(player, tileId);
      }
      int refreshValue = DataStorage.getRefreshValue(tileId);
      if (refreshValue > 0 && ConfigManager.shouldNotify(refreshValue)) {
        player.displayClientMessage(Component.translatable("lootr.message.refresh_in", refreshValue / 20).setStyle(getRefreshStyle()), true);
      } else if (refreshValue == -1) {
        if (ConfigManager.isRefreshing((ServerLevel) level, tile)) {
          startRefresh(player, tileId, refreshValue);
        }
      }
      MenuProvider provider = DataStorage.getInventory(level, tile.getTileId(), stacks, (ServerPlayer) player, pos, tile);
      if (provider == null) {
        // Error messages are already handled by nested methods in `getInventory`
        return;
      }
      checkScore((ServerPlayer) player, tile.getTileId());
      if (addOpener(tile, player)) {
        te.setChanged();
        tile.updatePacketViaState();
      }
      player.openMenu(provider);
      PiglinAi.angerNearbyPiglins(player, true);
    }
  }

  public static NonNullList<ItemStack> copyItemList(NonNullList<ItemStack> reference) {
    NonNullList<ItemStack> contents = NonNullList.withSize(reference.size(), ItemStack.EMPTY);
    for (int i = 0; i < reference.size(); i++) {
      contents.set(i, reference.get(i).copy());
    }
    return contents;
  }

  private static void checkScore(ServerPlayer player, UUID tileId) {
    if (!DataStorage.isScored(player.getUUID(), tileId)) {
      player.awardStat(ModStats.LOOTED_STAT);
      ModAdvancements.SCORE_PREDICATE.trigger(player, null);
      DataStorage.score(player.getUUID(), tileId);
    }
  }

  private static void notifyDecay(Player player, UUID tileId) {
    player.displayClientMessage(Component.translatable("lootr.message.decayed").setStyle(getDecayStyle()), true);
    DataStorage.removeDecayed(tileId);
  }

  private static void notifyRefresh(Player player, UUID tileId) {
    DataStorage.removeRefreshed(tileId);
    player.displayClientMessage(Component.translatable("lootr.message.refreshed").setStyle(getRefreshStyle()), true);
  }

  private static void startDecay(Player player, UUID tileId, int decayValue) {
    DataStorage.setDecaying(tileId, ConfigManager.get().decay.decay_value);
    player.displayClientMessage(Component.translatable("lootr.message.decay_start", ConfigManager.get().decay.decay_value / 20).setStyle(getDecayStyle()), true);
  }

  private static void startRefresh(Player player, UUID tileId, int refreshValue) {
    DataStorage.setRefreshing(tileId, ConfigManager.get().refresh.refresh_value);
    player.displayClientMessage(Component.translatable("lootr.message.refresh_start", ConfigManager.get().refresh.refresh_value / 20).setStyle(getRefreshStyle()), true);
  }
}