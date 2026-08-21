package com.canopycreations.mysticcraft.commands;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.lore.Bloodline;
import com.canopycreations.mysticcraft.lore.LoreFragment;
import com.canopycreations.mysticcraft.lore.Progenitor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The in-game codex. Lore that only lives in a README is lore nobody reads -
 * this puts the mythology in front of players where it can actually shape
 * how they roleplay.
 */
public class LoreCommand implements CommandExecutor {

    private final MysticCraft plugin;

    public LoreCommand(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            index(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "origins" -> origins(p);
            case "progenitors" -> progenitors(p, args);
            case "bloodlines" -> bloodlines(p, args);
            case "names" -> names(p);
            case "read" -> read(p, args);
            case "found" -> found(p);
            case "town" -> town(p);
            case "ashfall" -> town(p);
            case "vampires" -> vampires(p);
            case "werewolves" -> werewolves(p);
            case "witches" -> witches(p);
            case "humans" -> humans(p);
            default -> index(p);
        }
        return true;
    }

    private void index(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§lThe Codex");
        p.sendMessage("§8A record of what is known, and what is only whispered.");
        p.sendMessage("");
        p.sendMessage("§7/lore origins §8— how all of this began");
        p.sendMessage("§7/lore progenitors §8— the three who came first");
        p.sendMessage("§7/lore names §8— the names beneath the titles");
        p.sendMessage("§7/lore town §8— the places in Ashfall and what they are");
        p.sendMessage("§7/lore found §8— what you\'ve pieced together so far");
        p.sendMessage("§7/lore read <fragment> §8— read something you\'ve learned");
        p.sendMessage("§7/lore bloodlines §8— the seven wolf lines");
        p.sendMessage("§7/lore vampires §8· §7/lore werewolves");
        p.sendMessage("§7/lore witches §8· §7/lore humans");
        p.sendMessage("§8§m                                                ");
    }

    private void origins(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§lOrigins");
        p.sendMessage("");
        p.sendMessage("§7Two thousand years ago there were no vampires. There were no");
        p.sendMessage("§7wolves that walked as men. There were only witches, and the");
        p.sendMessage("§7balance they were sworn not to break.");
        p.sendMessage("");
        p.sendMessage("§7Three people broke it.");
        p.sendMessage("");
        p.sendMessage("§7A witch who wanted to live forever stole an elixir meant for");
        p.sendMessage("§7his wedding and drank it with someone else. He became the first");
        p.sendMessage("§7thing that could not die. §8(/lore progenitors)");
        p.sendMessage("");
        p.sendMessage("§7A witch murdered by her own people reached up from the ground");
        p.sendMessage("§7as she died and chained seven tribes to the moon. Their");
        p.sendMessage("§7descendants still turn. §8(/lore bloodlines)");
        p.sendMessage("");
        p.sendMessage("§7And a mother who lost a child to those wolves rewrote the");
        p.sendMessage("§7immortality spell to make her family into something worse than");
        p.sendMessage("§7what killed him. Nature charged her for it, and it is still");
        p.sendMessage("§7collecting from every vampire that has existed since.");
        p.sendMessage("");
        p.sendMessage("§8Everything you are is a consequence of a decision made by");
        p.sendMessage("§8someone who has been dead a very long time.");
        p.sendMessage("§8§m                                                ");
    }

    private void progenitors(Player p, String[] args) {
        if (args.length >= 2) {
            Progenitor prog = Progenitor.fromString(String.join("_", java.util.Arrays.copyOfRange(args, 1, args.length)));
            if (prog != null) {
                p.sendMessage("§8§m                                                ");
                p.sendMessage(prog.getFormattedTitle());
                p.sendMessage("  " + prog.getFullHistoricName());
                p.sendMessage("");
                p.sendMessage("§8" + prog.getNameOrigin());
                p.sendMessage(prog.getLore());
                p.sendMessage("§aPowers: §7" + prog.getPowers());
                p.sendMessage("§cWeaknesses: §7" + prog.getWeaknesses());
                p.sendMessage("§8§m                                                ");
                return;
            }
        }
        plugin.getProgenitorManager().sendStatus(p);
        p.sendMessage("§8Read one: §7/lore progenitors <the immortal|the original witch|the first wolf>");
    }

