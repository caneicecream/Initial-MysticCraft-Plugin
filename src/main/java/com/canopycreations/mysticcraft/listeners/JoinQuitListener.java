package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private final MysticCraft plugin;

    public JoinQuitListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Silently rolls the latent werewolf gene on a player's very first join.
        // Deliberately says nothing - a carrier is not supposed to know.
        plugin.getBloodlineManager().rollIfNeeded(event.getPlayer());

        // Loads (or creates) their PlayerData and applies race-appropriate attributes.
        plugin.getRaceManager().refreshAttributes(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBloodlineManager().clear(event.getPlayer().getUniqueId());
        plugin.getRaceManager().unload(event.getPlayer());
    }
}
