package com.canopycreations.mysticcraft.events;

import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired after RaceManager finishes changing a player's race. Kept separate
 * from RaceManager itself so integrations (Factions bridge, scoreboard
 * teams, external plugins) can hook in without RaceManager needing to know
 * they exist.
 */
public class RaceChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Race previousRace;
    private final Race newRace;

    public RaceChangeEvent(Player player, Race previousRace, Race newRace) {
        this.player = player;
        this.previousRace = previousRace;
        this.newRace = newRace;
    }

    public Player getPlayer() {
        return player;
    }

    public Race getPreviousRace() {
        return previousRace;
    }

    public Race getNewRace() {
        return newRace;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
