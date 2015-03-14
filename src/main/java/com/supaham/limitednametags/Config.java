package com.supaham.limitednametags;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Config {

  private static final ImmutableMap<String, Double> defaultGroups = ImmutableMap.
      <String, Double>builder().put("default", 10D).put("vip", 20D).build();

  private final LimitedNametagsPlugin plugin;
  private Map<String, Double> radiusGroups = new HashMap<>();
  private Map<String, WorldConfig> worldConfigs = new HashMap<>();

  public Config(LimitedNametagsPlugin plugin) {
    this.plugin = plugin;
  }

  private void writeDefaults() {
    FileConfiguration config = this.plugin.getConfig();
    boolean dirty = false;
    ConfigurationSection worldsCS = config.getConfigurationSection("worlds");
    if (worldsCS == null) {
      worldsCS = config.createSection("worlds");
      dirty = true;
    }
    Set<String> keys = worldsCS.getKeys(false);

    {
      // Default world entry
      Iterator<String> iterator = keys.iterator();
      if (!iterator.hasNext() || worldsCS.getConfigurationSection(iterator.next()) == null) {
        worldsCS.createSection("*");
        keys.add("*");
        dirty = true;
      }
    }
    
    for (String key : keys) {
      ConfigurationSection worldCS = worldsCS.getConfigurationSection(key);
      if (!worldCS.contains("enabled")) {
        worldCS.set("enabled", false);
        dirty = true;
      }
      if (!worldCS.contains("radius-groups")
          || worldCS.getConfigurationSection("radius-groups").getKeys(false).size() == 0) {
        worldCS.set("radius-groups", defaultGroups);
        dirty = true;
      }
      if (!worldCS.contains("team-name")) {
        worldCS.set("team-name", "ltd-nt");
        dirty = true;
      }
    }
    if(dirty) {
      save();
    }
  }

  void load() {
    this.plugin.reloadConfig();
    FileConfiguration config = this.plugin.getConfig();
    config.options().header("LimitedNametags by SupaHam.\n\n"
                            + "Please be careful when modifying this file. The YAML format\n"
                            + "can be tricky sometimes. Please refer to\n"
                            + "http://w.supa.me/LimitedNametags for assistance with this plugin.");

    // Add defaults
    writeDefaults();
    plugin.reloadConfig();
    config = this.plugin.getConfig();

    ConfigurationSection cs = config.getConfigurationSection("worlds");
    // Load config
    Set<String> keys = cs == null ? null : cs.getKeys(false);
    if (cs == null || keys.isEmpty()) {
      plugin.getLogger().warning("No worlds found for LimitedNametags to operate in.");
      return;
    }

    Map<String, WorldConfig> worldConfigs = new HashMap<>(this.worldConfigs);
    this.worldConfigs.clear();

    Set<String> updateStrings = new HashSet<>(2);
    for (String worldName : keys) {
      ConfigurationSection worldSection = cs.getConfigurationSection(worldName);
      WorldConfig worldConfig = worldConfigs.get(worldName);
      if (worldConfig == null) {
        worldConfig = new WorldConfig();
      }
      for (Entry<String, Object> entry : worldSection.getValues(false).entrySet()) {
        String key = entry.getKey();
        switch (key) {
          case "enabled":
            worldConfig.enabled = ((boolean) entry.getValue());
            break;
          case "nametag-radius": // OUTDATED, Replaced by radius-groups
            worldSection.set("nametag-radius", null);
            updateStrings.add("Replaced nametag-radius with radius-groups");
            break;
          case "radius-groups":
            Map<String, Object> values = ((ConfigurationSection) entry.getValue()).getValues(false);
            worldConfig.radiusGroups = Maps.transformValues(values, ObjectToDouble.INSTANCE);
            break;
          case "team-name":
            worldConfig.teamName = ((String) entry.getValue());
            break;
        }
      }
      if (worldConfig.enabled) {
        this.worldConfigs.put(worldName, worldConfig);
      }
    }

    if (!updateStrings.isEmpty()) {
      plugin.getLogger().warning(Joiner.on(", ").join(updateStrings) + ". Saving defaults "
                                 + "please check your config for the updates...");
    }
    save();
  }

  void save() {
    this.plugin.saveConfig();
  }

  public Map<String, WorldConfig> getWorldConfigs() {
    return worldConfigs;
  }

  public WorldConfig getWorldConfig(World world) {
    return getWorldConfig(world == null ? null : world.getName());
  }

  public WorldConfig getWorldConfig(String worldName) {
    WorldConfig config = this.worldConfigs.get(worldName == null ? "*" : worldName);
    return config != null && config.enabled ? config : null;
  }

  public static class WorldConfig {

    private boolean enabled;
    private Map<String, Double> radiusGroups;
    private String teamName;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof WorldConfig)) {
        return false;
      }
      WorldConfig wc = (WorldConfig) obj;
      return this.radiusGroups.equals(wc.radiusGroups) && this.teamName.equals(wc.teamName);
    }

    public Map<String, Double> getRadiusGroups() {
      return radiusGroups;
    }

    public String getTeamName() {
      return teamName;
    }
  }
  
  private static final class ObjectToDouble implements Function<Object, Double> {

    public static final ObjectToDouble INSTANCE = new ObjectToDouble();
    @Override
    public Double apply(Object input) {
      return ((Double) input);
    }
  }
}
