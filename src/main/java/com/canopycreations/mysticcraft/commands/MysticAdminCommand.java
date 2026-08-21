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
            sender.sendMessage("§7Usage: /mystic <reload|setrace|humanity|moon|originals|bloodline|progenitor|keeper|town|landmark|item|cure>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage("§aMysticCraft config reloaded.");
            }
            case "setrace" -> setRace(sender, args);
            case "humanity" -> humanity(sender, args);
            case "moon" -> sender.sendMessage("§7Full moon right now: " + plugin.getMoonPhaseManager().isFullMoon(Bukkit.getWorlds().get(0)));
            case "originals" -> sender.sendMessage("§6Original vampire slots remaining: §f"
                    + plugin.getOriginalsManager().remainingSlots() + "§7/§f" + plugin.getOriginalsManager().slotCount());
            case "bloodline" -> bloodline(sender, args);
            case "progenitor" -> progenitor(sender, args);
            case "keeper" -> keeper(sender, args);
            case "town" -> town(sender, args);
            case "landmark" -> landmark(sender, args);
            case "item" -> giveItem(sender, args);
            case "cure" -> cure(sender, args);
            default -> sender.sendMessage("§7Usage: /mystic <reload|setrace|humanity|moon|originals|bloodline|progenitor|keeper|town|landmark|item|cure>");
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
        if (race == Race.VAMPIRE) {
            plugin.getOriginalsManager().tryClaimOriginal(target);
        }
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

    private void bloodline(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /mystic bloodline <player> [give|remove]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        var data = plugin.getRaceManager().getData(target);

        if (args.length >= 3) {
            if (args[2].equalsIgnoreCase("give")) {
                data.setLatentWolfGene(true);
                data.setGeneRollDone(true);
                plugin.getDataStore().save(data);
                sender.sendMessage("§aGave " + target.getName() + " the latent werewolf gene (they were not notified).");
                return;
            }
            if (args[2].equalsIgnoreCase("remove")) {
                data.setLatentWolfGene(false);
                plugin.getDataStore().save(data);
                sender.sendMessage("§aRemoved the latent werewolf gene from " + target.getName() + ".");
                return;
            }
        }

        sender.sendMessage("§7" + target.getName() + " latent werewolf gene: "
                + (data.hasLatentWolfGene() ? "§6YES" : "§7no")
                + "§7 | curse triggered: " + (data.hasTriggeredCurse() ? "§6yes" : "§7no"));
    }

    private void progenitor(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /mystic progenitor <list|grant <player> <title>|strip <title>>");
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            if (sender instanceof Player p) plugin.getProgenitorManager().sendStatus(p);
            else for (var prog : com.canopycreations.mysticcraft.lore.Progenitor.values()) {
                var holder = plugin.getProgenitorManager().getHolder(prog);
                sender.sendMessage(prog.getTitle() + " - "
                        + (holder == null ? "unclaimed" : Bukkit.getOfflinePlayer(holder).getName()));
            }
            return;
        }
        if (args[1].equalsIgnoreCase("grant") && args.length >= 4) {
            Player target = Bukkit.getPlayerExact(args[2]);
            var prog = com.canopycreations.mysticcraft.lore.Progenitor.fromString(
                    String.join("_", java.util.Arrays.copyOfRange(args, 3, args.length)));
            if (target == null || prog == null) {
                sender.sendMessage("§cPlayer or title not found.");
                return;
            }
            boolean ok = plugin.getProgenitorManager().claim(target, prog);
            sender.sendMessage(ok ? "§aGranted." : "§cAlready claimed, or that player already holds a title.");
            return;
        }
        if (args[1].equalsIgnoreCase("strip") && args.length >= 3) {
            var prog = com.canopycreations.mysticcraft.lore.Progenitor.fromString(
                    String.join("_", java.util.Arrays.copyOfRange(args, 2, args.length)));
            if (prog == null) {
                sender.sendMessage("§cTitle not found.");
                return;
            }
            boolean ok = plugin.getProgenitorManager().strip(prog, "The title has been stripped.");
            sender.sendMessage(ok ? "§aStripped." : "§cThat title wasn't held.");
            return;
        }
        sender.sendMessage("§7Usage: /mystic progenitor <list|grant <player> <title>|strip <title>>");
    }

    private void town(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cPlayers only - the town generates around where you stand.");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§8§m                                                ");
            sender.sendMessage("§f§lGenerate Ashfall");
            sender.sendMessage("§7This will build the town centred on your position and");
            sender.sendMessage("§7register all 11 landmarks. §cIt overwrites terrain.");
            sender.sendMessage("");
            sender.sendMessage("§8The generated buildings are a readable skeleton, not a");
            sender.sendMessage("§8finished build. Replace them by hand or with schematics,");
            sender.sendMessage("§8then re-point each landmark with /mystic landmark set.");
            sender.sendMessage("");
            sender.sendMessage("§7Run §f/mystic town confirm§7 to proceed,");
            sender.sendMessage("§7or §f/mystic town confirm <seed>§7 to re-roll a specific layout.");
            sender.sendMessage("§8§m                                                ");
            return;
        }
        long seed;
        if (args.length >= 3) {
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                seed = args[2].hashCode();
            }
        } else {
            seed = System.currentTimeMillis();
        }
        sender.sendMessage("§7Building Ashfall... §8seed " + seed);
        sender.sendMessage("§8Generating in stages so the server keeps ticking.");
        plugin.getTownGenerator().generate(p.getLocation(), p, seed);
    }

    private void landmark(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /mystic landmark <list|set <name> [radius]|tp <name>>");
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§8§m                                                ");
            sender.sendMessage("§f§lLandmarks §8— " + plugin.getLandmarkManager().placedCount()
                    + "/" + com.canopycreations.mysticcraft.world.Landmark.values().length + " placed");
            for (var l : com.canopycreations.mysticcraft.world.Landmark.values()) {
                var loc = plugin.getLandmarkManager().getLocation(l);
                sender.sendMessage("  " + l.getFormattedName() + " §8"
                        + (loc == null ? "not placed"
                        : loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                        + " §8[" + l.getRole() + "]");
            }
            sender.sendMessage("§8§m                                                ");
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cPlayers only for set/tp.");
            return;
        }
        if (args[1].equalsIgnoreCase("set") && args.length >= 3) {
            var l = com.canopycreations.mysticcraft.world.Landmark.fromString(
                    String.join("_", java.util.Arrays.copyOfRange(args, 2, args.length)));
            if (l == null) {
                sender.sendMessage("§cUnknown landmark. /mystic landmark list");
                return;
            }
            int radius = 24;
            try {
                radius = Integer.parseInt(args[args.length - 1]);
            } catch (NumberFormatException ignored) {
                // no radius supplied, keep default
            }
            plugin.getLandmarkManager().setLocation(l, p.getLocation(), radius);
            sender.sendMessage("§a" + l.getFormattedName() + " §ais now here (radius " + radius + ").");
            return;
        }
        if (args[1].equalsIgnoreCase("tp") && args.length >= 3) {
            var l = com.canopycreations.mysticcraft.world.Landmark.fromString(
                    String.join("_", java.util.Arrays.copyOfRange(args, 2, args.length)));
            var loc = l == null ? null : plugin.getLandmarkManager().getLocation(l);
            if (loc == null) {
                sender.sendMessage("§cNot placed.");
                return;
            }
            p.teleport(loc);
            return;
        }
        sender.sendMessage("§7Usage: /mystic landmark <list|set <name> [radius]|tp <name>>");
    }

    private void keeper(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cPlayers only - keepers spawn where you're standing.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /mystic keeper <wanderer|apothecary|elder|packmother|hedgewitch|archivist>");
            sender.sendMessage("§8Spawns a persistent lore NPC at your location.");
            return;
        }
        String want = args[1].toLowerCase();
        var role = switch (want) {
            case "wanderer" -> com.canopycreations.mysticcraft.managers.LorekeeperManager.Role.WANDERER;
            case "apothecary" -> com.canopycreations.mysticcraft.managers.LorekeeperManager.Role.THE_APOTHECARY;
            case "elder" -> com.canopycreations.mysticcraft.managers.LorekeeperManager.Role.THE_ELDER;
            case "packmother" -> com.canopycreations.mysticcraft.managers.LorekeeperManager.Role.THE_PACKMOTHER;
            case "hedgewitch" -> com.canopycreations.mysticcraft.managers.LorekeeperManager.Role.THE_HEDGEWITCH;
            case "archivist" -> com.canopycreations.mysticcraft.managers.LorekeeperManager.Role.THE_ARCHIVIST;
            default -> null;
        };
        if (role == null) {
            sender.sendMessage("§cUnknown keeper. Options: wanderer, apothecary, elder, packmother, hedgewitch, archivist");
            return;
        }
        plugin.getLorekeeperManager().spawn(p.getLocation(), role);
        sender.sendMessage("§aSpawned " + role.getFormattedName() + "§a here. Right-click to speak.");
    }

    private void giveItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can receive items directly.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /mystic item <ring|wolfsbane|vervain|silver|stake|herb|wand|totem|book>");
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
            case "totem" -> plugin.getTotemListener().createTotem();
            default -> null;
        };
        if (item == null && args[1].equalsIgnoreCase("book")) {
            if (args.length < 3) {
                sender.sendMessage("§7Usage: /mystic item book <fragment>");
                return;
            }
            var frag = com.canopycreations.mysticcraft.lore.LoreFragment.fromString(args[2]);
            if (frag == null) {
                sender.sendMessage("§cUnknown fragment.");
                return;
            }
            item = plugin.getLoreBooks().create(frag);
        }
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
