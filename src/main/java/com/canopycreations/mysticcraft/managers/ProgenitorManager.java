package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.lore.Progenitor;
import com.canopycreations.mysticcraft.lore.ProgenitorReservations;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Tracks who holds each of the three unique Progenitor titles. Each can only
 * ever be held by one player at a time, and claiming is deliberately rare -
 * these are meant to be server-defining events, not routine progression.
 */
public class ProgenitorManager {

    private final MysticCraft plugin;
    private final Map<Progenitor, UUID> holders = new EnumMap<>(Progenitor.class);
    private final File file;
    private final ProgenitorReservations reservations;

    public ProgenitorManager(MysticCraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "progenitors.yml");
        this.reservations = new ProgenitorReservations(plugin);
        load();
    }

    public ProgenitorReservations getReservations() {
        return reservations;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (Progenitor p : Progenitor.values()) {
            String id = yml.getString(p.name(), null);
            if (id == null) continue;
            try {
                holders.put(p, UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        holders.forEach((p, uuid) -> yml.set(p.name(), uuid.toString()));
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save progenitors.yml", e);
        }
    }

    public boolean isClaimed(Progenitor progenitor) {
        return holders.containsKey(progenitor);
    }

    public UUID getHolder(Progenitor progenitor) {
        return holders.get(progenitor);
    }

    public Progenitor getProgenitorOf(UUID uuid) {
        for (Map.Entry<Progenitor, UUID> e : holders.entrySet()) {
            if (e.getValue().equals(uuid)) return e.getKey();
        }
        return null;
    }

    public boolean isProgenitor(Player player) {
        return getProgenitorOf(player.getUniqueId()) != null;
    }

    /**
     * Claims a progenitor title for a player, if unclaimed. Announces it with
     * appropriate weight - these should feel like world events.
     */
    public boolean claim(Player player, Progenitor progenitor) {
        if (isClaimed(progenitor)) return false;
        if (isProgenitor(player)) return false; // one title per player, ever

        // A reserved title can only be taken by the player it was written for.
        if (!reservations.mayClaim(player, progenitor)) {
            player.sendMessage(reservations.blockedMessage(progenitor));
            return false;
        }

        boolean namedHeir = reservations.isNamedHeir(player, progenitor);

        holders.put(progenitor, player.getUniqueId());
        save();

        com.canopycreations.mysticcraft.util.Fx.progenitorAwakening(player);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§m                                                        ");
        if (namedHeir) {
            Bukkit.broadcastMessage(progenitor.getFullHistoricName() + " §7walks again.");
            Bukkit.broadcastMessage("§f" + player.getName() + " §7has taken up " + progenitor.getFormattedTitle() + "§7.");
            Bukkit.broadcastMessage("§8The name is older than the server. It was always going to be them.");
        } else {
            Bukkit.broadcastMessage(progenitor.getFormattedTitle() + " §7has awakened.");
            Bukkit.broadcastMessage("§f" + player.getName() + " §7now carries a title that will not be given again.");
        }
        Bukkit.broadcastMessage("§8§m                                                        ");
        Bukkit.broadcastMessage("");

        if (namedHeir) {
            for (String line : reservations.recognitionLines(player, progenitor)) {
                player.sendMessage(line);
            }
        }

        player.sendMessage("");
        player.sendMessage(progenitor.getLore());
        player.sendMessage("");
        player.sendMessage("§aYour powers: §7" + progenitor.getPowers());
        player.sendMessage("§cYour weaknesses: §7" + progenitor.getWeaknesses());
        player.sendMessage("");

        plugin.getLogger().info("[Progenitor] " + player.getName() + " claimed " + progenitor.getTitle()
                + (namedHeir ? " (named heir - " + progenitor.getHistoricName() + ")" : ""));
        return true;
    }

    /** Strips a title (admin use, or lore events like the White Oak being destroyed). */
    public boolean strip(Progenitor progenitor, String reason) {
        UUID uuid = holders.remove(progenitor);
        if (uuid == null) return false;
        save();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§4You are no longer " + progenitor.getFormattedTitle() + "§4. " + reason);
        }
        Bukkit.broadcastMessage("§8" + progenitor.getFormattedTitle() + " §8is no longer held. " + reason);
        return true;
    }

    public void sendStatus(Player viewer) {
        viewer.sendMessage("§8§m                                                        ");
        viewer.sendMessage("§f§lThe Progenitors");
        for (Progenitor p : Progenitor.values()) {
            UUID uuid = holders.get(p);
            viewer.sendMessage("");
            viewer.sendMessage(p.getFormattedTitle());
            viewer.sendMessage("  " + p.getFullHistoricName());
            if (uuid != null) {
                viewer.sendMessage("  §7Held by §f" + Bukkit.getOfflinePlayer(uuid).getName());
            } else {
                viewer.sendMessage("  " + reservations.describeReservation(p));
            }
        }
        viewer.sendMessage("");
        viewer.sendMessage("§8§m                                                        ");
    }
}
