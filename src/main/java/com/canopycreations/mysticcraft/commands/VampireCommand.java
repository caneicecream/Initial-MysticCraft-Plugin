package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.items.MysticItems;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class VampireCommand implements CommandExecutor {

    private final MysticCraft plugin;

    public VampireCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        PlayerData data = plugin.getRaceManager().getData(player);
        if (data.getRace() != Race.VAMPIRE) {
            player.sendMessage("§cYou aren't a vampire.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§7Usage: /vampire <compel|status>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "compel" -> compel(player, data, args);
            case "status" -> status(player, data);
            default -> player.sendMessage("§7Usage: /vampire <compel|status>");
        }
        return true;
    }

    private void compel(Player vampire, PlayerData data, String[] args) {
        if (args.length < 3) {
            vampire.sendMessage("§8§m                                                ");
            vampire.sendMessage("§4§lCompulsion");
            vampire.sendMessage("§7You don't need a command for this.");
            vampire.sendMessage("");
            vampire.sendMessage("§7Crouch. Get close. Look them in the eye —");
            vampire.sendMessage("§7and they have to be looking back.");
            vampire.sendMessage("§7Hold it, and they're yours.");
            vampire.sendMessage("");
            vampire.sendMessage("§8Vervain stops it cold. So does looking away.");
            vampire.sendMessage("§8And anyone watching will see something pass between you.");
            vampire.sendMessage("§8§m                                                ");
            return;
        }
        long cooldownMillis = plugin.getConfig().getInt("vampire.compulsion-cooldown-seconds", 90) * 1000L;
        long remaining = cooldownMillis - (System.currentTimeMillis() - data.getLastCompulsionMillis());
        if (remaining > 0) {
            vampire.sendMessage("§4Your compulsion hasn't recharged yet. (" + (remaining / 1000) + "s left)");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            vampire.sendMessage("§cPlayer not found.");
            return;
        }

        double range = plugin.getConfig().getDouble("vampire.compulsion-range", 5.0);
        if (target.getLocation().distance(vampire.getLocation()) > range) {
            vampire.sendMessage("§4They're too far away to compel - get closer.");
            return;
        }

        if (isVervainProtected(target)) {
            vampire.sendMessage("§4They're wearing vervain - compulsion fails.");
            target.sendMessage("§dYou feel a strange pull... but the vervain protects you.");
            return;
        }

        boolean eyeContactRequired = plugin.getConfig().getBoolean("vampire.compulsion-requires-eye-contact", true);
        if (eyeContactRequired && !isFacing(target, vampire)) {
            vampire.sendMessage("§4They need to be looking at you for compulsion to work.");
            return;
        }

        String suggestion = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        data.setLastCompulsionMillis(System.currentTimeMillis());
        plugin.getDataStore().save(data);

        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0));
        target.sendTitle("§4§oCompelled", "§7\"" + suggestion + "\"", 5, 60, 10);
        vampire.sendMessage("§4You compel " + target.getName() + ": \"" + suggestion + "\"");
    }

    private boolean isVervainProtected(Player target) {
        MysticItems items = plugin.getMysticItems();
        for (ItemStack stack : target.getInventory().getContents()) {
            if (items.hasTag(stack, MysticItems.TAG_VERVAIN)) return true;
        }
        return false;
    }

    private boolean isFacing(Player looker, Player target) {
        Vector toTarget = target.getLocation().toVector().subtract(looker.getLocation().toVector()).normalize();
        Vector lookDir = looker.getLocation().getDirection().normalize();
        return lookDir.dot(toTarget) > 0.5; // roughly within a ~60 degree cone
    }

    private void status(Player player, PlayerData data) {
        player.sendMessage("§7--- §4Vampire Status§7 ---");
        if (data.isTransitioning()) {
            long remainingMs = data.getTransitionDeadlineMillis() - System.currentTimeMillis();
            long minutes = Math.max(0, remainingMs / 60_000L);
            player.sendMessage("§4§lTransitioning - §7feed on a human within §4" + minutes + " minutes§7 or this is permanent.");
        }
        player.sendMessage("§7Humanity: §f" + data.getHumanity() + "/100");
        player.sendMessage("§7Daylight Ring: " + (data.isDaylightRingEquipped() ? "§aEquipped" : "§cNot equipped"));
        player.sendMessage("§7Original Vampire: " + (data.isOriginalVampire() ? "§6Yes" : "§7No"));
        boolean poisoned = plugin.getWerewolfListener().isPoisoned(player);
        player.sendMessage("§7Werewolf venom: " + (poisoned ? "§4Active - find a cure!" : "§aNone"));
    }
}
