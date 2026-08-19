package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * TVD lore: a doppelganger-descended werewolf only becomes a "true" werewolf
 * the moment they cause a human death. Before that they're mechanically
 * human even if their race is set to WEREWOLF.
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
        if (killer == null) return;

        PlayerData killerData = plugin.getRaceManager().getData(killer);
        if (killerData.getRace() != Race.WEREWOLF || killerData.hasTriggeredCurse()) return;

        killerData.setTriggeredCurse(true);
        plugin.getDataStore().save(killerData);

        killer.sendMessage("§6§lSomething inside you has changed. You feel it stirring - the curse is triggered.");
        Bukkit.broadcastMessage("§6" + killer.getName() + " §7has triggered the werewolf curse...");
    }
}
