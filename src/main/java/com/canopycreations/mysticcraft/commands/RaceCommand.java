package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RaceCommand implements CommandExecutor {

    private final MysticCraft plugin;

    public RaceCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            info(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> info(player);
            case "list" -> player.sendMessage("§7Races: §fHuman, §4Vampire§f, §6Werewolf§f, §5Witch");
            case "set" -> setRace(player, args);
            case "humanity" -> humanity(player);
            default -> player.sendMessage("§7Usage: /race [set <race>|info|humanity|list]");
        }
        return true;
    }

    private void info(Player player) {
        PlayerData data = plugin.getRaceManager().getData(player);
        player.sendMessage("§7--- §dMysticCraft §7---");
        player.sendMessage("§7Race: " + data.getRace().getFormattedName());
        if (data.getRace() == Race.VAMPIRE) {
            player.sendMessage("§7Humanity: §f" + data.getHumanity() + "/100");
        }
        if (data.getRace() == Race.WEREWOLF) {
            player.sendMessage("§7Curse triggered: " + (data.hasTriggeredCurse() ? "§aYes" : "§cNo"));
            player.sendMessage("§7Shifted: " + (data.isShifted() ? "§6Yes" : "§7No"));
        }
    }

    private void setRace(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§7Usage: /race set <human|vampire|werewolf|witch>");
            return;
        }
        Race race = Race.fromString(args[1]);
        if (race == null) {
            player.sendMessage("§cUnknown race. Try human, vampire, werewolf, or witch.");
            return;
        }
        if (!plugin.getRaceManager().canSwitchRace(player)) {
            player.sendMessage("§cYou can't switch races again yet - ask an admin or wait for the cooldown.");
            return;
        }
        plugin.getRaceManager().setRace(player, race, true);
        plugin.getRaceManager().getData(player).setLastRaceSwitchMillis(System.currentTimeMillis());
    }

    private void humanity(Player player) {
        PlayerData data = plugin.getRaceManager().getData(player);
        if (data.getRace() != Race.VAMPIRE) {
            player.sendMessage("§cOnly vampires have a humanity meter.");
            return;
        }
        player.sendMessage("§7Your humanity: §f" + data.getHumanity() + "/100");
    }
}
