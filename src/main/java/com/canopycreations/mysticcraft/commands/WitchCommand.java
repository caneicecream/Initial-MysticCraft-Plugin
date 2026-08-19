package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.managers.SpellManager;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class WitchCommand implements CommandExecutor {

    private final MysticCraft plugin;

    public WitchCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (plugin.getRaceManager().getRace(player) != Race.WITCH) {
            player.sendMessage("§cYou aren't a witch.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§7Usage: /witch <cast|list> [spell]");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§5Known spells: §f" + String.join(", ", SpellManager.SPELLS));
            return true;
        }

        if (args[0].equalsIgnoreCase("cast")) {
            if (args.length < 2) {
                player.sendMessage("§7Usage: /witch cast <spell>");
                return true;
            }
            LivingEntity target = getTargetedEntity(player);
            plugin.getSpellManager().cast(player, args[1], target);
            return true;
        }

        player.sendMessage("§7Usage: /witch <cast|list> [spell]");
        return true;
    }

    private LivingEntity getTargetedEntity(Player player) {
        RayTraceResult result = player.rayTraceEntities(20);
        if (result != null && result.getHitEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
