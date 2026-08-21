package com.canopycreations.mysticcraft.world;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Where the landmarks actually are, and what standing in them does.
 *
 * Locations are persisted so an admin can place them once (either via the
 * generator or by hand after building properly) and have every mechanic
 * downstream just work.
 */
public class LandmarkManager {

    private final MysticCraft plugin;
    private final Map<Landmark, Location> locations = new EnumMap<>(Landmark.class);
    private final Map<Landmark, Integer> radii = new EnumMap<>(Landmark.class);
    private final Map<UUID, Landmark> currentlyInside = new HashMap<>();
    private final File file;

    private static final int DEFAULT_RADIUS = 24;

    public LandmarkManager(MysticCraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "landmarks.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (Landmark l : Landmark.values()) {
            String path = l.name();
            if (!yml.contains(path + ".world")) continue;
            World world = Bukkit.getWorld(yml.getString(path + ".world", ""));
            if (world == null) continue;
            locations.put(l, new Location(world,
                    yml.getDouble(path + ".x"),
                    yml.getDouble(path + ".y"),
                    yml.getDouble(path + ".z")));
            radii.put(l, yml.getInt(path + ".radius", DEFAULT_RADIUS));
        }
        plugin.getLogger().info("Loaded " + locations.size() + " landmarks.");
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        locations.forEach((l, loc) -> {
            String path = l.name();
            yml.set(path + ".world", loc.getWorld().getName());
            yml.set(path + ".x", loc.getX());
            yml.set(path + ".y", loc.getY());
            yml.set(path + ".z", loc.getZ());
            yml.set(path + ".radius", radii.getOrDefault(l, DEFAULT_RADIUS));
        });
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save landmarks.yml", e);
        }
    }

    public void setLocation(Landmark landmark, Location location, int radius) {
        locations.put(landmark, location);
        radii.put(landmark, radius);
        save();
    }

    public Location getLocation(Landmark landmark) {
        return locations.get(landmark);
    }

    public boolean isPlaced(Landmark landmark) {
        return locations.containsKey(landmark);
    }

    public int placedCount() {
        return locations.size();
    }

    /** Which landmark, if any, a location falls inside. */
    public Landmark landmarkAt(Location location) {
        for (Map.Entry<Landmark, Location> e : locations.entrySet()) {
            Location l = e.getValue();
            if (!l.getWorld().equals(location.getWorld())) continue;
            int r = radii.getOrDefault(e.getKey(), DEFAULT_RADIUS);
            if (l.distanceSquared(location) <= (double) r * r) return e.getKey();
        }
        return null;
    }

    /** True if a chunk overlaps a landmark that forbids clan claims. */
    public boolean isUnclaimable(Location location) {
        Landmark at = landmarkAt(location);
        if (at == null) return false;
        return at.getRole() == Landmark.Role.NEUTRAL_GROUND
                || at.getRole() == Landmark.Role.SANCTUARY
                || at.getRole() == Landmark.Role.WHITE_OAK;
    }

    /** True if PvP is forbidden here. */
    public boolean isSanctuary(Location location) {
        Landmark at = landmarkAt(location);
        return at != null && at.getRole() == Landmark.Role.SANCTUARY;
    }

    /**
     * Called on the slow tick. Announces arrival at a landmark once, and
     * applies whatever that ground does to whoever is standing on it.
     */
    public void tick(Player player) {
        Landmark at = landmarkAt(player.getLocation());
        Landmark was = currentlyInside.get(player.getUniqueId());

        if (at == null) {
            if (was != null) currentlyInside.remove(player.getUniqueId());
            return;
        }

        if (at != was) {
            currentlyInside.put(player.getUniqueId(), at);
            player.sendTitle("", at.getFormattedName(), 10, 40, 15);
            player.sendActionBar("§8" + at.getShortDescription());
            plugin.getCodexManager().discover(player, fragmentFor(at));
        }

        applyEffect(player, at);
    }

    private com.canopycreations.mysticcraft.lore.LoreFragment fragmentFor(Landmark landmark) {
        return switch (landmark) {
            case THE_WHITE_OAK -> com.canopycreations.mysticcraft.lore.LoreFragment.THE_ASH_MOTHER;
            case THE_BURNED_CHURCH, THE_OLD_CEMETERY ->
                    com.canopycreations.mysticcraft.lore.LoreFragment.THE_PRICE;
            case THE_TOMB -> com.canopycreations.mysticcraft.lore.LoreFragment.THE_UNBURIED;
            case LOCKRIDGE_MANOR -> com.canopycreations.mysticcraft.lore.LoreFragment.SEVEN_KNIVES;
            case THE_BOARDING_HOUSE -> com.canopycreations.mysticcraft.lore.LoreFragment.THE_THIRST;
            default -> com.canopycreations.mysticcraft.lore.LoreFragment.SOMETHING_IN_THE_DARK;
        };
    }

    private void applyEffect(Player player, Landmark landmark) {
        Race race = plugin.getRaceManager().getRace(player);

        switch (landmark.getRole()) {
            case CONSECRATED -> {
                if (race == Race.WITCH) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, false));
                } else if (race == Race.VAMPIRE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, false));
                    player.sendActionBar("§5This ground doesn't want you on it.");
                }
            }
            case WARDED -> {
                if (race == Race.VAMPIRE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, false));
                    player.sendActionBar("§bRunning water. It fights you.");
                }
            }
            case COVEN_SEAT -> {
                if (race == Race.WITCH) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, false));
                }
            }
            case COURT_SEAT -> {
                if (race == Race.VAMPIRE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, true, false));
                }
            }
            case PACK_SEAT -> {
                if (race == Race.WEREWOLF) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false));
                }
            }
            case SANCTUARY -> player.sendActionBar("§6Neutral ground. Nobody swings first in here.");
            case WHITE_OAK -> {
                if (race == Race.VAMPIRE) {
                    player.sendActionBar("§fEvery part of you knows what this tree is.");
                }
            }
            default -> { }
        }
    }

    /** True if the coven seat should discount spell costs for this caster. */
    public boolean atCovenSeat(Player player) {
        Landmark at = landmarkAt(player.getLocation());
        return at != null && at.getRole() == Landmark.Role.COVEN_SEAT;
    }

    /** True if witch power should be amplified here. */
    public boolean isConsecrated(Player player) {
        Landmark at = landmarkAt(player.getLocation());
        return at != null && at.getRole() == Landmark.Role.CONSECRATED;
    }
}
