package noobanidus.mods.lootr.common.command;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.LootrTags;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;
import noobanidus.mods.lootr.common.api.registry.LootrRegistry;
import noobanidus.mods.lootr.common.block.LootrBarrelBlock;
import noobanidus.mods.lootr.common.block.LootrChestBlock;
import noobanidus.mods.lootr.common.block.LootrShulkerBlock;
import noobanidus.mods.lootr.common.block.entity.LootrInventoryBlockEntity;
import noobanidus.mods.lootr.common.data.DataStorage;
import noobanidus.mods.lootr.common.data.LootrInventory;
import noobanidus.mods.lootr.common.data.LootrSavedData;
import noobanidus.mods.lootr.common.entity.LootrChestMinecartEntity;
import noobanidus.mods.lootr.common.mixins.MixinBaseContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandLootr {
  private static List<ResourceKey<LootTable>> tables = null;
  private static List<String> tableNames = null;
  private CommandDispatcher<CommandSourceStack> dispatcher;

  public CommandLootr(CommandDispatcher<CommandSourceStack> dispatcher) {
    this.dispatcher = dispatcher;
  }

  private static List<ResourceKey<LootTable>> getTables() {
    if (tables == null) {
      tables = new ArrayList<>(BuiltInLootTables.all());
      tableNames = tables.stream().map(o -> o.location().toString()).toList();
    }
    return tables;
  }

  private static List<String> getProfiles() {
    MinecraftServer server = LootrAPI.getServer();
    if (server == null) {
      return Collections.emptyList();
    }

    GameProfileCache cache = server.getProfileCache();
    if (cache == null) {
      return Collections.emptyList();
    }

    // This uses an access widener as the GameProfileInfo class is package-private and thus an accessor mixin is not possible
    return Lists.newArrayList(cache.profilesByName.keySet());
  }

  private static List<String> getTableNames() {
    getTables();
    return tableNames;
  }

  public static void createBlock(CommandSourceStack c, @Nullable Block block, @Nullable ResourceKey<LootTable> incomingTable) {
    Level world = c.getLevel();
    Vec3 incomingPos = c.getPosition();
    BlockPos pos = new BlockPos((int) incomingPos.x, (int) incomingPos.y, (int) incomingPos.z);
    ResourceKey<LootTable> table;
    if (incomingTable == null) {
      table = getTables().get(world.getRandom().nextInt(getTables().size()));
    } else {
      table = incomingTable;
    }
    if (block == null) {
      LootrChestMinecartEntity cart = new LootrChestMinecartEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      Entity e = c.getEntity();
      if (e != null) {
        cart.setYRot(e.getYRot());
      }
      cart.setLootTable(table, world.getRandom().nextLong());
      world.addFreshEntity(cart);
      c.sendSuccess(() -> Component.translatable("lootr.commands.summon", ComponentUtils.wrapInSquareBrackets(Component.translatable("lootr.commands.blockpos", pos.getX(), pos.getY(), pos.getZ()).setStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN)).withBold(true))), table.toString()), false);
    } else {
      BlockState placementState = block.defaultBlockState();
      Entity e = c.getEntity();
      if (e != null) {
        EnumProperty<Direction> prop = null;
        Direction dir = Direction.orderedByNearest(e)[0].getOpposite();
        if (placementState.hasProperty(LootrBarrelBlock.FACING)) {
          prop = LootrBarrelBlock.FACING;
        } else if (placementState.hasProperty(LootrChestBlock.FACING)) {
          prop = LootrChestBlock.FACING;
          dir = e.getDirection().getOpposite();
        } else if (placementState.hasProperty(LootrShulkerBlock.FACING)) {
          prop = LootrShulkerBlock.FACING;
        }
        if (prop != null) {
          placementState = placementState.setValue(prop, dir);
        }
      }
      world.setBlock(pos, placementState, 2);
      if (world.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity randomizableBe) {
        randomizableBe.setLootTable(table, world.getRandom().nextLong());
      }
      c.sendSuccess(() -> Component.translatable("lootr.commands.create", Component.translatable(block.getDescriptionId()), ComponentUtils.wrapInSquareBrackets(Component.translatable("lootr.commands.blockpos", pos.getX(), pos.getY(), pos.getZ()).setStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN)).withBold(true))), table.toString()), false);
    }
  }

  private static NonNullList<ItemStack> copyItemList(NonNullList<ItemStack> reference) {
    NonNullList<ItemStack> contents = NonNullList.withSize(reference.size(), ItemStack.EMPTY);
    for (int i = 0; i < reference.size(); i++) {
      contents.set(i, reference.get(i).copy());
    }
    return contents;
  }

  public CommandLootr register() {
    this.dispatcher.register(builder(Commands.literal("lootr").requires(p -> p.hasPermission(2))));
    this.dispatcher = null;
    return this;
  }

  private RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> suggestTables() {
    return Commands.argument("table", ResourceLocationArgument.id())
        .suggests((c, build) -> SharedSuggestionProvider.suggest(getTableNames(), build));
  }

  private RequiredArgumentBuilder<CommandSourceStack, String> suggestProfiles() {
    return Commands.argument("profile", StringArgumentType.string()).suggests((c, build) -> SharedSuggestionProvider.suggest(getProfiles(), build));
  }

  public LiteralArgumentBuilder<CommandSourceStack> builder(LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.executes(c -> {
      c.getSource().sendSuccess(() -> Component.translatable("lootr.commands.usage"), false);
      return 1;
    });
    builder.then(Commands.literal("barrel").executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getBarrelBlock(), null);
      return 1;
    }).then(suggestTables().executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getBarrelBlock(), ResourceKey.create(Registries.LOOT_TABLE, ResourceLocationArgument.getId(c, "table")));
      return 1;
    })));
    builder.then(Commands.literal("trapped_chest").executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getTrappedChestBlock(), null);
      return 1;
    }).then(suggestTables().executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getTrappedChestBlock(), ResourceKey.create(Registries.LOOT_TABLE, ResourceLocationArgument.getId(c, "table")));
      return 1;
    })));
    builder.then(Commands.literal("chest").executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getChestBlock(), null);
      return 1;
    }).then(suggestTables().executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getChestBlock(), ResourceKey.create(Registries.LOOT_TABLE, ResourceLocationArgument.getId(c, "table")));
      return 1;
    })));
    builder.then(Commands.literal("shulker").executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getShulkerBlock(), null);
      return 1;
    }).then(suggestTables().executes(c -> {
      createBlock(c.getSource(), LootrRegistry.getShulkerBlock(), ResourceKey.create(Registries.LOOT_TABLE, ResourceLocationArgument.getId(c, "table")));
      return 1;
    })));
    builder.then(Commands.literal("clear").executes(c -> {
      c.getSource().sendSuccess(() -> Component.literal("Must provide player name."), true);
      return 1;
    }).then(suggestProfiles().executes(c -> {
      String playerName = StringArgumentType.getString(c, "profile");
      Optional<GameProfile> opt_profile = c.getSource().getServer().getProfileCache().get(playerName);
      if (!opt_profile.isPresent()) {
        c.getSource().sendFailure(Component.literal("Invalid player name: " + playerName + ", profile not found in the cache."));
        return 0;
      }
      GameProfile profile = opt_profile.get();
      c.getSource().sendSuccess(() -> Component.literal(LootrAPI.clearPlayerLoot(profile.getId()) ? "Cleared stored inventories for " + playerName : "No stored inventories for " + playerName + " to clear"), true);
      return 1;
    })));
    builder.then(Commands.literal("cart").executes(c -> {
      createBlock(c.getSource(), null, null);
      return 1;
    }).then(suggestTables().executes(c -> {
      createBlock(c.getSource(), null, ResourceKey.create(Registries.LOOT_TABLE, ResourceLocationArgument.getId(c, "table")));
      return 1;
    })));
    builder.then(Commands.literal("custom").executes(c -> {
      BlockPos pos = BlockPos.containing(c.getSource().getPosition());
      Level level = c.getSource().getLevel();
      BlockState state = level.getBlockState(pos);
      if (!state.is(LootrTags.Blocks.CUSTOM_ELIGIBLE)) {
        pos = pos.below();
        state = level.getBlockState(pos);
      }
      if (!state.is(LootrTags.Blocks.CUSTOM_ELIGIBLE)) {
        c.getSource().sendSuccess(() -> Component.literal("Please stand on the container you wish to convert."), false);
      } else {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BaseContainerBlockEntity container)) {
          c.getSource().sendSuccess(() -> Component.literal("Please stand on the container you wish to convert."), false);
        }
        NonNullList<ItemStack> reference = ((MixinBaseContainerBlockEntity) blockEntity).invokeGetItems();
        BlockState newState = updateBlockState(state, LootrRegistry.getInventoryBlock().defaultBlockState());
        NonNullList<ItemStack> custom = copyItemList(reference);
        level.removeBlockEntity(pos);
        level.setBlockAndUpdate(pos, newState);
        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof LootrInventoryBlockEntity inventory)) {
          c.getSource().sendSuccess(() -> Component.literal("Unable to convert chest, BlockState is not a Lootr Inventory block."), false);
        } else {
          inventory.setCustomInventory(custom);
          inventory.setChanged();

        }
      }
      return 1;
    }));
    builder.then(Commands.literal("open_as").executes(c -> {
      c.getSource().sendSuccess(() -> Component.literal("Must provide player name."), true);
      return 1;
    }).then(suggestProfiles().executes(c -> {
      String playerName = StringArgumentType.getString(c, "profile");
      Optional<GameProfile> opt_profile = c.getSource().getServer().getProfileCache().get(playerName);
      if (!opt_profile.isPresent()) {
        c.getSource().sendFailure(Component.literal("Invalid player name: " + playerName + ", profile not found in the cache."));
        return 0;
      }
      GameProfile profile = opt_profile.get();
      BlockPos pos = BlockPos.containing(c.getSource().getPosition());
      Level level = c.getSource().getLevel();
      BlockEntity te = level.getBlockEntity(pos);
      if (!(LootrAPI.resolveBlockEntity(te) instanceof ILootrBlockEntity)) {
        pos = pos.below();
        te = level.getBlockEntity(pos);
      }
      ILootrBlockEntity ibe = LootrAPI.resolveBlockEntity(te);
      if (ibe == null) {
        c.getSource().sendSuccess(() -> Component.literal("Please stand on a valid Lootr container."), false);
        return 0;
      }

      LootrSavedData data = DataStorage.getData(ibe);
      if (data == null) {
        c.getSource().sendSuccess(() -> Component.literal("No Lootr data found for this container."), false);
        return 0;
      }
      LootrInventory inventory = data.getInventory(profile.getId());
      if (inventory == null) {
        c.getSource().sendSuccess(() -> Component.literal("No stored inventory for " + playerName + " found."), true);
        return 0;
      }

      ServerPlayer player = c.getSource().getPlayer();
      if (player == null) {
        c.getSource().sendSuccess(() -> Component.literal("Command can only be executed by a player"), false);
        return 0;
      }

      player.openMenu(inventory);

      return 1;
    })));
    builder.then(Commands.literal("open_as_uuid").executes(c -> {
      c.getSource().sendSuccess(() -> Component.literal("Must provide player UUID."), true);
      return 1;
    }).then(Commands.argument("uuid", StringArgumentType.string()).executes(c -> {
      String uuid = StringArgumentType.getString(c, "uuid");
      UUID id;
      try {
        id = UUID.fromString(uuid);
      } catch (IllegalArgumentException exception) {
        c.getSource().sendFailure(Component.literal("Invalid UUID: " + uuid));
        return 0;
      }
      BlockPos pos = BlockPos.containing(c.getSource().getPosition());
      Level level = c.getSource().getLevel();
      BlockEntity te = level.getBlockEntity(pos);
      if (!(LootrAPI.resolveBlockEntity(te) instanceof ILootrBlockEntity)) {
        pos = pos.below();
        te = level.getBlockEntity(pos);
      }
      ILootrBlockEntity ibe = LootrAPI.resolveBlockEntity(te);
      if (ibe == null) {
        c.getSource().sendSuccess(() -> Component.literal("Please stand on a valid Lootr container."), false);
        return 0;
      }

      LootrSavedData data = DataStorage.getData(ibe);
      LootrInventory inventory = data.getInventory(id);
      if (inventory == null) {
        c.getSource().sendSuccess(() -> Component.literal("No stored inventory for " + id + " found."), true);
        return 0;
      }

      ServerPlayer player = c.getSource().getPlayer();
      if (player == null) {
        c.getSource().sendSuccess(() -> Component.literal("Command can only be executed by a player"), false);
        return 0;
      }

      player.openMenu(inventory);

      return 1;
    })));
    builder.then(Commands.literal("id").executes(c -> {
      BlockPos pos = BlockPos.containing(c.getSource().getPosition());
      Level world = c.getSource().getLevel();
      BlockEntity te = world.getBlockEntity(pos);
      if (!(LootrAPI.resolveBlockEntity(te) instanceof ILootrBlockEntity)) {
        pos = pos.below();
        te = world.getBlockEntity(pos);
      }
      ILootrBlockEntity ibe = LootrAPI.resolveBlockEntity(te);
      if (ibe == null) {
        c.getSource().sendSuccess(() -> Component.literal("Please stand on a valid Lootr container."), false);
      } else {
        c.getSource().sendSuccess(() -> Component.literal("The ID of this inventory is: ").append(ComponentUtils.copyOnClickText(ibe.getInfoUUID().toString())), false);
      }
      return 1;
    }));
    builder.then(Commands.literal("refresh").executes(c -> {
      BlockPos pos = BlockPos.containing(c.getSource().getPosition());
      Level level = c.getSource().getLevel();
      BlockEntity be = level.getBlockEntity(pos);
      if (!(LootrAPI.resolveBlockEntity(be) instanceof ILootrBlockEntity)) {
        pos = pos.below();
        be = level.getBlockEntity(pos);
      }
      ILootrBlockEntity ibe = LootrAPI.resolveBlockEntity(be);
      if (ibe != null) {
        LootrAPI.setRefreshing(ibe);
        c.getSource().sendSuccess(() -> Component.literal("Container with ID " + (ibe).getInfoUUID() + " has been set to refresh with a delay of " + LootrAPI.getRefreshValue()), false);
      } else {
        c.getSource().sendSuccess(() -> Component.literal("Please stand on a valid Lootr container."), false);
      }
      return 1;
    }));
    builder.then(Commands.literal("decay").executes(c -> {
      BlockPos pos = BlockPos.containing(c.getSource().getPosition());
      Level level = c.getSource().getLevel();
      BlockEntity be = level.getBlockEntity(pos);

      if (!(LootrAPI.resolveBlockEntity(be) instanceof ILootrBlockEntity)) {
        pos = pos.below();
        be = level.getBlockEntity(pos);
      }
      ILootrBlockEntity ibe = LootrAPI.resolveBlockEntity(be);
      if (ibe != null) {
        LootrAPI.setDecaying(ibe);
        c.getSource().sendSuccess(() -> Component.literal("Container with ID " + (ibe).getInfoUUID() + " has been set to decay with a delay of " + LootrAPI.getDecayValue()), false);
      } else {
        c.getSource().sendSuccess(() -> Component.literal("Please stand on a valid Lootr container."), false);
      }
      return 1;
    }));
    builder.then(Commands.literal("openers").then(Commands.argument("location", Vec3Argument.vec3()).executes(c -> {
      BlockPos position = Vec3Argument.getCoordinates(c, "location").getBlockPos(c.getSource());
      Level world = c.getSource().getLevel();
      BlockEntity blockEntity = world.getBlockEntity(position);
      ILootrBlockEntity ibe = LootrAPI.resolveBlockEntity(blockEntity);
      if (ibe != null) {
        Set<UUID> openers = ibe.getActualOpeners();
        if (openers != null) {
          c.getSource().sendSuccess(() -> Component.literal("BlockEntity at location " + position + " has " + openers.size() + " openers. UUIDs as follows:"), true);
          for (UUID uuid : openers) {
            Optional<GameProfile> prof = c.getSource().getServer().getProfileCache().get(uuid);
            c.getSource().sendSuccess(() -> Component.literal("UUID: " + uuid + ", user profile: " + (prof.isPresent() ? prof.get().getName() : "null")), true);
          }
        }
      } else {
        c.getSource().sendSuccess(() -> Component.literal("No Lootr block entity exists at location: " + position), false);
      }
      return 1;
    })));
    builder.then(Commands.literal("convert").then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(context -> {
      BoundingBox bounds = BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to"));
      ChunkPos start = new ChunkPos(new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()));
      ChunkPos stop = new ChunkPos(new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()));
      List<ChunkPos> positions = new ArrayList<>();
      for (int x = start.x; x <= stop.x; x++) {
        for (int z = start.z; z <= stop.z; z++) {
          positions.add(new ChunkPos(x, z));
        }
      }
      ServerLevel level = context.getSource().getLevel();
      for (ChunkPos chunkPos : positions) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        List<BlockPos> convertableBlocks = new ArrayList<>();
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
          if (!bounds.isInside(pos)) {
            continue;
          }
          convertableBlocks.add(pos);
        }
        if (convertableBlocks.isEmpty()) {
          continue;
        }
        for (BlockPos pos : convertableBlocks) {
          BlockEntity blockEntity = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
          if (!(blockEntity instanceof BaseContainerBlockEntity)) {
            continue;
          }
          if (blockEntity instanceof RandomizableContainerBlockEntity lootContainer) {
            if (lootContainer.getLootTable() != null) {
              continue;
            }
          }
          BlockState state = blockEntity.getBlockState();
          if (!state.is(LootrTags.Blocks.CUSTOM_ELIGIBLE) && !blockEntity.getType().builtInRegistryHolder().is(LootrTags.BlockEntity.CUSTOM_INELIGIBlE)) {
            NonNullList<ItemStack> reference = ((MixinBaseContainerBlockEntity) blockEntity).invokeGetItems();
            BlockState newState = updateBlockState(state, LootrRegistry.getInventoryBlock().defaultBlockState());
            NonNullList<ItemStack> custom = copyItemList(reference);
            level.removeBlockEntity(pos);
            level.setBlockAndUpdate(pos, newState);
            BlockEntity te = level.getBlockEntity(pos);
            if (!(te instanceof LootrInventoryBlockEntity inventory)) {
              context.getSource().sendSuccess(() -> Component.literal("Unable to convert chest, BlockState is not a Lootr Inventory block."), false);
            } else {
              inventory.setCustomInventory(custom);
              inventory.setChanged();
            }
          }
        }
      }

      return 1;
    }))));
    return builder;
  }

  private static BlockState updateBlockState (BlockState oldState, BlockState newState) {
    if (oldState.hasProperty(BlockStateProperties.FACING) && newState.hasProperty(BlockStateProperties.FACING)) {
      newState = newState.setValue(BlockStateProperties.FACING, oldState.getValue(BlockStateProperties.FACING));
    }
    if (oldState.hasProperty(HorizontalDirectionalBlock.FACING) && newState.hasProperty(HorizontalDirectionalBlock.FACING)) {
      newState = newState.setValue(HorizontalDirectionalBlock.FACING, oldState.getValue(HorizontalDirectionalBlock.FACING));
    }
    if (oldState.hasProperty(BlockStateProperties.WATERLOGGED) && newState.hasProperty(BlockStateProperties.WATERLOGGED)) {
      newState = newState.setValue(BlockStateProperties.WATERLOGGED, oldState.getValue(BlockStateProperties.WATERLOGGED));
    }
    return newState;
  }
}

