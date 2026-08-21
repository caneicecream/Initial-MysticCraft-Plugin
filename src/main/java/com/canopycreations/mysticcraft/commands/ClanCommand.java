package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.clans.Clan;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClanCommand implements CommandExecutor {

    private final MysticCraft plugin;
    /** invitee uuid -> clan name they've been invited to */
    private final Map<UUID, String> invites = new HashMap<>();

    public ClanCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            help(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(player, args);
            case "invite" -> invite(player, args);
            case "join" -> join(player, args);
            case "leave" -> leave(player);
            case "claim" -> claim(player);
            case "unclaim" -> unclaim(player);
            case "info" -> info(player, args);
            case "list" -> list(player);
            case "ally" -> relation(player, args, true);
            case "enemy" -> relation(player, args, false);
            case "promote" -> promote(player, args);
            case "kick" -> kick(player, args);
            case "disband" -> disband(player);
            case "secret" -> toggleSecret(player);
            default -> help(player);
        }
        return true;
    }

    private void help(Player p) {
        p.sendMessage("§8§m                                        ");
        p.sendMessage("§f§lClans §7— covens, packs, courts and orders");
        p.sendMessage("§7/clan create <name> <court|pack|coven|order|circle>");
        p.sendMessage("§7/clan invite <player> §8· §7/clan join <name> §8· §7/clan leave");
        p.sendMessage("§7/clan info [name] §8· §7/clan list §8— §8claim ground by planting a totem");
        p.sendMessage("§7/clan ally <name> §8· §7/clan enemy <name>");
        p.sendMessage("§7/clan promote <player> §8· §7/clan kick <player> §8· §7/clan disband");
        p.sendMessage("§7/clan secret §8— hide from public listings");
        p.sendMessage("§8§m                                        ");
    }

    private void create(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage("§7Usage: /clan create <name> <court|pack|coven|order|circle>");
            return;
        }
        if (plugin.getClanManager().getPlayerClan(p.getUniqueId()) != null) {
            p.sendMessage("§cLeave your current clan first.");
            return;
        }
        String name = args[1];
        if (plugin.getClanManager().exists(name)) {
            p.sendMessage("§cA clan by that name already exists.");
            return;
        }
        Clan.Kind kind = Clan.Kind.fromString(args[2]);
        if (kind == null) {
            p.sendMessage("§cUnknown kind. Options: court, pack, coven, order, circle");
            return;
        }
        Race required = kind.getRequiredRace();
        Race playerRace = plugin.getRaceManager().getRace(p);
        if (required != null && playerRace != required) {
            p.sendMessage("§cOnly " + required.getFormattedName() + "§c players can found a "
                    + kind.getLabel() + ". A §7Circle§c is open to anyone.");
            return;
        }

        Clan clan = plugin.getClanManager().create(name, kind, p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cCouldn't create that clan.");
            return;
        }
        p.sendMessage("§aYou founded " + clan.getFormattedName() + "§a, a " + kind.getLabel() + ".");
        if (!clan.isSecret()) {
            Bukkit.broadcastMessage("§7A new " + kind.getColorCode() + kind.getLabel()
                    + " §7has been founded: " + clan.getFormattedName());
        }
    }

    private void invite(Player p, String[] args) {
        Clan clan = requireElder(p);
        if (clan == null) return;
        if (args.length < 2) {
            p.sendMessage("§7Usage: /clan invite <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            p.sendMessage("§cPlayer not found.");
            return;
        }
        Race required = clan.getKind().getRequiredRace();
        if (required != null && plugin.getRaceManager().getRace(target) != required) {
            p.sendMessage("§cOnly " + required.getFormattedName() + "§c players can join a "
                    + clan.getKind().getLabel() + ".");
            return;
        }
        invites.put(target.getUniqueId(), clan.getName());
        target.sendMessage("§7You've been invited to " + clan.getFormattedName()
                + "§7. Type §f/clan join " + clan.getName() + "§7 to accept.");
        p.sendMessage("§aInvited " + target.getName() + ".");
    }

    private void join(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§7Usage: /clan join <name>");
            return;
        }
        String invited = invites.get(p.getUniqueId());
        if (invited == null || !invited.equalsIgnoreCase(args[1])) {
            p.sendMessage("§cYou haven't been invited to that clan.");
            return;
        }
        Clan clan = plugin.getClanManager().getClan(args[1]);
        if (clan == null) {
            p.sendMessage("§cThat clan no longer exists.");
            return;
        }
        Race required = clan.getKind().getRequiredRace();
        if (required != null && plugin.getRaceManager().getRace(p) != required) {
            p.sendMessage("§cYour race no longer qualifies for that clan.");
            return;
        }
        plugin.getClanManager().addMember(clan, p.getUniqueId());
        invites.remove(p.getUniqueId());
        p.sendMessage("§aYou joined " + clan.getFormattedName() + "§a.");
        clan.getMembers().forEach(u -> {
            Player m = Bukkit.getPlayer(u);
            if (m != null && !m.equals(p)) m.sendMessage("§7" + p.getName() + " has joined the clan.");
        });
    }

    private void leave(Player p) {
        Clan clan = plugin.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cYou're not in a clan.");
            return;
        }
        if (clan.isLeader(p.getUniqueId()) && clan.getMemberCount() > 1) {
            p.sendMessage("§cPromote a new leader before leaving, or disband the clan.");
            return;
        }
        plugin.getClanManager().removeMember(clan, p.getUniqueId());
        if (clan.getMemberCount() == 0) plugin.getClanManager().disband(clan);
        p.sendMessage("§7You left the clan.");
    }

    private void claim(Player p) {
        Clan clan = requireElder(p);
        if (clan == null) return;
        p.sendMessage("§8Ground is taken by planting a §6Clan Totem§8, not by typing.");
        p.sendMessage("§8It stands where you put it. Enemies can tear it down.");
        int base = plugin.getConfig().getInt("clans.base-claim-limit", 10);
        int per = plugin.getConfig().getInt("clans.claims-per-member", 5);
        if (clan.getClaimCount() >= clan.getClaimLimit(base, per)) {
            p.sendMessage("§cYour clan has claimed all the ground it can hold. Recruit more members.");
            return;
        }
        if (!plugin.getClanManager().claimChunk(clan, p.getLocation().getChunk())) {
            p.sendMessage("§cThis ground already belongs to someone.");
            return;
        }
        p.sendMessage("§aClaimed this chunk for " + clan.getFormattedName() + "§a. ("
                + clan.getClaimCount() + "/" + clan.getClaimLimit(base, per) + ")");
    }

    private void unclaim(Player p) {
        Clan clan = requireElder(p);
        if (clan == null) return;
        if (!plugin.getClanManager().unclaimChunk(clan, p.getLocation().getChunk())) {
            p.sendMessage("§cYour clan doesn't hold this chunk.");
            return;
        }
        p.sendMessage("§7Released this chunk.");
    }

    private void info(Player p, String[] args) {
        Clan clan = args.length >= 2
                ? plugin.getClanManager().getClan(args[1])
                : plugin.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cNo such clan.");
            return;
        }
        int base = plugin.getConfig().getInt("clans.base-claim-limit", 10);
        int per = plugin.getConfig().getInt("clans.claims-per-member", 5);
        p.sendMessage("§8§m                                        ");
        p.sendMessage(clan.getFormattedName() + " §8— " + clan.getKind().getColorCode() + clan.getKind().getLabel());
        if (!clan.getDescription().isBlank()) p.sendMessage("§7" + clan.getDescription());
        p.sendMessage("§7Leader: §f" + Bukkit.getOfflinePlayer(clan.getLeader()).getName());
        p.sendMessage("§7Members: §f" + clan.getMemberCount());
        p.sendMessage("§7Territory: §f" + clan.getClaimCount() + "§7/§f" + clan.getClaimLimit(base, per));
        if (!clan.getAllies().isEmpty()) p.sendMessage("§7Allies: §a" + String.join(", ", clan.getAllies()));
        if (!clan.getEnemies().isEmpty()) p.sendMessage("§7Enemies: §c" + String.join(", ", clan.getEnemies()));
        p.sendMessage("§8§m                                        ");
    }

    private void list(Player p) {
        var visible = plugin.getClanManager().getVisibleClans();
        if (visible.isEmpty()) {
            p.sendMessage("§7No clans have been founded yet.");
            return;
        }
        p.sendMessage("§8§m                                        ");
        p.sendMessage("§f§lKnown clans");
        visible.stream()
                .sorted((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()))
                .limit(15)
                .forEach(c -> p.sendMessage("  " + c.getFormattedName() + " §8" + c.getKind().getLabel()
                        + " §7— " + c.getMemberCount() + " members, " + c.getClaimCount() + " chunks"));
        p.sendMessage("§8§m                                        ");
    }

    private void relation(Player p, String[] args, boolean ally) {
        Clan clan = requireLeader(p);
        if (clan == null) return;
        if (args.length < 2) {
            p.sendMessage("§7Usage: /clan " + (ally ? "ally" : "enemy") + " <name>");
            return;
        }
        Clan other = plugin.getClanManager().getClan(args[1]);
        if (other == null || other.getName().equalsIgnoreCase(clan.getName())) {
            p.sendMessage("§cNo such clan.");
            return;
        }
        String key = other.getName().toLowerCase();
        if (ally) {
            clan.getEnemies().remove(key);
            clan.getAllies().add(key);
            p.sendMessage("§aYou've declared " + other.getFormattedName() + "§a an ally.");
            Bukkit.broadcastMessage("§7" + clan.getFormattedName() + " §7extends an alliance to " + other.getFormattedName());
        } else {
            clan.getAllies().remove(key);
            clan.getEnemies().add(key);
            p.sendMessage("§cYou've declared war on " + other.getFormattedName() + "§c.");
            Bukkit.broadcastMessage("§4" + clan.getFormattedName() + " §4has declared war on " + other.getFormattedName());
        }
        plugin.getClanManager().save();
    }

    private void promote(Player p, String[] args) {
        Clan clan = requireLeader(p);
        if (clan == null) return;
        if (args.length < 2) {
            p.sendMessage("§7Usage: /clan promote <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !clan.isMember(target.getUniqueId())) {
            p.sendMessage("§cThat player isn't in your clan.");
            return;
        }
        clan.addElder(target.getUniqueId());
        plugin.getClanManager().save();
        p.sendMessage("§a" + target.getName() + " is now an elder.");
        target.sendMessage("§aYou've been made an elder of " + clan.getFormattedName() + "§a.");
    }

    private void kick(Player p, String[] args) {
        Clan clan = requireElder(p);
        if (clan == null) return;
        if (args.length < 2) {
            p.sendMessage("§7Usage: /clan kick <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !clan.isMember(target.getUniqueId())) {
            p.sendMessage("§cThat player isn't in your clan.");
            return;
        }
        if (clan.isLeader(target.getUniqueId())) {
            p.sendMessage("§cYou can't kick the leader.");
            return;
        }
        plugin.getClanManager().removeMember(clan, target.getUniqueId());
        target.sendMessage("§cYou've been removed from " + clan.getFormattedName() + "§c.");
        p.sendMessage("§aRemoved " + target.getName() + ".");
    }

    private void disband(Player p) {
        Clan clan = requireLeader(p);
        if (clan == null) return;
        plugin.getClanManager().disband(clan);
        Bukkit.broadcastMessage("§8" + clan.getFormattedName() + " §8has been disbanded.");
    }

    private void toggleSecret(Player p) {
        Clan clan = requireLeader(p);
        if (clan == null) return;
        clan.setSecret(!clan.isSecret());
        plugin.getClanManager().save();
        p.sendMessage(clan.isSecret()
                ? "§8Your clan is now hidden from public listings. Only members know it exists."
                : "§7Your clan is now publicly listed.");
    }

    private Clan requireLeader(Player p) {
        Clan clan = plugin.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cYou're not in a clan.");
            return null;
        }
        if (!clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cOnly the leader can do that.");
            return null;
        }
        return clan;
    }

    private Clan requireElder(Player p) {
        Clan clan = plugin.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cYou're not in a clan.");
            return null;
        }
        if (!clan.isElder(p.getUniqueId())) {
            p.sendMessage("§cOnly elders can do that.");
            return null;
        }
        return clan;
    }
}
