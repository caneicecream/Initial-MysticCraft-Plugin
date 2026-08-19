package com.canopycreations.mysticcraft.integrations.factions;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.events.RaceChangeEvent;
import com.canopycreations.mysticcraft.races.Race;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Bridges MysticCraft races to SaberFactions faction membership.
 *
 * IMPORTANT FOR WHOEVER MAINTAINS THIS: this is written against the classic
 * com.massivecraft.factions API lineage (FPlayer / FPlayers / Faction /
 * Factions / FPlayerJoinEvent), which SaberFactions confirms it kept in its
 * own repo. That API has been broadly stable since ~2014, but SaberFactions
 * has changed hands a few times (TeamPixel -> SaberLLC -> Driftay's fork on
 * JitPack), so if this fails to compile against the exact jar you're
 * running, the most likely culprits are:
 *   - FPlayers.getInstance().getByPlayer(player)  -> confirm exact method name
 *   - fplayer.setFaction(faction) / fplayer.getFaction()
 *   - Factions.getInstance().getByTag(tag)
 *   - FPlayerJoinEvent's getFPlayer()/getFaction() accessors
 * All of those are small, mechanical fixes - the surrounding logic doesn't
 * change. This class is only ever instantiated if the "Factions" plugin is
 * actually detected on the server (see MysticCraft#onEnable), so a missing
 * or incompatible Factions install never crashes the rest of MysticCraft.
 */
public class FactionsBridge implements Listener {

    private final MysticCraft plugin;
    private final Map<Race, String> raceToFactionTag = new EnumMap<>(Race.class);
    private final Map<String, Race> factionTagToRace = new HashMap<>();

    public FactionsBridge(MysticCraft plugin) {
        this.plugin = plugin;
        loadMappings();
    }

    private void loadMappings() {
        raceToFactionTag.clear();
        factionTagToRace.clear();
        for (Race race : Race.values()) {
            if (race == Race.HUMAN) continue;
            String tag = plugin.getConfig().getString("factions-bridge.race-factions." + race.name(), null);
            if (tag != null && !tag.isBlank()) {
                raceToFactionTag.put(race, tag);
                factionTagToRace.put(tag.toLowerCase(), race);
            }
        }
    }

    public void reload() {
        loadMappings();
    }

    // ---------------------------------------------------------------
    // Auto-assign faction membership when a player's race changes
    // ---------------------------------------------------------------
    @EventHandler
    public void onRaceChange(RaceChangeEvent event) {
        if (!plugin.getConfig().getBoolean("factions-bridge.auto-assign-faction-on-turn", true)) return;

        String targetTag = raceToFactionTag.get(event.getNewRace());
        if (targetTag == null) return; // no faction configured for this race (or they turned Human)

        try {
            Faction target = Factions.getInstance().getByTag(targetTag);
            if (target == null) {
                plugin.getLogger().warning("[FactionsBridge] Configured faction '" + targetTag
                        + "' for race " + event.getNewRace() + " doesn't exist yet - create it with /f create "
                        + targetTag + " first.");
                return;
            }
            FPlayer fplayer = FPlayers.getInstance().getByPlayer(event.getPlayer());
            if (fplayer == null) return;

            // Second argument varies by SaberFactions build (commonly a "silent"/"auto" flag).
            // false = behave like a normal join (fires the usual join messaging/side-effects).
            fplayer.setFaction(target, false);
            event.getPlayer().sendMessage("§7[§dMysticCraft§7] You've been moved into the §f" + target.getTag()
                    + "§7 faction to match your new race.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[FactionsBridge] Failed to auto-assign faction - "
                    + "the installed Factions build may use a slightly different API. See FactionsBridge.java comments.", e);
        }
    }

    // ---------------------------------------------------------------
    // Race-locked factions: block /f join into a race-restricted faction
    // if the joining player's MysticCraft race doesn't match
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onFactionJoin(FPlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("factions-bridge.lock-faction-membership-by-race", true)) return;

        try {
            String tag = event.getFaction().getTag();
            Race requiredRace = factionTagToRace.get(tag.toLowerCase());
            if (requiredRace == null) return; // not a race-locked faction

            // FPlayerJoinEvent's accessor for the joining FPlayer varies across SaberFactions
            // builds (getFPlayer/getFplayer/etc.), but FPlayer instances are guaranteed to be
            // the same object per player (docs: "you can use the == operator"), so we can
            // resolve the Bukkit Player by matching identity against online players instead
            // of relying on a specific accessor name.
            Player player = resolvePlayerFromJoinEvent(event);
            if (player == null) return;

            Race playerRace = plugin.getRaceManager().getRace(player);
            if (playerRace != requiredRace) {
                event.setCancelled(true);
                player.sendMessage("§cOnly " + requiredRace.getFormattedName() + "§c players can join "
                        + tag + ". Use /race set " + requiredRace.name().toLowerCase() + " first (if you're eligible).");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[FactionsBridge] Failed to enforce race lock on faction join - "
                    + "the installed Factions build may use a slightly different API.", e);
        }
    }

    /**
     * Tries a handful of known getter-name variants for pulling the FPlayer off
     * FPlayerJoinEvent, then matches it against online players by identity
     * (FPlayer instances are 1-per-player and stable, per the classic API docs).
     * This avoids the whole bridge breaking again if this build renamed the
     * accessor yet again.
     */
    private Player resolvePlayerFromJoinEvent(FPlayerJoinEvent event) {
        FPlayer joiningFPlayer = null;
        for (String methodName : new String[]{"getFPlayer", "getFplayer", "getPlayer", "getFPlayerJoining"}) {
            try {
                java.lang.reflect.Method m = event.getClass().getMethod(methodName);
                Object result = m.invoke(event);
                if (result instanceof FPlayer fp) {
                    joiningFPlayer = fp;
                    break;
                }
            } catch (Exception ignored) {
                // try the next candidate name
            }
        }
        if (joiningFPlayer == null) {
            try {
                java.lang.reflect.Field f = event.getClass().getDeclaredField("fplayer");
                f.setAccessible(true);
                Object result = f.get(event);
                if (result instanceof FPlayer fp) joiningFPlayer = fp;
            } catch (Exception ignored) {
                // give up - bridge just won't enforce this particular check
            }
        }
        if (joiningFPlayer == null) return null;

        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (FPlayers.getInstance().getByPlayer(candidate) == joiningFPlayer) {
                return candidate;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Faction-wide race majority buff - called periodically from MysticCraft's tick loop
    // ---------------------------------------------------------------
    public void tickMajorityBonus() {
        if (!plugin.getConfig().getBoolean("factions-bridge.majority-bonus.enabled", true)) return;

        try {
            // Group online players by their Faction tag using only confirmed-stable
            // FPlayer methods (avoids relying on Faction#getFPlayers, which varies more across forks).
            Map<String, Map<Race, Integer>> tagToRaceCounts = new HashMap<>();
            Map<String, java.util.List<Player>> tagToPlayers = new HashMap<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
                if (fplayer == null || fplayer.getFaction() == null) continue;
                String tag = fplayer.getFaction().getTag();
                Race race = plugin.getRaceManager().getRace(player);
                tagToRaceCounts.computeIfAbsent(tag, k -> new EnumMap<>(Race.class)).merge(race, 1, Integer::sum);
                tagToPlayers.computeIfAbsent(tag, k -> new java.util.ArrayList<>()).add(player);
            }

            double threshold = plugin.getConfig().getDouble("factions-bridge.majority-bonus.threshold-percent", 70) / 100.0;

            for (Map.Entry<String, Map<Race, Integer>> entry : tagToRaceCounts.entrySet()) {
                Map<Race, Integer> counts = entry.getValue();
                int total = counts.values().stream().mapToInt(Integer::intValue).sum();
                if (total == 0) continue;

                Race majorityRace = counts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
                if (majorityRace == null || majorityRace == Race.HUMAN) continue;

                double fraction = counts.get(majorityRace) / (double) total;
                if (fraction < threshold) continue;

                for (Player player : tagToPlayers.get(entry.getKey())) {
                    if (plugin.getRaceManager().getRace(player) != majorityRace) continue;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 0, true, false));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[FactionsBridge] Failed to compute majority bonus - "
                    + "the installed Factions build may use a slightly different API.", e);
        }
    }
}
