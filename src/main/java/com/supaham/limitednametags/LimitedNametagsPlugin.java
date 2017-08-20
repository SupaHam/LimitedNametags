package com.supaham.limitednametags;

import com.google.common.base.Preconditions;

import com.supaham.limitednametags.Config.WorldConfig;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LimitedNametagsPlugin extends JavaPlugin {

  private static LimitedNametagsPlugin instance;
  private Config config;
  private Map<World, NametagManager> nametagManagers = new HashMap<>();

  public static LimitedNametagsPlugin getInstance() {
    return instance;
  }

  public LimitedNametagsPlugin() {
    Preconditions.checkState(instance == null, "LimitedNametags already initialized.");
    LimitedNametagsPlugin.instance = this;
  }

  @Override
  public void onEnable() {
    if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
      getLogger().severe("LimitedNametags requires ProtocolLib to function... "
                         + "Disabling LimitedNametags.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    this.config = new Config(this);
    reload();
    getServer().getPluginManager().registerEvents(new LimitedNametagsListener(this), this);
//    ProtocolLibrary.getProtocolManager().addPacketListener(new LimitedNametagAdapter(this));
  }

  @Override
  public void saveConfig() {
    super.saveConfig();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    reload();
    sender.sendMessage(ChatColor.YELLOW + "You've successfully reloaded LimitedNametags.");
    return true;
  }

  public void reload() {
    for (Player player : getServer().getOnlinePlayers()) {
      NametagManager mgr = getNametagManager(player.getWorld());
      if (mgr != null) {
        mgr.clear(player, true);
      }
    }

    if (!this.nametagManagers.isEmpty()) {
      Iterator<Entry<World, NametagManager>> it = this.nametagManagers.entrySet().iterator();
      while (it.hasNext()) {
        it.next().getValue().destroy();
        it.remove();
      }
    }

    this.config.load();
    for (String worldName : this.config.getWorldConfigs().keySet()) {
      World world = getServer().getWorld(worldName);
      // Only register * and valid worlds during reloads.
      // A world can be invalid due to it not being loaded at the time.
      // We support late world loading in the LimitedNametagsListener class.
      if (worldName.equals("*") || world != null) {
        register(world);
      }
    }

    for (Player player : getServer().getOnlinePlayers()) {
      NametagManager mgr = getNametagManager(player.getWorld());
      if (mgr != null) {
        final PlayerRadiusSupplier supplier = PlayerRadiusSupplier
            .get(player, mgr.getRadiusFor(player), false);
        List<Player> playersToHide = supplier.get();
        mgr.hideNametags(player, playersToHide, true);
      }
    }
  }

  public boolean isEnabledFor(World world) {
    return this.nametagManagers.containsKey("*") || this.nametagManagers.containsKey(world);
  }

  public boolean register(World world) {
    if (this.nametagManagers.containsKey(world)) {
      getLogger().severe("World '" + world.getName() + "' is already registered.");
      return false;
    }
    WorldConfig config = this.config.getWorldConfig(world);
    this.nametagManagers.put(world, new NametagManager(this, world, config));
    return true;
  }
  
  public boolean unregister(World world) {
    NametagManager mgr = this.nametagManagers.remove(world);
    if(mgr == null) {
      return false;
    }
    mgr.destroy();
    return true;
  }

  public Config _getConfig() {
    return config;
  }

  public NametagManager getNametagManager(World world) {
    return this.nametagManagers.get(world);
  }
  
  public Map<World, NametagManager> getNametagManagers() {
    return Collections.unmodifiableMap(this.nametagManagers);
  }
}
