package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.lore.LoreFragment;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Per-player codex progress.
 *
 * Discovery is deliberately quiet - a short atmospheric line and a sound,
 * not a screen-filling achievement banner. The goal is for a player to
 * notice they've learned something, not to be congratulated.
 */
public class CodexManager {

    private final MysticCraft plugin;
    private final Map<UUID, Set<LoreFragment>> discovered = new HashMap<>();
    private final File file;

    public CodexManager(MysticCraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "codex.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Set<LoreFragment> set = EnumSet.noneOf(LoreFragment.class);
                for (String name : yml.getStringList(key)) {
                    LoreFragment f = LoreFragment.fromString(name);
                    if (f != null) set.add(f);
                }
                discovered.put(uuid, set);
            } catch (IllegalArgumentException ignored) {
                // skip malformed key
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        discovered.forEach((uuid, set) ->
                yml.set(uuid.toString(), set.stream().map(Enum::name).toList()));
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save codex.yml", e);
        }
    }

    public Set<LoreFragment> getDiscovered(UUID uuid) {
        return discovered.computeIfAbsent(uuid, k -> EnumSet.noneOf(LoreFragment.class));
    }

    public boolean hasDiscovered(Player player, LoreFragment fragment) {
        return getDiscovered(player.getUniqueId()).contains(fragment);
    }

    public int discoveredCount(Player player) {
        return getDiscovered(player.getUniqueId()).size();
    }

    public int totalCount() {
        return LoreFragment.values().length;
    }

    /**
     * Grants a fragment. Returns false if they already had it, so callers can
     * fire discovery hooks freely without worrying about spam.
     */
    public boolean discover(Player player, LoreFragment fragment) {
        Set<LoreFragment> set = getDiscovered(player.getUniqueId());
        if (!set.add(fragment)) return false;
        save();

        com.canopycreations.mysticcraft.util.Fx.discovery(player);
        player.sendMessage("");
        player.sendMessage("§8✦ §7You've come to understand something. §8— " + fragment.getTitle());
        player.sendMessage("§8  Read it with §7/lore read " + fragment.name().toLowerCase());
        player.sendMessage("");

        // Deep-lore fragment unlocks once someone has nearly finished the codex.
        if (set.size() >= totalCount() - 2 && !set.contains(LoreFragment.THE_CURE)) {
            discover(player, LoreFragment.THE_CURE);
        }

        plugin.getQuestManager().onFragmentDiscovered(player);
        return true;
    }

    /** Fires every fragment matching a trigger. Safe to call repeatedly. */
    public void trigger(Player player, LoreFragment.Trigger trigger) {
        for (LoreFragment f : LoreFragment.values()) {
            if (f.getTrigger() == trigger) discover(player, f);
        }
    }
}
