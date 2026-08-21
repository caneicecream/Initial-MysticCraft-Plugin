package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {

    private final MysticCraft plugin;

    public QuestCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        plugin.getQuestManager().sendProgress(p);
        p.sendMessage("§8Your story changes if you do. §7/lore §8for what you've pieced together.");
        return true;
    }
}
