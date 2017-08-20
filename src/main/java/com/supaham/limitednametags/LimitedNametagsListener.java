package com.supaham.limitednametags;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Map.Entry;

final class LimitedNametagsListener implements Listener {

  private final LimitedNametagsPlugin plugin;

  public LimitedNametagsListener(LimitedNametagsPlugin plugin) {
    this.plugin = plugin;
  }
  
  @EventHandler
  public void onWorldLoad(WorldLoadEvent event) {
    if (!this.plugin.isEnabledFor(event.getWorld())) {

      // Check world config directly for late world loading.
      if (plugin._getConfig().getWorldConfig(event.getWorld()) != null) {
        this.plugin.register(event.getWorld());
      }
    }
  }
  
  @EventHandler
  public void onWorldUnload(WorldUnloadEvent event) {
    this.plugin.unregister(event.getWorld());
  }
}
