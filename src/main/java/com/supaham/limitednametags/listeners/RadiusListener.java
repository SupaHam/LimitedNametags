package com.supaham.limitednametags.listeners;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import com.supaham.limitednametags.NametagManager;
import com.supaham.limitednametags.PlayerRadiusSupplier;
import com.supaham.limitednametags.SimpleNametagListener;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ali on 26/03/2015.
 */
public class RadiusListener extends SimpleNametagListener<Player> {

  private final Map<Player, Double> radiusCache = new HashMap<>();
  private final Multimap<Player, Player> distanceFarCache = HashMultimap.create();
  private final Multimap<Player, Player> distanceCloseCache = HashMultimap.create();
  private BukkitRunnable task;
  private final double radius;

  /**
   * Constructs a new {@link SimpleNametagListener} with a type extending entity.
   *
   * @param radius radius to display nametags, anything less 
   */
  public RadiusListener(double radius) {
    super(Player.class);
    this.radius = radius;
  }

  @Override
  public void onEnable(NametagManager manager) {
    setupTask(manager, true);
  }

  @Override
  public void onReload(NametagManager manager) {
    this.radiusCache.clear();
  }

  @Override
  public void onDisable(NametagManager manager) {
    setupTask(manager, false);
    this.radiusCache.clear();
  }

  @Override
  public Collection<Player> hide(Player forPlayer, Collection<Player> players) {
    if (this.radius == 0) {
      return players;
    } else if (this.radius < 0 || this.radius > 55) {
      return Collections.emptyList();
    }
    Collection<Player> cache = distanceFarCache.get(forPlayer);
    if(cache.isEmpty()) { // first time
      cache = PlayerRadiusSupplier.get(forPlayer, players, this.radius, false).get();
      distanceFarCache.putAll(forPlayer, cache);
    }
    // Be sure to add the forPlayer's cache to each of their cached players
    for (Player player : cache) {
      distanceFarCache.put(player, forPlayer);
    }
    return cache;
  }

  @Override
  public Collection<Player> show(Player forPlayer, Collection<Player> players) {
    if (this.radius == 0) {
      return players;
    } else if (this.radius < 0) {
      return Collections.emptyList();
    }
    Collection<Player> cache = distanceCloseCache.get(forPlayer);
    if(cache.isEmpty()) { // first time
      cache = PlayerRadiusSupplier.get(forPlayer, players, this.radius, true).get();
      distanceCloseCache.putAll(forPlayer, cache);
    }
    // Be sure to add the forPlayer's cache to each of their cached players
    for (Player player : cache) {
      distanceCloseCache.put(player, forPlayer);
    }
    return cache;
  }
  
  private void setupTask(NametagManager manager, boolean create) {
    if(this.task != null) {
      this.task.run();
      this.task.cancel();
      this.task = null;
    }
    if (!create) {
      return;
    }
    task = new BukkitRunnable() {
      @Override
      public void run() {
        distanceFarCache.clear();
        distanceCloseCache.clear();
      }
    };
    task.runTaskTimer(manager.getPlugin(), 2L, 2L); 
    // TODO make configurable, and maybe revert back to 1 tick?
  }
}
