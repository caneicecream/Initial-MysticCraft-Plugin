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
            gestures(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gestures")) {
            gestures(player);
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

        gestures(player);
        return true;
    }

    /**
     * The gesture vocabulary. This is the real interface - the /witch cast
     * command exists only as an accessibility fallback for players who can't
     * comfortably perform the physical motions.
     */
    private void gestures(Player player) {
        player.sendMessage("§8§m                                                ");
        player.sendMessage("§5§lThe Craft");
        player.sendMessage("§8Hold your Grimoire Wand. The motion is the spell.");
        player.sendMessage("");
        player.sendMessage("§7Raise it to the sky §8→ §aheal");
        player.sendMessage("§7Kneel and touch the earth §8→ §dboundary");
        player.sendMessage("§7Strike toward someone §8→ §btelekinesis");
        player.sendMessage("§7Point at them, crouched §8→ §5pain");
        player.sendMessage("§7Point at them, standing §8→ §6desiccate");
        player.sendMessage("§7Kneel, wand raised §8→ §eforge a daylight ring");
        player.sendMessage("");
        player.sendMessage("§8Others can see you do this. Choose your moment.");
        player.sendMessage("§8§m                                                ");
    }

    private LivingEntity getTargetedEntity(Player player) {
        RayTraceResult result = player.rayTraceEntities(20);
        if (result != null && result.getHitEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
