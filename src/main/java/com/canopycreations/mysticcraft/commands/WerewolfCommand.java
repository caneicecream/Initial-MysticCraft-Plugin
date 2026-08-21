package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WerewolfCommand implements CommandExecutor {

    private final MysticCraft plugin;

    public WerewolfCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        PlayerData data = plugin.getRaceManager().getData(player);
        if (data.getRace() != Race.WEREWOLF) {
            player.sendMessage("§cYou aren't a werewolf.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§7Usage: /werewolf <shift|info>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "shift" -> shift(player, data);
            case "info" -> info(player, data);
            default -> player.sendMessage("§7Usage: /werewolf <shift|info>");
        }
        return true;
    }

    private void shift(Player player, PlayerData data) {
        player.sendMessage("§8You don't call the wolf with words.");
        player.sendMessage("§7Crouch under a night sky, look straight up, and hold it.");
        player.sendMessage("§8They'll hear you for a hundred blocks.");
        player.sendMessage("");
        if (!data.hasTriggeredCurse()) {
            player.sendMessage("§6You haven't triggered the curse yet. Until you take a human life, the wolf stays dormant.");
            return;
        }

        boolean fullMoon = plugin.getMoonPhaseManager().isFullMoon(player.getWorld());
        if (fullMoon && !data.isShifted()) {
            player.sendMessage("§6It's a full moon - the shift isn't a choice tonight. It's already happening.");
            return;
        }

        data.setShifted(!data.isShifted());
        plugin.getRaceManager().refreshAttributes(player);
        plugin.getDataStore().save(data);

        if (data.isShifted()) {
            player.sendMessage("§6You give in to the wolf. Bones shift, senses sharpen.");
        } else {
            player.sendMessage("§6You force yourself back to human form.");
        }
    }

    private void info(Player player, PlayerData data) {
        player.sendMessage("§7--- §6Werewolf Status§7 ---");
        player.sendMessage("§7Curse triggered: " + (data.hasTriggeredCurse() ? "§aYes" : "§cNo - kill a human to trigger it"));
        player.sendMessage("§7Currently shifted: " + (data.isShifted() ? "§6Yes" : "§7No"));
        player.sendMessage("§7Full moon tonight: " + (plugin.getMoonPhaseManager().isFullMoon(player.getWorld()) ? "§6Yes" : "§7No"));
    }
}
