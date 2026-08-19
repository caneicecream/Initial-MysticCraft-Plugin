package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MysticAdminCommand implements CommandExecutor {

    private final MysticCraft plugin;

    public MysticAdminCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mysticcraft.admin")) {
            sender.sendMessage("§cYou don't have permission for that.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§7Usage: /mystic <reload|setrace|humanity|moon|item|cure>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                if (plugin.getFactionsBridge() != null) {
                    plugin.getFactionsBridge().reload();
                }
                sender.sendMessage("§aMysticCraft config reloaded.");
            }
            case "setrace" -> setRace(sender, args);
            case "humanity" -> humanity(sender, args);
            case "moon" -> sender.sendMessage("§7Full moon right now: " + plugin.getMoonPhaseManager().isFullMoon(Bukkit.getWorlds().get(0)));
            case "item" -> giveItem(sender, args);
            case "cure" -> cure(sender, args);
            default -> sender.sendMessage("§7Usage: /mystic <reload|setrace|humanity|moon|item|cure>");
        }
        return true;
    }

    private void setRace(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§7Usage: /mystic setrace <player> <race>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        Race race = Race.fromString(args[2]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        if (race == null) {
            sender.sendMessage("§cUnknown race.");
            return;
        }
        plugin.getRaceManager().setRace(target, race, true);
        sender.sendMessage("§aSet " + target.getName() + " to " + race.getFormattedName());
    }

    private void humanity(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§7Usage: /mystic humanity <player> <amount>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        try {
            int amount = Integer.parseInt(args[2]);
            PlayerData data = plugin.getRaceManager().getData(target);
            data.setHumanity(amount);
            plugin.getDataStore().save(data);
            sender.sendMessage("§aSet " + target.getName() + "'s humanity to " + amount);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a number.");
        }
    }

    private void giveItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can receive items directly.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /mystic item <ring|wolfsbane|vervain|silver|stake|herb|wand>");
            return;
        }
        ItemStack item = switch (args[1].toLowerCase()) {
            case "ring" -> plugin.getMysticItems().daylightRing();
            case "wolfsbane" -> plugin.getMysticItems().wolfsbane();
            case "vervain" -> plugin.getMysticItems().vervain();
            case "silver" -> plugin.getMysticItems().silverWeapon();
            case "stake" -> plugin.getMysticItems().stake();
            case "herb" -> plugin.getMysticItems().witchHerb();
            case "wand" -> plugin.getMysticItems().witchWand();
            default -> null;
        };
        if (item == null) {
            sender.sendMessage("§cUnknown item type.");
            return;
        }
        player.getInventory().addItem(item);
        sender.sendMessage("§aGave you a " + item.getItemMeta().getDisplayName());
    }

    private void cure(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /mystic cure <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        plugin.getWerewolfListener().cureToxin(target);
        sender.sendMessage("§aCured " + target.getName() + " of werewolf venom (if they had it).");
    }
}
