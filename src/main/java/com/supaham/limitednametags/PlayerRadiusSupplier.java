package com.supaham.limitednametags;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class PlayerRadiusSupplier implements Supplier<List<Player>> {

  private final WeakReference<Player> player;
  private final double radius;
  private final boolean within;

  public static PlayerRadiusSupplier get(Player player, double radius, boolean within) {
    Preconditions.checkNotNull(player, "player cannot be null.");
    return new PlayerRadiusSupplier(player, radius, within);
  }

  public PlayerRadiusSupplier(Player player, double radius, boolean within) {
    this.player = new WeakReference<>(player);
    this.radius = radius;
    this.within = within;
  }

  @Override
  public List<Player> get() {
    Player player = this.player.get();
    if (player == null) {
      return Collections.emptyList();
    }
    List<Player> list = player.getWorld().getPlayers();
    // if radius is larger than 0 start removing, otherwise return all players in the our player's 
    // world.
    if (radius > 0) {
      for (int i = list.size() - 1; i >= 0; i--) {
        Player worldPlayer = list.get(i);
        if (worldPlayer.equals(player) || !player.canSee(worldPlayer)) {
          list.remove(i);
          continue;
        }
        if (this.within) {
          if (worldPlayer.getLocation().distance(player.getLocation()) > radius) {
            list.remove(i);
          }
        } else {
          if (worldPlayer.getLocation().distance(player.getLocation()) <= radius) {
            list.remove(i);
          }
        }
      }
    }
    return list;
  }
}
