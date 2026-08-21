package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.lore.LoreFragment;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

/**
 * Lorekeepers: persistent NPCs who carry the world's stories.
 *
 * Built on tagged, AI-disabled, invulnerable villagers rather than requiring
 * Citizens or any other NPC plugin - one less dependency to break, and these
 * survive restarts because the tag lives in the entity's own persistent data.
 *
 * Each keeper has a role that shapes what they'll tell you, and several will
 * only talk properly to certain races. A vampire and a human asking the same
 * old woman the same question get different answers, which is exactly the
 * dynamic a secrets-driven server runs on.
 */
public class LorekeeperManager {

    public enum Role {
        WANDERER("The Wanderer", "§7", null,
                "Knows a little about everything and won't say how.",
                List.of(
                        "§7\"You've seen the tracks too, then. Bigger than a bear and the wrong shape.\"",
                        "§7\"People here stopped going out after dark about forty years ago. Nobody remembers deciding to.\"",
                        "§7\"There's a tree east of here older than the story about it. Go look at it sometime.\""
                ), LoreFragment.SOMETHING_IN_THE_DARK),

        THE_APOTHECARY("The Apothecary", "§a", Race.HUMAN,
                "Sells the herbs that keep people alive. Knows exactly what they're for.",
                List.of(
                        "§a\"Vervain. Keep it on you. I don't care if you believe me.\"",
                        "§a\"Wolfsbane's for a different problem. You'll know if you've got that one.\"",
                        "§a\"I've buried four people who told me they didn't need any of this.\""
                ), LoreFragment.THE_THIRST),

        THE_ELDER("The Elder", "§4", Race.VAMPIRE,
                "Has been dead a very long time and is bored of new vampires.",
                List.of(
                        "§4\"You're hungry. Everyone's hungry at the start. It gets quieter, not better.\"",
                        "§4\"The switch is real. Don't ask me how I know, and don't ask me to show you.\"",
                        "§4\"There's one older than all of us. If he ever notices you, leave.\""
                ), LoreFragment.THE_SWITCH),

        THE_PACKMOTHER("The Packmother", "§6", Race.WEREWOLF,
                "Remembers which bloodline is which, and what each one costs.",
                List.of(
                        "§6\"Seven knives. Seven lines. Yours is one of them whether you like it or not.\"",
                        "§6\"First moon's the worst. Not the pain - the not remembering.\"",
                        "§6\"You'll want a pack. Everyone says they don't. Everyone's wrong.\""
                ), LoreFragment.SEVEN_KNIVES),

        THE_HEDGEWITCH("The Hedgewitch", "§5", Race.WITCH,
                "Teaches the price before the power, which is why she has fewer students than she'd like.",
                List.of(
                        "§5\"Magic isn't yours. You're borrowing. Everything borrowed gets called in.\"",
                        "§5\"Cast in a circle and you'll do things you couldn't alone. Push it and the circle pays.\"",
                        "§5\"A woman made the first vampires trying to save her children. Remember that before you improvise.\""
                ), LoreFragment.THE_PRICE),

        THE_ARCHIVIST("The Archivist", "§e", null,
                "Has read everything and believes most of it.",
                List.of(
                        "§e\"There was a cure once. One dose. Never used, as far as anyone knows.\"",
                        "§e\"Three names sit under the three titles. Most people only ever learn the titles.\"",
                        "§e\"Bring me what you've pieced together and I'll tell you what you're missing.\""
                ), LoreFragment.THE_UNBURIED);

        private final String displayName;
        private final String colorCode;
        private final Race preferredRace; // null = speaks to anyone
        private final String description;
        private final List<String> dialogue;
        private final LoreFragment teaches;

        Role(String displayName, String colorCode, Race preferredRace, String description,
             List<String> dialogue, LoreFragment teaches) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.preferredRace = preferredRace;
            this.description = description;
            this.dialogue = dialogue;
            this.teaches = teaches;
        }

        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
        public Race getPreferredRace() { return preferredRace; }
        public String getDescription() { return description; }
        public List<String> getDialogue() { return dialogue; }
        public LoreFragment getTeaches() { return teaches; }
        public String getFormattedName() { return colorCode + displayName; }

        public static Role fromString(String s) {
            if (s == null) return null;
            for (Role r : values()) {
                if (r.name().equalsIgnoreCase(s) || r.displayName.equalsIgnoreCase(s)) return r;
            }
            return null;
        }
    }

    private final MysticCraft plugin;
    private final NamespacedKey key;
    private final Random random = new Random();

    public LorekeeperManager(MysticCraft plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lorekeeper_role");
    }

    public NamespacedKey getKey() {
        return key;
    }

    /** Spawns a persistent, invulnerable, non-wandering keeper. */
    public Villager spawn(Location location, Role role) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setCustomName(role.getFormattedName());
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.getPersistentDataContainer().set(key, PersistentDataType.STRING, role.name());
        return villager;
    }

    public Role getRole(Entity entity) {
        if (entity == null) return null;
        String stored = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return Role.fromString(stored);
    }

    public boolean isLorekeeper(Entity entity) {
        return getRole(entity) != null;
    }

    /**
     * Handles a player interacting with a keeper. Keepers who serve a
     * particular race give a colder reception to everyone else - and that
     * refusal is itself informative, which is the point.
     */
    public void speakTo(Player player, Role role) {
        Race playerRace = plugin.getRaceManager().getRace(player);
        boolean welcome = role.getPreferredRace() == null || role.getPreferredRace() == playerRace;

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.7f, 1.0f);
        player.sendMessage("");
        player.sendMessage(role.getFormattedName() + " §8— " + role.getDescription());
        player.sendMessage("");

        if (!welcome) {
            player.sendMessage("§8They look at you for a long moment and decide against it.");
            player.sendMessage("§8\"...no. Not for you, I don't think.\"");
            player.sendMessage("");
            player.sendMessage("§8Someone else might get a different answer out of them.");
            player.sendMessage("");
            return;
        }

        player.sendMessage(role.getDialogue().get(random.nextInt(role.getDialogue().size())));
        player.sendMessage("");

        if (role.getTeaches() != null) {
            plugin.getCodexManager().discover(player, role.getTeaches());
        }

        // The Archivist points you at what you're missing, rather than handing it over.
        if (role == Role.THE_ARCHIVIST) {
            int have = plugin.getCodexManager().discoveredCount(player);
            int total = plugin.getCodexManager().totalCount();
            player.sendMessage("§e\"You've got " + have + " of " + total + " pieces. "
                    + (have >= total ? "That's all of it. Very few manage that.\""
                    : "Keep going - the rest are out there being lived through by somebody.\""));
            player.sendMessage("");
        }
    }
}
