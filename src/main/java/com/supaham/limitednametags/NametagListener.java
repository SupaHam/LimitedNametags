package com.supaham.limitednametags;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Represents a listener for a {@link NametagManager} used to determine whether a player should see
 * another player's nametag.
 *
 * @param <T> type of class this Nametag listener listens for
 */
public interface NametagListener<T extends Entity> {

  /**
   * Called when this listener is being enabled.
   */
  void onEnable(NametagManager manager);

  /**
   * Called when this listener is being reloaded.
   */
  void onReload(NametagManager manager);

  /**
   * Called when this listener is being disabled.
   */
  void onDisable(NametagManager manager);

  /**
   * Returns whether to hide a player's nametag for another player.
   * <p />
   * If you wanted to hide Spiderman's nametag from SupaHam then {@code hide(SupaHam, Spiderman)}
   * should return true.
   *
   * @param forPlayer player to hide the nametag from
   * @param players collection of player nametags to hide
   *
   * @return a collection of nametag owners (players) to hide from {@code forPlayer}
   */
  Collection<Player> hide(Player forPlayer, Collection<Player> players);

  /**
   * Returns whether to show a player's nametag for another player.
   * <p />
   * If you wanted to show Spiderman's nametag to SupaHam then {@code show(SupaHam, Spiderman)}
   * should return true.
   *
   * @param forPlayer player to show the nametag to
   * @param players collection of player nametags to show
   *
   * @return a collection of nametag owners (players) to show to {@code forPlayer}
   */
  Collection<Player> show(Player forPlayer, Collection<Player> players);

  /**
   * Gets the {@link Class} (extending entity) that this listener handles.
   *
   * @return class type
   */
  Class<T> getClassType();
}
