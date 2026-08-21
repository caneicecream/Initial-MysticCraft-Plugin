package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * TVD-accurate vampire turning: a human dying with vampire blood still in
 * their system rises as a vampire mid-transition, and must feed on human
 * blood within a deadline to complete it - or dies for real. This is the
 * only way to become a vampire; there is no self-service race switch.
 */
public class TurningListener implements Listener {

    private final MysticCraft plugin;

    public TurningListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        PlayerData data = plugin.getRaceManager().getData(victim);

        if (data.getRace() != Race.HUMAN) return;
        if (!data.hasVampireBloodInSystem()) return;
        if (System.currentTimeMillis() > data.getVampireBloodExpiresAtMillis()) {
            // Blood had already worn off before they died - no transition.
            data.setHasVampireBloodInSystem(false);
            plugin.getDataStore().save(data);
            return;
        }

        data.setHasVampireBloodInSystem(false);
        data.setPendingTransition(true);
        plugin.getDataStore().save(data);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getRaceManager().getData(player);
        if (!data.isPendingTransition()) return;

        data.setPendingTransition(false);
        plugin.getRaceManager().setRace(player, Race.VAMPIRE, false);

        int windowMinutes = plugin.getConfig().getInt("vampire.turning.transition-window-minutes", 60);
        data.setTransitioning(true);
        data.setTransitionDeadlineMillis(System.currentTimeMillis() + windowMinutes * 60_000L);
        plugin.getDataStore().save(data);

        player.sendMessage("§4§lYour heart has stopped.");
        player.sendMessage("§4A hunger unlike anything you've felt is rising. You must feed on human blood within "
                + windowMinutes + " minutes - or this is permanent, and not in the way you'd hope.");
        Bukkit.broadcastMessage("§4" + player.getName() + " §7has died... and risen changed.");
    }

    /** Called once per second from MysticCraft's tick task. */
    public void tickTransitionDeadline(Player player, PlayerData data) {
        if (!data.isTransitioning()) return;
        if (System.currentTimeMillis() <= data.getTransitionDeadlineMillis()) return;

        data.setTransitioning(false);
        plugin.getRaceManager().setRace(player, Race.HUMAN, false);
        plugin.getDataStore().save(data);

        player.sendMessage("§4§lThe transition has failed. You couldn't complete it in time.");
        player.setHealth(0);
        Bukkit.broadcastMessage("§7" + player.getName() + "'s transition §4§lfailed§7 - the change didn't take.");
    }
}