    private void bloodlines(Player p, String[] args) {
        if (args.length >= 2) {
            Bloodline b = Bloodline.fromString(String.join("_", java.util.Arrays.copyOfRange(args, 1, args.length)));
            if (b != null) {
                p.sendMessage("§8§m                                                ");
                p.sendMessage(b.getFormattedName() + " §8bloodline");
                p.sendMessage("§7" + b.getLore());
                p.sendMessage("§8Venom: " + b.getVenomDamagePerSecond() + "/s for " + b.getVenomDurationSeconds() + "s");
                p.sendMessage("§8§m                                                ");
                return;
            }
        }
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§lThe Seven Bloodlines");
        p.sendMessage("§8Seven tribes stood over her body. Seven lines still pay for it.");
        p.sendMessage("");
        for (Bloodline b : Bloodline.values()) {
            p.sendMessage("  " + b.getFormattedName());
        }
        p.sendMessage("");
        p.sendMessage("§8Read one: §7/lore bloodlines <name>");
        p.sendMessage("§8§m                                                ");
    }

    private void found(Player p) {
        var codex = plugin.getCodexManager();
        int have = codex.discoveredCount(p);
        int total = codex.totalCount();

        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§lWhat You Know §8— " + have + "/" + total);
        p.sendMessage("");
        if (have == 0) {
            p.sendMessage("§8Nothing yet. Go outside. Talk to people. Survive some nights.");
            p.sendMessage("§8§m                                                ");
            return;
        }
        for (LoreFragment f : LoreFragment.values()) {
            if (codex.hasDiscovered(p, f)) {
                p.sendMessage("  §7" + f.getTitle() + " §8— /lore read " + f.name().toLowerCase());
            } else {
                p.sendMessage("  §8??? ");
            }
        }
        p.sendMessage("");
        p.sendMessage("§8Others have found pieces you haven\'t. That\'s deliberate.");
        p.sendMessage("§8§m                                                ");
    }

