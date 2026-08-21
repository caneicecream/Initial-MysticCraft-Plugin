package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.quests.Questline;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Questline progress. A player's line is determined by their race, so
 * turning mid-game hands you a new story rather than orphaning the old one -
 * a human who becomes a vampire stops being "The Ones Who Stayed" and starts
 * "The Newly Dead" at step one, which is thematically exactly right.
 */
public class QuestManager {

    private final MysticCraft plugin;
    /** uuid -> (questline name -> step index reached) */
    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    /** uuid -> (objective name -> accumulated count) */
    private final Map<UUID, Map<String, Integer>> counters = new HashMap<>();
    private final File file;

    public QuestManager(MysticCraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Map<String, Integer> steps = new HashMap<>();
                var sSec = yml.getConfigurationSection(key + ".steps");
                if (sSec != null) sSec.getKeys(false).forEach(k -> steps.put(k, sSec.getInt(k)));
                progress.put(uuid, steps);

                Map<String, Integer> counts = new HashMap<>();
                var cSec = yml.getConfigurationSection(key + ".counters");
                if (cSec != null) cSec.getKeys(false).forEach(k -> counts.put(k, cSec.getInt(k)));
                counters.put(uuid, counts);
            } catch (IllegalArgumentException ignored) {
                // skip malformed key
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        progress.forEach((uuid, steps) ->
                steps.forEach((q, i) -> yml.set(uuid + ".steps." + q, i)));
        counters.forEach((uuid, counts) ->
                counts.forEach((o, i) -> yml.set(uuid + ".counters." + o, i)));
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save quests.yml", e);
        }
    }

    public Questline getActiveLine(Player player) {
        return Questline.forRace(plugin.getRaceManager().getRace(player));
    }

    public int getStep(Player player, Questline line) {
        return progress.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(line.name(), 0);
    }

    private void setStep(Player player, Questline line, int step) {
        progress.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(line.name(), step);
        save();
    }

    public int getCounter(Player player, Questline.Objective objective) {
        return counters.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(objective.name(), 0);
    }

    private void setCounter(Player player, Questline.Objective objective, int value) {
        counters.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(objective.name(), value);
        save();
    }

    /**
     * The single entry point gameplay code calls. Increments an objective and
     * advances the player's line if that satisfies their current step.
     */
    public void progress(Player player, Questline.Objective objective, int amount) {
        Questline line = getActiveLine(player);
        if (line == null) return;

        int step = getStep(player, line);
        if (step >= line.getSteps().size()) return; // line already finished

        Questline.QuestStep current = line.getSteps().get(step);
        if (current.objective() != objective) return; // not what they're working on

        int updated = getCounter(player, objective) + amount;
        setCounter(player, objective, updated);

        if (updated < current.required()) {
            player.sendActionBar("§8" + current.name() + " §7" + updated + "§8/§7" + current.required());
            return;
        }

        completeStep(player, line, step, current);
    }

    /** Convenience for one-shot objectives. */
    public void progress(Player player, Questline.Objective objective) {
        progress(player, objective, 1);
    }

    private void completeStep(Player player, Questline line, int step, Questline.QuestStep current) {
        setStep(player, line, step + 1);

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 0.8f);
        player.sendMessage("");
        player.sendMessage("§8§m                                                ");
        player.sendMessage(line.getColorCode() + "§l" + current.name());
        player.sendMessage("");
        player.sendMessage(current.completionText());
        player.sendMessage("");

        int next = step + 1;
        if (next < line.getSteps().size()) {
            Questline.QuestStep upcoming = line.getSteps().get(next);
            player.sendMessage("§8Next: §7" + upcoming.instruction());
        } else {
            player.sendMessage(line.getColorCode() + "§lYou've reached the end of " + line.getTitle() + ".");
            player.sendMessage("§7What you do from here isn't written down anywhere.");
        }
        player.sendMessage("§8§m                                                ");
        player.sendMessage("");
    }

    /** Called by CodexManager when a fragment is found (drives the human line's first step). */
    public void onFragmentDiscovered(Player player) {
        progress(player, Questline.Objective.DISCOVER_FRAGMENTS, 1);
    }

    /** Shows a player where they are. */
    public void sendProgress(Player player) {
        Questline line = getActiveLine(player);
        if (line == null) {
            player.sendMessage("§7You have no story yet. §8Everyone starts as something.");
            return;
        }
        int step = getStep(player, line);

        player.sendMessage("§8§m                                                ");
        player.sendMessage(line.getFormattedTitle());
        player.sendMessage("§8" + line.getPremise());
        player.sendMessage("");

        for (int i = 0; i < line.getSteps().size(); i++) {
            Questline.QuestStep s = line.getSteps().get(i);
            if (i < step) {
                player.sendMessage("  §a✔ §8" + s.name());
            } else if (i == step) {
                int have = getCounter(player, s.objective());
                String count = s.required() > 1 ? " §8(" + have + "/" + s.required() + ")" : "";
                player.sendMessage("  §e▸ §f" + s.name() + count);
                player.sendMessage("      §7" + s.instruction());
            } else {
                player.sendMessage("  §8✧ ???");
            }
        }
        player.sendMessage("§8§m                                                ");
    }
}
