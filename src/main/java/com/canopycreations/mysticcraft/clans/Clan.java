package com.canopycreations.mysticcraft.clans;

import com.canopycreations.mysticcraft.races.Race;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A clan is MysticCraft's native faction. Unlike a generic factions plugin,
 * a clan knows what it IS: a vampire Court, a werewolf Pack, a witch Coven,
 * or a human Order. That lets territory and race mechanics actually interact
 * (a Coven's ground weakens vampires; a Pack's ground is dangerous on a full
 * moon) instead of being two unrelated systems bolted together.
 */
public class Clan {

    public enum Kind {
        COURT("Court", Race.VAMPIRE, "§4"),
        PACK("Pack", Race.WEREWOLF, "§6"),
        COVEN("Coven", Race.WITCH, "§5"),
        ORDER("Order", Race.HUMAN, "§f"),
        MIXED("Circle", null, "§7"); // open to any race - for cross-race alliances and secret societies

        private final String label;
        private final Race requiredRace; // null = open to all
        private final String colorCode;

        Kind(String label, Race requiredRace, String colorCode) {
            this.label = label;
            this.requiredRace = requiredRace;
            this.colorCode = colorCode;
        }

        public String getLabel() { return label; }
        public Race getRequiredRace() { return requiredRace; }
        public String getColorCode() { return colorCode; }

        public static Kind fromString(String s) {
            if (s == null) return null;
            for (Kind k : values()) {
                if (k.name().equalsIgnoreCase(s) || k.label.equalsIgnoreCase(s)) return k;
            }
            return null;
        }
    }

    private final String name;
    private Kind kind;
    private UUID leader;
    private String description = "";
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> elders = new HashSet<>();   // officers
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();
    private final Set<String> claimedChunks = new LinkedHashSet<>(); // "world:x:z"
    private double power = 0.0;
    private long createdAt = System.currentTimeMillis();
    private boolean secret = false; // secret societies don't appear in /clan list

    public Clan(String name, Kind kind, UUID leader) {
        this.name = name;
        this.kind = kind;
        this.leader = leader;
        this.members.add(leader);
    }

    public String getName() { return name; }

    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<UUID> getMembers() { return members; }
    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); elders.remove(uuid); }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public int getMemberCount() { return members.size(); }

    public Set<UUID> getElders() { return elders; }
    public void addElder(UUID uuid) { if (members.contains(uuid)) elders.add(uuid); }
    public void removeElder(UUID uuid) { elders.remove(uuid); }
    public boolean isElder(UUID uuid) { return elders.contains(uuid) || isLeader(uuid); }

    public Set<String> getAllies() { return allies; }
    public Set<String> getEnemies() { return enemies; }
    public boolean isAlly(String clanName) { return allies.contains(clanName.toLowerCase()); }
    public boolean isEnemy(String clanName) { return enemies.contains(clanName.toLowerCase()); }

    public Set<String> getClaimedChunks() { return claimedChunks; }
    public int getClaimCount() { return claimedChunks.size(); }

    public double getPower() { return power; }
    public void setPower(double power) { this.power = Math.max(0, power); }
    public void addPower(double amount) { setPower(this.power + amount); }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isSecret() { return secret; }
    public void setSecret(boolean secret) { this.secret = secret; }

    /** Claim limit scales with membership - bigger clans hold more ground. */
    public int getClaimLimit(int base, int perMember) {
        return base + (perMember * members.size());
    }

    public String getFormattedName() {
        return kind.getColorCode() + name;
    }

    public List<String> serializeMembers() {
        List<String> out = new ArrayList<>();
        members.forEach(u -> out.add(u.toString()));
        return out;
    }
}
