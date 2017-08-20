package com.supaham.limitednametags;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.supaham.limitednametags.Config.WorldConfig;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class NametagManager {

  public static final PacketContainer
      CREATE_TEAM_PACKET = new PacketContainer(Server.SCOREBOARD_TEAM),
      ADD_PACKET = new PacketContainer(Server.SCOREBOARD_TEAM),
      REMOVE_PACKET = new PacketContainer(Server.SCOREBOARD_TEAM);
  public static final String PERMISSION_PREFIX = "limitednametags.radius.";
  
  private final Plugin plugin;
  private final World world;
  private final String worldName; // world name, if world is null, this is *
  private final String teamName;
  private final Map<String, Double> radiusGroups;
  // A list of uuids in the nametags team for each player.
  private final SetMultimap<Player, Player> nametags = HashMultimap.create();
  // packets to send
  private final PacketContainer createTeamPacket;
  private final PacketContainer addPacket;
  private final PacketContainer removePacket;
  private final NametagsListener listener;
  private final Map<Player, Double> radiusCache = new HashMap<>();
  
  private Set<NametagListener> listeners = new HashSet<>();
  // This task is used to validate players nametags every so often.
  private VisibilityValidatorTask validatorTask;

  static {
    CREATE_TEAM_PACKET.getIntegers().write(1, 0); // mode
    CREATE_TEAM_PACKET.getIntegers().write(2, 1); // friendly fire off
    CREATE_TEAM_PACKET.getStrings().write(4, "never");
    ADD_PACKET.getIntegers().write(1, 3); // mode 
    REMOVE_PACKET.getIntegers().write(1, 4); // mode
  }

  public static void sendPacket(Player player, PacketContainer packet) {
    try {
      ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  public NametagManager(Plugin plugin, World world, WorldConfig worldConfig) {
    checkNotNull(plugin, "plugin cannot be null.");
    checkNotNull(worldConfig, "world config cannot be null.");
    this.plugin = plugin;
    this.worldName = world == null ? "*" : world.getName();

    checkNotNull(worldConfig.getTeamName(),
                 "team name for world '" + this.worldName + "' cannot be null.");
    checkArgument(!worldConfig.getTeamName().isEmpty(),
                  "team name for world '" + this.worldName + "' cannot be empty.");
    this.world = world;
    this.teamName = worldConfig.getTeamName();
    this.radiusGroups = worldConfig.getRadiusGroups();
    this.listener = new NametagsListener(this);
    plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

    validatorTask = new VisibilityValidatorTask();
    validatorTask.runTaskTimer(plugin, 20L, 20L);

    PacketContainer packet = new PacketContainer(Server.SCOREBOARD_TEAM);
    packet.getStrings().write(0, teamName);

    this.createTeamPacket = CREATE_TEAM_PACKET.shallowClone();
    this.createTeamPacket.getStrings().write(0, teamName);
    this.addPacket = ADD_PACKET.shallowClone();
    this.addPacket.getStrings().write(0, teamName);
    this.removePacket = REMOVE_PACKET.shallowClone();
    this.removePacket.getStrings().write(0, teamName);
  }

  public void destroy() {
    HandlerList.unregisterAll(this.listener);
    this.validatorTask.cancel();
  }

  public void createTeamFor(Player player) {
    PacketContainer packet = this.createTeamPacket.shallowClone();
    packet.getSpecificModifier(Collection.class).write(0, Arrays.asList(player.getName()));
    sendPacket(player, packet);
  }

  public void hideNametag(Player forPlayer, Player playerToHide, boolean notifyOthers) {
    hideNametags(forPlayer, Arrays.asList(playerToHide), notifyOthers);
  }

  public void hideNametags(Player forPlayer, Collection<Player> playersToHide,
                           boolean notifyOthers) {
    Preconditions.checkNotNull(forPlayer, "player cannot be null.");
    Preconditions.checkNotNull(playersToHide, "players collection cannot be null.");
    if (playersToHide.isEmpty()) {
      return;
    }
    playersToHide.remove(forPlayer);
    PacketContainer packet = this.addPacket.shallowClone();
    List<String> toAdd = new ArrayList<>();
    packet.getSpecificModifier(Collection.class).write(0, toAdd);
    for (Player player : playersToHide) {
      if (this.nametags.containsEntry(forPlayer, player)) {
        continue;
      }
      if (toAdd.size() >= 450) { // pretty big IF
        break;
      }
      toAdd.add(player.getName());
      if (notifyOthers) {
        hideNametag(player, forPlayer, false);
      }
    }
    sendPacket(forPlayer, packet);
    Multimaps.synchronizedMultimap(this.nametags).putAll(forPlayer, playersToHide);
  }

  public void showNametag(Player forPlayer, Player playerToShow, boolean notifyOthers) {
    showNametags(forPlayer, Arrays.asList(playerToShow), notifyOthers);
  }

  public void showNametags(Player forPlayer, Collection<Player> playersToShow,
                           boolean notifyOthers) {
    Preconditions.checkNotNull(forPlayer, "player cannot be null.");
    Preconditions.checkNotNull(playersToShow, "players collection cannot be null.");
    if (playersToShow.isEmpty()) {
      return;
    }
    // Remove that bugger. hah get it? BUG-ger.
    if (playersToShow.contains(forPlayer)) {
      playersToShow.remove(forPlayer);
    }
    PacketContainer packet = this.removePacket.shallowClone();
    List<String> toRemove = new ArrayList<>();
    packet.getSpecificModifier(Collection.class).write(0, toRemove);
    for (Player player : playersToShow) {
      if (!this.nametags.containsEntry(forPlayer, player)) {
        continue;
      }
      if (toRemove.size() >= 450) { // pretty big IF
        break;
      }
      toRemove.add(player.getName());
      if (notifyOthers) {
        showNametag(player, forPlayer, false);
      }
    }
    if (!toRemove.isEmpty()) {
      sendPacket(forPlayer, packet);
    }
    Multimap<Player, Player> map = Multimaps.synchronizedMultimap(this.nametags);
    for (Player player : playersToShow) {
      map.remove(forPlayer, player);
    }
  }

  public void clear(Player player, boolean sendPacketsToPlayer) {
    // Remove them from validator task
    Collections.synchronizedMap(validatorTask.lastUpdate).remove(player);
    // Clone array list as it is modified in showNametag
    Collection<Player> players = new ArrayList<>(Multimaps.synchronizedMultimap(this.nametags).get(player));
    for (Player player1 : players) {
      showNametag(player1, player, sendPacketsToPlayer);
    }
  }

  public Plugin getPlugin() {
    return plugin;
  }

  public World getWorld() {
    return world;
  }

  public String getTeamName() {
    return teamName;
  }

  public double getRadiusFor(Player player) {
    Double radius = radiusCache.get(player);
    if (radius != null) {
      return radius;
    }
    String group = null;
    for (String iGroup : this.radiusGroups.keySet()) {
      if (player.hasPermission(PERMISSION_PREFIX + iGroup)
          || player.hasPermission(PERMISSION_PREFIX +
                                  StringUtils.normalizeString(worldName))) {
        group = iGroup;
      }
    }
    if (group == null) {
      group = "default";
    }
    radius = this.radiusGroups.get(group);
    radiusCache.put(player, radius);
    return radius;
  }

  public SetMultimap<Player, Player> getNametags() {
    return nametags;
  }

  public Set<NametagListener> getListeners() {
    return listeners;
  }

  public void setListeners(Set<NametagListener> listeners) {
    Preconditions.checkNotNull(listeners, "listeners cannot be null.");
    this.listeners = listeners;
  }
  
  /**
   * All suppliers must be sync to prevent CMEs from nms.world.entityList. TODO create our own
   * player list later
   */
  public static class NametagsListener implements Listener {

    private final NametagManager mgr;

    public NametagsListener(NametagManager manager) {
      this.mgr = manager;
    }

    private boolean sameWorld(World world) {
      return this.mgr.getWorld() == null || this.mgr.getWorld().equals(world);
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
      final Player player = event.getPlayer();
      if (!sameWorld(player.getWorld())) {
        return;
      }

      final List<Player> list = PlayerRadiusSupplier
          .get(player, mgr.getRadiusFor(player), false).get();
      // Add an extra second to the task since the player just logged in, isn't worth the effort.
      mgr.validatorTask.lastUpdate.put(player, System.currentTimeMillis() + 1000);
      new BukkitRunnable() {
        @Override
        public void run() {
          mgr.createTeamFor(player);
          mgr.hideNametags(player, list, true);
        }
      }.runTaskAsynchronously(this.mgr.getPlugin());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
      final Player player = event.getPlayer();
      if (!sameWorld(player.getWorld())) {
        return;
      }
      mgr.radiusCache.remove(event.getPlayer());
      new BukkitRunnable() {
        @Override
        public void run() {
          mgr.clear(player, false);
        }
      }.runTaskAsynchronously(this.mgr.getPlugin());
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
      final Player player = event.getPlayer();
      
      // Player used to be in our world, clear them out.
      if (sameWorld(event.getFrom())) {
        // Clear any nametags the player might have had in this world
        new BukkitRunnable() {
          @Override
          public void run() {
            mgr.clear(player, true);
          }
        }.runTaskAsynchronously(mgr.getPlugin());
      }
      // This case is nececssary for NametagManagers that include wildcards.
      // This will return false if this NametagManager does not manage one or more worlds.
      if (!sameWorld(player.getWorld())) {
        return;
      }
      
      if (!sameWorld(player.getWorld())) {
        return;
      }
      
      final List<Player> list = PlayerRadiusSupplier
          .get(player, mgr.getRadiusFor(player), false).get();
      new BukkitRunnable() {
        @Override
        public void run() {
          // Precaution clear? :3
          mgr.clear(player, true);

          // Create team for players that join unregistered nametag worlds.
          mgr.createTeamFor(player);
          // TODO abstract this to completely support plugins doing their own nametag managers.
          if (((LimitedNametagsPlugin) mgr.plugin).isEnabledFor(player.getWorld())) {
            mgr.hideNametags(player, list, true);
          }
        }
      }.runTaskAsynchronously(this.mgr.getPlugin());
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
      final NametagManager mgr = this.mgr;
      if (!sameWorld(event.getPlayer().getWorld())) {
        return;
      }
      if (!sameBlock(event.getFrom(), event.getTo())) {
        final Player player = event.getPlayer();
        mgr.validatorTask.lastUpdate.put(player, System.currentTimeMillis());
        // Everyone within the moving player
        final List<Player> list = PlayerRadiusSupplier
            .get(player, mgr.getRadiusFor(event.getPlayer()), true).get();
//        list.remove(player);
        final List<Player> worldPlayers = new ArrayList<>(player.getWorld().getPlayers());
        new BukkitRunnable() {
          @Override
          public void run() {
            // Show nametags of those in range to moving player.
            for (Player otherPlayer : list) {
              // Don't notify others, we need to do that ourselves using the modified worldPlayers to ensure radius
              // specifications are followed
              boolean notify = false;
              mgr.showNametag(player, otherPlayer, notify);
            }

            // Hide all other nametags from moving player.
            worldPlayers.removeAll(list);
            mgr.hideNametags(player, worldPlayers, true);
          }
        }.runTaskAsynchronously(this.mgr.getPlugin());
      }
    }

    public static boolean sameBlock(Location o, Location o2) {
      return o == null && o2 == null ||
             (o != null && o2 != null) && (o.getBlockX() == o2.getBlockX()) && (o.getBlockY() == o2
                 .getBlockY()) &&
             (o.getBlockZ() == o2.getBlockZ());
    }
  }

  private final class VisibilityValidatorTask extends BukkitRunnable {

    private final Map<Player, Long> lastUpdate = new HashMap<>();

    @Override
    public void run() {
      for (Entry<Player, Long> entry : lastUpdate.entrySet()) {
        if (System.currentTimeMillis() - entry.getValue() > 1000) {
          final Player player = entry.getKey();
          final List<Player> hidePlayers = PlayerRadiusSupplier
              .get(player, getRadiusFor(player), false).get();
          final List<Player> showPlayers = PlayerRadiusSupplier
              .get(player, getRadiusFor(player), true).get();
          hidePlayers.removeAll(showPlayers);
          new BukkitRunnable() {
            @Override
            public void run() {
              hideNametags(player, hidePlayers, false);
              showNametags(player, showPlayers, false);
            }
          }.runTaskAsynchronously(plugin);
        }
      }
    }
  }
}
