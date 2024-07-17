package noobanidus.mods.lootr.api;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public interface IOpeners extends IMarkChanged {
  // TODO: SERVER ONLY
  @Nullable
  Set<UUID> getVisualOpeners();

  @Nullable
  Set<UUID> getActualOpeners();

  default boolean addOpener (Player player) {
    boolean result1 = addVisualOpener(player);
    boolean result2 = addActualOpener(player);
    return result1 || result2;
  }

  default boolean addVisualOpener (UUID uuid) {
    Set<UUID> openers = getVisualOpeners();
    if (openers == null) {
      return false;
    }
    if (openers.add(uuid)) {
      markChanged();
      return true;
    }
    return false;
  }

  default boolean hasVisualOpened(UUID uuid) {
    Set<UUID> openers = getVisualOpeners();
    if (openers == null) {
      return false;
    }
    return openers.contains(uuid);
  }

  default boolean removeVisualOpener (UUID uuid) {
    Set<UUID> openers = getVisualOpeners();
    if (openers == null) {
      return false;
    }
    if (openers.remove(uuid)) {
      markChanged();
      return true;
    }
    return false;
  }

  default boolean addActualOpener(UUID uuid) {
    Set<UUID> openers = getActualOpeners();
    if (openers == null) {
      return false;
    }
    if (openers.add(uuid)) {
      markChanged();
      return true;
    }
    return false;
  }


  default boolean hasOpened(UUID uuid) {
    Set<UUID> openers = getActualOpeners();
    if (openers == null) {
      return false;
    }
    return openers.contains(uuid);
  }

  default boolean hasOpened(Player player) {
    return hasOpened(player.getUUID());
  }

  default boolean addActualOpener(Player player) {
    return addActualOpener(player.getUUID());
  }

  default boolean addVisualOpener (Player player) {
    return addVisualOpener(player.getUUID());
  }

  default boolean hasVisualOpened(Player player) {
    return hasVisualOpened(player.getUUID());
  }

  default boolean removeVisualOpener (Player player) {
    return removeVisualOpener(player.getUUID());
  }
}