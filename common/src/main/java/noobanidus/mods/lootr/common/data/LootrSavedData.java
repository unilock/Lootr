package noobanidus.mods.lootr.common.data;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import noobanidus.mods.lootr.common.api.data.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class LootrSavedData extends SavedData implements ILootrSavedData {
  private boolean hasBeenOpened;
  private boolean justLoaded;
  private ILootrInfo info;
  private final Map<UUID, LootrInventory> inventories = new HashMap<>();
  private final Set<UUID> openers = new ObjectLinkedOpenHashSet<>();
  private final Set<UUID> actualOpeners = new ObjectLinkedOpenHashSet<>();

  protected LootrSavedData(ILootrInfo info) {
    this.info = BaseLootrInfo.copy(info);
    this.justLoaded = false;
  }

  protected LootrSavedData(ILootrInfo info, boolean noCopy) {
    this.info = info;
    this.justLoaded = true;
  }

  public static Supplier<LootrSavedData> fromInfo(ILootrInfo info) {
    return () -> new LootrSavedData(info);
  }

  public static LootrSavedData load(CompoundTag compound, HolderLookup.Provider provider) {
    ILootrInfo info = ILootrInfo.loadInfoFromTag(compound, provider);
    LootrSavedData data = new LootrSavedData(info, true);
    data.inventories.clear();
    data.openers.clear();
    data.actualOpeners.clear();

    ListTag compounds = compound.getList("inventories", Tag.TAG_COMPOUND);

    for (int i = 0; i < compounds.size(); i++) {
      CompoundTag thisTag = compounds.getCompound(i);
      CompoundTag itemTag = thisTag.getCompound("chest");
      NonNullList<ItemStack> items = info.buildInitialInventory();
      ContainerHelper.loadAllItems(itemTag, items, provider);
      UUID uuid = thisTag.getUUID("uuid");
      data.inventories.put(uuid, new LootrInventory(data, items));
    }

    if (compound.contains("openers")) {
      ListTag openers = compound.getList("openers", Tag.TAG_INT_ARRAY);
      for (Tag opener : openers) {
        data.openers.add(NbtUtils.loadUUID(opener));
      }
    }
    if (compound.contains("actualOpeners")) {
      ListTag openers = compound.getList("actualOpeners", Tag.TAG_INT_ARRAY);
      for (Tag opener : openers) {
        data.actualOpeners.add(NbtUtils.loadUUID(opener));
      }
    }
    if (compound.contains("hasBeenOpened")) {
      data.hasBeenOpened = compound.getBoolean("hasBeenOpened");
    }
    return data;
  }



  @Override
  public ILootrInfo getRedirect() {
    return info;
  }

  @Override
  public Set<UUID> getVisualOpeners() {
    return openers;
  }

  @Override
  public boolean addVisualOpener(UUID uuid) {
    boolean result = ILootrSavedData.super.addVisualOpener(uuid);
    if (result) {
      setDirty();
    }
    return result;
  }

  @Override
  public boolean removeVisualOpener(UUID uuid) {
    boolean result = ILootrSavedData.super.removeVisualOpener(uuid);
    if (result) {
      setDirty();
    }
    return result;
  }

  @Override
  public boolean addActualOpener(UUID uuid) {
    boolean result = ILootrSavedData.super.addActualOpener(uuid);
    if (result) {
      setDirty();
    }
    return result;
  }

  private void removeOpener (UUID uuid) {
    /*Set<UUID> actualOpeners = getActualOpeners();
    if (actualOpeners != null) {
      if (actualOpeners.remove(uuid)) {
        setDirty();
      }
    }*/
    Set<UUID> visualOpeners = getVisualOpeners();
    if (visualOpeners != null) {
      if (visualOpeners.remove(uuid)) {
        setDirty();
      }
    }
  }

  @Override
  public Set<UUID> getActualOpeners() {
    return actualOpeners;
  }

  @Override
  public void markChanged() {
    setDirty();
  }

  @Override
  public void markDataChanged() {
    markChanged();
  }

  @Nullable
  public LootrInventory getInventory(UUID id) {
    LootrInventory inventory = inventories.get(id);
    if (inventory != null) {
      inventory.setInfo(this);
    }
    return inventory;
  }

  @Override
  public LootrInventory createInventory(ILootrInfoProvider provider, ServerPlayer player, LootFiller filler) {

    LootrInventory result = new LootrInventory(this, provider.buildInitialInventory());
    filler.unpackLootTable(provider, player, result);
    inventories.put(player.getUUID(), result);
    hasBeenOpened = true;
    setDirty();
    return result;
  }

  @Override
  public CompoundTag save(CompoundTag compound, HolderLookup.Provider provider) {
    this.info.saveInfoToTag(compound, provider);

    ListTag compounds = new ListTag();
    for (Map.Entry<UUID, LootrInventory> entry : inventories.entrySet()) {
      CompoundTag thisTag = new CompoundTag();
      thisTag.putUUID("uuid", entry.getKey());
      thisTag.put("chest", entry.getValue().saveToTag(provider));
      compounds.add(thisTag);
    }
    compound.put("inventories", compounds);
    ListTag openers = new ListTag();
    for (UUID opener : this.openers) {
      openers.add(NbtUtils.createUUID(opener));
    }
    compound.put("openers", openers);
    ListTag actualOpeners = new ListTag();
    for (UUID opener : this.actualOpeners) {
      actualOpeners.add(NbtUtils.createUUID(opener));
    }
    compound.put("actualOpeners", actualOpeners);

    compound.putBoolean("hasBeenOpened", hasBeenOpened);

    return compound;
  }

  @Override
  public boolean shouldUpdate() {
    return justLoaded;
  }

  @Override
  public void update(ILootrInfo info) {
    if (shouldUpdate() || info.getInfoPos() != getInfoPos() || info.getInfoDimension() != getInfoDimension()) {
      markChanged();
      this.info = info;
      justLoaded = false;
    }
  }

  @Override
  public void refresh() {
    inventories.clear();
    hasBeenOpened = false;
  }

  // This is triggered in createInventory and reset in refresh.
  @Override
  public boolean hasBeenOpened() {
    return hasBeenOpened;
  }

  @Override
  public boolean isPhysicallyOpen() {
    return false;
  }

  @Override
  public boolean clearInventories(UUID id) {
    if (inventories.remove(id) != null) {
      removeOpener(id);
      setDirty();
      return true;
    }

    return false;
  }

  @Override
  public void save(File pFile, HolderLookup.Provider provider) {
    if (isDirty()) {
      pFile.getParentFile().mkdirs();
    }
    super.save(pFile, provider);
  }
}
