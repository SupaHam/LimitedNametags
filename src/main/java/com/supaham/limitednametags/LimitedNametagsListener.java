package com.supaham.limitednametags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

final class LimitedNametagsListener implements Listener {

  private final LimitedNametagsPlugin plugin;

  public LimitedNametagsListener(LimitedNametagsPlugin plugin) {
    this.plugin = plugin;
  }
  
  @EventHandler
  public void onWorldLoad(WorldLoadEvent event) {
    if (!this.plugin.isEnabledFor(event.getWorld())) {
      this.plugin.register(event.getWorld());
    }
  }
  
  @EventHandler
  public void onWorldUnload(WorldUnloadEvent event) {
    this.plugin.unregister(event.getWorld());
  }
}
