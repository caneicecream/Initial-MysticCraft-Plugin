package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The hidden werewolf bloodline.
 *
 * A small percentage of players carry the werewolf gene from the moment they
 * first join, but are told nothing - /race reports them as HUMAN, because as
 * far as they know, they are. The curse only manifests the first time they
 * kill another player (see CurseTriggerListener).
 *
 * The tell: triggered werewolves who spend enough continuous time near a
 * carrier start catching a scent. The messages are deliberately vague and
 * never name the player - a werewolf knows *someone* nearby is like them,
 * but has to work out who through roleplay, observation, and elimination.
 * That ambiguity is the point; it's what turns a hidden stat into a
 * social mystery.
 */
public class BloodlineManager {

    private final MysticCraft plugin;
    private final Random random = new Random();

    /** carrier uuid -> (sniffer uuid -> accumulated seconds in proximity) */
    private final Map<UUID, Map<UUID, Integer>> proximitySeconds = new HashMap<>();
    /** sniffer uuid -> last time we sent them a scent hint (millis), to avoid spam */
    private final Map<UUID, Long> lastHintMillis = new HashMap<>();

    private static final List<String> SCENT_HINTS = List.of(
            "§8You catch something on the air. Familiar. Wrong.",
            "§8Something nearby smells like the woods after a storm. You can't place it.",
            "§8The hair on your neck rises. Someone here isn't only what they seem.",
            "§8A scent you half-recognize - like looking in a mirror and not seeing yourself.",
            "§8Your instincts prickle. There's blood close by that runs like yours."
    );

    public BloodlineManager(MysticCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Rolls once, ever, for a given player - on their first join. Silent by
     * design: a carrier is told nothing and shows as HUMAN everywhere.
     */
    public void rollIfNeeded(Player player) {
        PlayerData data = plugin.getRaceManager().getData(player);
        if (data.isGeneRollDone()) return;

        data.setGeneRollDone(true);

        // Only humans can carry it latently - anyone already turned is skipped.
        if (data.getRace() == Race.HUMAN) {
            double chance = plugin.getConfig().getDouble("werewolf.latent-gene.chance-percent", 8.0);
            if (random.nextDouble() * 100.0 < chance) {
                data.setLatentWolfGene(true);
                plugin.getLogger().info("[Bloodline] " + player.getName()
                        + " has been seeded with the latent werewolf gene (they were not told).");
            }
        }

        plugin.getDataStore().save(data);
    }

    /**
     * Called on a slow timer. Accumulates proximity time between triggered
     * werewolves and latent carriers, and occasionally sends the werewolf a
     * vague hint once they've been close long enough.
     */
    public void tickScent() {
        if (!plugin.getConfig().getBoolean("werewolf.latent-gene.scent-detection-enabled", true)) return;

        double radius = plugin.getConfig().getDouble("werewolf.latent-gene.scent-radius", 12.0);
        int requiredSeconds = plugin.getConfig().getInt("werewolf.latent-gene.scent-seconds-required", 120);
        long hintCooldownMillis = plugin.getConfig().getInt("werewolf.latent-gene.scent-hint-cooldown-seconds", 300) * 1000L;

        for (Player sniffer : Bukkit.getOnlinePlayers()) {
            PlayerData snifferData = plugin.getRaceManager().getData(sniffer);
            // Only a werewolf who has actually triggered their curse can smell it.
            if (snifferData.getRace() != Race.WEREWOLF || !snifferData.hasTriggeredCurse()) continue;

            for (Player nearby : Bukkit.getOnlinePlayers()) {
                if (nearby.equals(sniffer)) continue;
                if (!nearby.getWorld().equals(sniffer.getWorld())) continue;
                if (nearby.getLocation().distance(sniffer.getLocation()) > radius) continue;

                PlayerData nearbyData = plugin.getRaceManager().getData(nearby);
                boolean isCarrier = nearbyData.hasLatentWolfGene()
                        && nearbyData.getRace() == Race.HUMAN
                        && !nearbyData.hasTriggeredCurse();
                if (!isCarrier) continue;

                Map<UUID, Integer> perSniffer = proximitySeconds
                        .computeIfAbsent(nearby.getUniqueId(), k -> new HashMap<>());
                int seconds = perSniffer.merge(sniffer.getUniqueId(), 5, Integer::sum); // tick runs every 5s

                if (seconds < requiredSeconds) continue;

                long last = lastHintMillis.getOrDefault(sniffer.getUniqueId(), 0L);
                if (System.currentTimeMillis() - last < hintCooldownMillis) continue;

                lastHintMillis.put(sniffer.getUniqueId(), System.currentTimeMillis());
                sniffer.sendMessage(SCENT_HINTS.get(random.nextInt(SCENT_HINTS.size())));
            }
        }
    }

    /** Clears tracking for a player who logs off, so timers don't persist stale. */
    public void clear(UUID uuid) {
        proximitySeconds.remove(uuid);
        lastHintMillis.remove(uuid);
        proximitySeconds.values().forEach(m -> m.remove(uuid));
    }

    /**
     * Admin/diagnostic helper - deliberately NOT exposed to normal players,
     * since the whole mechanic depends on carriers not knowing.
     */
    public boolean isCarrier(Player player) {
        PlayerData data = plugin.getRaceManager().getData(player);
        return data.hasLatentWolfGene();
    }
}
