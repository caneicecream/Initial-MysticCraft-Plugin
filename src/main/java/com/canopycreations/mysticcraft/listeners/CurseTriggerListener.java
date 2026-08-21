package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.lore.Bloodline;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * The werewolf curse only activates when its carrier takes a human life.
 *
 * Two paths lead here:
 *   1. A latent carrier (seeded silently on first join by BloodlineManager,
 *      and shown as HUMAN the entire time) kills a player - this is the
 *      surprise reveal, and the moment they learn what they are.
 *   2. A player who already knows they're a WEREWOLF but hasn't triggered
 *      yet kills a player - a confirmation rather than a reveal.
 */
public class CurseTriggerListener implements Listener {

    private final MysticCraft plugin;

    public CurseTriggerListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        PlayerData killerData = plugin.getRaceManager().getData(killer);
        if (killerData.hasTriggeredCurse()) return;

        // Path 1: latent carrier who had no idea.
        if (killerData.getRace() == Race.HUMAN && killerData.hasLatentWolfGene()) {
            revealLatentCurse(killer, killerData);
            return;
        }

        // Path 2: known werewolf, curse not yet triggered.
        if (killerData.getRace() == Race.WEREWOLF) {
            killerData.setTriggeredCurse(true);
            Bloodline line = assignBloodline(killerData);
            plugin.getDataStore().save(killerData);
            killer.sendMessage("\u00a76\u00a7lSomething inside you has changed. The curse is triggered - the wolf is awake now.");
            killer.sendMessage("\u00a77Your blood runs from the " + line.getFormattedName() + " \u00a77line. \u00a78(/lore bloodlines)");
            Bukkit.broadcastMessage("\u00a76" + killer.getName() + " \u00a77has triggered the werewolf curse...");
        }
    }

    /** Assigns a bloodline if the player doesn't have one, weighted so rare lines stay rare. */
    private Bloodline assignBloodline(PlayerData data) {
        if (data.getBloodline() != null) return data.getBloodline();

        // Weighted draw - Riverborne is common, Hollow-Born and Emberfell are rare.
        java.util.List<Bloodline> pool = new java.util.ArrayList<>();
        java.util.Map<Bloodline, Integer> weights = java.util.Map.of(
                Bloodline.RIVERBORNE, 30,
                Bloodline.CRESCENT, 20,
                Bloodline.STONE_RIDGE, 18,
                Bloodline.ASHWOOD, 15,
                Bloodline.NORTH_ATLANTIC, 10,
                Bloodline.EMBERFELL, 5,
                Bloodline.HOLLOW_BORN, 2
        );
        weights.forEach((b, w) -> { for (int i = 0; i < w; i++) pool.add(b); });
        Bloodline chosen = pool.get(new java.util.Random().nextInt(pool.size()));
        data.setBloodline(chosen);
        return chosen;
    }

    private void revealLatentCurse(Player killer, PlayerData data) {
        data.setLatentWolfGene(false);
        data.setTriggeredCurse(true);
        Bloodline line = assignBloodline(data);
        plugin.getRaceManager().setRace(killer, Race.WEREWOLF, false);
        plugin.getDataStore().save(data);

        // A disorienting, physical reveal - they didn't choose this and didn't see it coming.
        killer.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 1));
        killer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
        killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.2f, 0.6f);

        killer.sendMessage("");
        killer.sendMessage("\u00a74\u00a7lSomething tears loose inside you.");
        killer.sendMessage("\u00a77Your bones ache. Your vision swims. The blood on your hands smells");
        killer.sendMessage("\u00a77like something you've been hungry for your whole life without knowing it.");
        killer.sendMessage("");
        killer.sendMessage("\u00a76\u00a7lYou are a werewolf. \u00a77You always were - it just took a death to wake it.");
        killer.sendMessage("\u00a77Your blood runs from the " + line.getFormattedName() + " \u00a77line. \u00a78(/lore bloodlines)");
        killer.sendMessage("\u00a77The next full moon will not ask your permission. \u00a78(/werewolf info)");
        killer.sendMessage("");

        Bukkit.broadcastMessage("\u00a76" + killer.getName() + " \u00a77has triggered a curse they never knew they carried...");
        plugin.getLogger().info("[Bloodline] " + killer.getName() + " triggered their latent werewolf curse by killing another player.");
    }
}
