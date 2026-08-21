package com.canopycreations.mysticcraft.data;

import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Simple flat-file (YAML per player) persistence layer.
 * Swap this out for SQL/SQLite later if the server grows -
 * the RaceManager only talks to this class, never to files directly.
 */
public class DataStore {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    private PlayerData load(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        PlayerData data = new PlayerData(uuid);
        if (file.exists()) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            Race race = Race.fromString(yml.getString("race", "HUMAN"));
            data.setRace(race != null ? race : Race.HUMAN);
            data.setHumanity(yml.getInt("humanity", 100));
            data.setDaylightRingEquipped(yml.getBoolean("daylightRingEquipped", false));
            data.setVampireAbilitiesActive(yml.getBoolean("vampireAbilitiesActive", true));
            data.setTriggeredCurse(yml.getBoolean("hasTriggeredCurse", false));
            data.setLastRaceSwitchMillis(yml.getLong("lastRaceSwitchMillis", 0L));
            data.setLastShiftMillis(yml.getLong("lastShiftMillis", 0L));
            data.setHasVampireBloodInSystem(yml.getBoolean("hasVampireBloodInSystem", false));
            data.setVampireBloodExpiresAtMillis(yml.getLong("vampireBloodExpiresAtMillis", 0L));
            data.setPendingTransition(yml.getBoolean("pendingTransition", false));
            data.setTransitioning(yml.getBoolean("transitioning", false));
            data.setTransitionDeadlineMillis(yml.getLong("transitionDeadlineMillis", 0L));
            data.setOriginalVampire(yml.getBoolean("originalVampire", false));
            data.setLatentWolfGene(yml.getBoolean("latentWolfGene", false));
            data.setGeneRollDone(yml.getBoolean("geneRollDone", false));
            data.setBloodline(com.canopycreations.mysticcraft.lore.Bloodline.fromString(yml.getString("bloodline", null)));
        }
        return data;
    }

    public void save(PlayerData data) {
        File file = new File(dataFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("race", data.getRace().name());
        yml.set("humanity", data.getHumanity());
        yml.set("daylightRingEquipped", data.isDaylightRingEquipped());
        yml.set("vampireAbilitiesActive", data.isVampireAbilitiesActive());
        yml.set("hasTriggeredCurse", data.hasTriggeredCurse());
        yml.set("lastRaceSwitchMillis", data.getLastRaceSwitchMillis());
        yml.set("lastShiftMillis", data.getLastShiftMillis());
        yml.set("hasVampireBloodInSystem", data.hasVampireBloodInSystem());
        yml.set("vampireBloodExpiresAtMillis", data.getVampireBloodExpiresAtMillis());
        yml.set("pendingTransition", data.isPendingTransition());
        yml.set("transitioning", data.isTransitioning());
        yml.set("transitionDeadlineMillis", data.getTransitionDeadlineMillis());
        yml.set("originalVampire", data.isOriginalVampire());
        yml.set("latentWolfGene", data.hasLatentWolfGene());
        yml.set("geneRollDone", data.isGeneRollDone());
        yml.set("bloodline", data.getBloodline() == null ? null : data.getBloodline().name());
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player data for " + data.getUuid(), e);
        }
    }

    public void saveAll() {
        cache.values().forEach(this::save);
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }
}
