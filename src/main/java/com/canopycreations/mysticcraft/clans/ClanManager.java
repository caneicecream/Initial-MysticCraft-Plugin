package com.canopycreations.mysticcraft.clans;

import com.canopycreations.mysticcraft.MysticCraft;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ClanManager {

    private final MysticCraft plugin;
    private final Map<String, Clan> clans = new HashMap<>();        // lowercase name -> clan
    private final Map<UUID, String> playerClans = new HashMap<>();  // player -> lowercase clan name
    private final Map<String, String> chunkOwners = new HashMap<>(); // "world:x:z" -> lowercase clan name
    private final File file;

    public ClanManager(MysticCraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clans.yml");
        load();
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------
    public void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("clans");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            try {
                String name = s.getString("name", key);
                Clan.Kind kind = Clan.Kind.fromString(s.getString("kind", "MIXED"));
                UUID leader = UUID.fromString(s.getString("leader"));
                Clan clan = new Clan(name, kind == null ? Clan.Kind.MIXED : kind, leader);
                clan.setDescription(s.getString("description", ""));
                clan.setPower(s.getDouble("power", 0.0));
                clan.setCreatedAt(s.getLong("createdAt", System.currentTimeMillis()));
                clan.setSecret(s.getBoolean("secret", false));

                for (String m : s.getStringList("members")) {
                    try { clan.addMember(UUID.fromString(m)); } catch (IllegalArgumentException ignored) {}
                }
                for (String e : s.getStringList("elders")) {
                    try { clan.addElder(UUID.fromString(e)); } catch (IllegalArgumentException ignored) {}
                }
                clan.getAllies().addAll(s.getStringList("allies"));
                clan.getEnemies().addAll(s.getStringList("enemies"));
                clan.getClaimedChunks().addAll(s.getStringList("chunks"));

                clans.put(name.toLowerCase(Locale.ROOT), clan);
                clan.getMembers().forEach(u -> playerClans.put(u, name.toLowerCase(Locale.ROOT)));
                clan.getClaimedChunks().forEach(c -> chunkOwners.put(c, name.toLowerCase(Locale.ROOT)));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Skipping malformed clan entry: " + key, e);
            }
        }
        plugin.getLogger().info("Loaded " + clans.size() + " clans.");
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Clan clan : clans.values()) {
            String path = "clans." + clan.getName().toLowerCase(Locale.ROOT);
            yml.set(path + ".name", clan.getName());
            yml.set(path + ".kind", clan.getKind().name());
            yml.set(path + ".leader", clan.getLeader().toString());
            yml.set(path + ".description", clan.getDescription());
            yml.set(path + ".power", clan.getPower());
            yml.set(path + ".createdAt", clan.getCreatedAt());
            yml.set(path + ".secret", clan.isSecret());
            yml.set(path + ".members", clan.getMembers().stream().map(UUID::toString).toList());
            yml.set(path + ".elders", clan.getElders().stream().map(UUID::toString).toList());
            yml.set(path + ".allies", new ArrayList<>(clan.getAllies()));
            yml.set(path + ".enemies", new ArrayList<>(clan.getEnemies()));
            yml.set(path + ".chunks", new ArrayList<>(clan.getClaimedChunks()));
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save clans.yml", e);
        }
    }

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------
    public Clan getClan(String name) {
        return name == null ? null : clans.get(name.toLowerCase(Locale.ROOT));
    }

    public Clan getPlayerClan(UUID uuid) {
        String name = playerClans.get(uuid);
        return name == null ? null : clans.get(name);
    }

    public boolean exists(String name) {
        return name != null && clans.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Clan> getAllClans() {
        return clans.values();
    }

    public List<Clan> getVisibleClans() {
        List<Clan> out = new ArrayList<>();
        for (Clan c : clans.values()) {
            if (!c.isSecret()) out.add(c);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Membership
    // ------------------------------------------------------------------
    public Clan create(String name, Clan.Kind kind, UUID leader) {
        if (exists(name)) return null;
        Clan clan = new Clan(name, kind, leader);
        clans.put(name.toLowerCase(Locale.ROOT), clan);
        playerClans.put(leader, name.toLowerCase(Locale.ROOT));
        save();
        return clan;
    }

    public void addMember(Clan clan, UUID uuid) {
        Clan existing = getPlayerClan(uuid);
        if (existing != null) removeMember(existing, uuid);
        clan.addMember(uuid);
        playerClans.put(uuid, clan.getName().toLowerCase(Locale.ROOT));
        save();
    }

    public void removeMember(Clan clan, UUID uuid) {
        clan.removeMember(uuid);
        playerClans.remove(uuid);
        save();
    }

    public void disband(Clan clan) {
        clan.getMembers().forEach(playerClans::remove);
        clan.getClaimedChunks().forEach(chunkOwners::remove);
        clans.remove(clan.getName().toLowerCase(Locale.ROOT));
        save();
    }

    // ------------------------------------------------------------------
    // Territory
    // ------------------------------------------------------------------
    public static String chunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public Clan getChunkOwner(Chunk chunk) {
        String owner = chunkOwners.get(chunkKey(chunk));
        return owner == null ? null : clans.get(owner);
    }

    public boolean claimChunk(Clan clan, Chunk chunk) {
        String key = chunkKey(chunk);
        if (chunkOwners.containsKey(key)) return false;
        chunkOwners.put(key, clan.getName().toLowerCase(Locale.ROOT));
        clan.getClaimedChunks().add(key);
        save();
        plugin.refreshMap();
        return true;
    }

    public boolean unclaimChunk(Clan clan, Chunk chunk) {
        String key = chunkKey(chunk);
        if (!clan.getClaimedChunks().remove(key)) return false;
        chunkOwners.remove(key);
        save();
        plugin.refreshMap();
        return true;
    }
}
