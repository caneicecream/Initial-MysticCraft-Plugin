package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.items.MysticItems;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * A strictly limited pool of "Original" vampires (default: 8) who receive a
 * free Daylight Ring the moment they become a true vampire - whether via
 * admin bootstrap (/mystic setrace) or by naturally completing a transition.
 * Once the pool is full, later vampires need a witch to forge them a ring
 * instead (see SpellManager's "forgering" spell).
 */
public class OriginalsManager {

    private final MysticCraft plugin;
    private final Set<UUID> originals = new LinkedHashSet<>();
    private final File file;

    public OriginalsManager(MysticCraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "originals.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        List<String> ids = yml.getStringList("originals");
        for (String id : ids) {
            try {
                originals.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries rather than fail the whole load
            }
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("originals", originals.stream().map(UUID::toString).toList());
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save originals.yml", e);
        }
    }

    public int slotCount() {
        return plugin.getConfig().getInt("vampire.originals.slot-count", 8);
    }

    public int remainingSlots() {
        return Math.max(0, slotCount() - originals.size());
    }

    public boolean isOriginal(UUID uuid) {
        return originals.contains(uuid);
    }

    /**
     * Attempts to claim an Original slot for this player. If a slot is
     * available, marks them as an Original, hands them a free Daylight
     * Ring, persists the change, and announces it server-wide. Safe to
     * call multiple times for the same player (won't double-grant).
     *
     * @return true if this player is (now, or already was) an Original.
     */
    public boolean tryClaimOriginal(Player player) {
        if (originals.contains(player.getUniqueId())) {
            return true;
        }
        if (remainingSlots() <= 0) {
            return false;
        }

        originals.add(player.getUniqueId());
        save();

        MysticItems items = plugin.getMysticItems();
        player.getInventory().addItem(items.daylightRing());

        var data = plugin.getRaceManager().getData(player);
        data.setOriginalVampire(true);
        plugin.getDataStore().save(data);

        int remaining = remainingSlots();
        player.sendMessage("§6§lYou are one of the Originals. §7A Daylight Ring has been placed in your inventory.");
        Bukkit.broadcastMessage("§6" + player.getName() + " §7has awakened as one of the §6§lOriginal Vampires§7! "
                + (remaining > 0 ? "§7Only §6" + remaining + "§7 spot" + (remaining == 1 ? "" : "s") + " remain."
                : "§7All Original spots are now claimed."));
        return true;
    }
}