    private void read(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§7Usage: /lore read <fragment> §8— see /lore found");
            return;
        }
        LoreFragment f = LoreFragment.fromString(args[1]);
        if (f == null) {
            p.sendMessage("§8No such fragment.");
            return;
        }
        if (!plugin.getCodexManager().hasDiscovered(p, f)) {
            p.sendMessage("§8You haven\'t learned that yet. It\'s out there being lived through by somebody.");
            return;
        }
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§l" + f.getTitle());
        p.sendMessage(f.getText());
        p.sendMessage("§8§m                                                ");
    }

    private void town(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§lAshfall");
        p.sendMessage("§8She burned her old life down here. The town grew in the ash.");
        p.sendMessage("");
        for (var l : com.canopycreations.mysticcraft.world.Landmark.values()) {
            boolean placed = plugin.getLandmarkManager().isPlaced(l);
            p.sendMessage("  " + l.getFormattedName() + (placed ? "" : " §8(not yet found)"));
            p.sendMessage("    §8" + l.getShortDescription());
        }
        p.sendMessage("");
        p.sendMessage("§8Read one: §7/lore town <name>");
        p.sendMessage("§8§m                                                ");
    }

    private void names(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§lThe Three Names");
        p.sendMessage("§8Titles are what history remembers. These are what their");
        p.sendMessage("§8mothers called them.");
        p.sendMessage("");
        for (Progenitor prog : Progenitor.values()) {
            p.sendMessage(prog.getFullHistoricName());
            p.sendMessage("  §8" + prog.getColorCode() + prog.getTitle());
            p.sendMessage("  §8" + prog.getNameOrigin());
            p.sendMessage("");
        }
        p.sendMessage("§8Read one in full: §7/lore progenitors <title>");
        p.sendMessage("§8§m                                                ");
    }

    private void vampires(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§4§lVampires");
        p.sendMessage("§7Undead. Not merely long-lived — §fdead§7, and moving anyway.");
        p.sendMessage("");
        p.sendMessage("§7A vampire is made, never born. Blood is shared, the body dies,");
        p.sendMessage("§7and what gets back up has one chance to feed or finish dying.");
        p.sendMessage("");
        p.sendMessage("§7They are faster and stronger than anything human. The sun");
        p.sendMessage("§7unmakes them. Fire unmakes them. So does a stake of white oak,");
        p.sendMessage("§7or losing their head. And a wolf's bite is a death sentence");
        p.sendMessage("§7written slowly.");
        p.sendMessage("");
        p.sendMessage("§7Their worst enemy is not any of that. It's the humanity they");
        p.sendMessage("§7carry — a thing they can switch off to stop the guilt, and");
        p.sendMessage("§7find they cannot easily switch back on.");
        p.sendMessage("§8§m                                                ");
    }

    private void werewolves(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§6§lWerewolves");
        p.sendMessage("§7The curse sleeps. That's the cruelty of it.");
        p.sendMessage("");
        p.sendMessage("§7You can carry the bloodline your whole life and die of old age");
        p.sendMessage("§7never knowing. It only wakes when you take a life — which means");
        p.sendMessage("§7every wolf that has ever lived became one by killing someone.");
        p.sendMessage("");
        p.sendMessage("§7After that, the moon owns you. You turn whether you want to or");
        p.sendMessage("§7not, and what you do in that shape isn't yours to decide.");
        p.sendMessage("");
        p.sendMessage("§7Wolfsbane burns them. Silver bites deeper than it should.");
        p.sendMessage("§7But their venom kills vampires, and that has kept them alive");
        p.sendMessage("§7through a thousand years of being hunted.");
        p.sendMessage("§8§m                                                ");
    }

    private void witches(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§5§lWitches");
        p.sendMessage("§7Servants of the balance — and the reason it keeps breaking.");
        p.sendMessage("");
        p.sendMessage("§7Every catastrophe in this world traces back to a witch who");
        p.sendMessage("§7decided the rules didn't apply to them. Immortality. The moon");
        p.sendMessage("§7curse. The first vampires. All witchcraft. All punished.");
        p.sendMessage("");
        p.sendMessage("§7They have no fangs, no claws, no strength worth speaking of.");
        p.sendMessage("§7What they have is the ability to stop a vampire's heart from");
        p.sendMessage("§7across a room, and every other race knows it.");
        p.sendMessage("");
        p.sendMessage("§7Power taken beyond what nature allows is always borrowed.");
        p.sendMessage("§7Channel too much and it comes out of you instead.");
        p.sendMessage("§8§m                                                ");
    }

    private void humans(Player p) {
        p.sendMessage("§8§m                                                ");
        p.sendMessage("§f§lHumans");
        p.sendMessage("§7The overwhelming majority. The ones with the most to lose.");
        p.sendMessage("");
        p.sendMessage("§7A human has no gifts. What they have is numbers, daylight, and");
        p.sendMessage("§7the fact that everything hunting them still needs them alive.");
        p.sendMessage("");
        p.sendMessage("§7Vervain in your pocket makes your mind your own. Wolfsbane and");
        p.sendMessage("§7silver make a wolf think twice. A stake ends a creature that");
        p.sendMessage("§7has walked the earth for centuries.");
        p.sendMessage("");
        p.sendMessage("§7Some humans build Orders and hunt back. Some trade secrets to");
        p.sendMessage("§7the highest bidder. Some spend their whole lives never knowing");
        p.sendMessage("§7their neighbour turns when the moon is full.");
        p.sendMessage("");
        p.sendMessage("§8And some of you are carrying something you haven't found yet.");
        p.sendMessage("§8§m                                                ");
    }
}
