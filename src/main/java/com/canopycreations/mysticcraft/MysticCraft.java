package com.canopycreations.mysticcraft;

import com.canopycreations.mysticcraft.commands.MysticAdminCommand;
import com.canopycreations.mysticcraft.commands.RaceCommand;
import com.canopycreations.mysticcraft.commands.VampireCommand;
import com.canopycreations.mysticcraft.commands.WerewolfCommand;
import com.canopycreations.mysticcraft.commands.WitchCommand;
import com.canopycreations.mysticcraft.data.DataStore;
import com.canopycreations.mysticcraft.items.MysticItems;
import com.canopycreations.mysticcraft.listeners.CurseTriggerListener;
import com.canopycreations.mysticcraft.listeners.JoinQuitListener;
import com.canopycreations.mysticcraft.listeners.VampireListener;
import com.canopycreations.mysticcraft.listeners.WerewolfListener;
import com.canopycreations.mysticcraft.managers.BoundaryManager;
import com.canopycreations.mysticcraft.managers.HumanityManager;
import com.canopycreations.mysticcraft.managers.MoonPhaseManager;
import com.canopycreations.mysticcraft.managers.RaceManager;
import com.canopycreations.mysticcraft.managers.SpellManager;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.integrations.factions.FactionsBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MysticCraft extends JavaPlugin {

    private DataStore dataStore;
    private RaceManager raceManager;
    private HumanityManager humanityManager;
    private MoonPhaseManager moonPhaseManager;
    private SpellManager spellManager;
    private BoundaryManager boundaryManager;
    private MysticItems mysticItems;

    private VampireListener vampireListener;
    private WerewolfListener werewolfListener;
    private FactionsBridge factionsBridge; // null unless a Factions plugin is actually detected

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.dataStore = new DataStore(this);
        this.mysticItems = new MysticItems(this);
        this.raceManager = new RaceManager(this);
        this.humanityManager = new HumanityManager(this);
        this.moonPhaseManager = new MoonPhaseManager(this);
        this.spellManager = new SpellManager(this);
        this.boundaryManager = new BoundaryManager(this);

        this.vampireListener = new VampireListener(this);
        this.werewolfListener = new WerewolfListener(this);

        Bukkit.getPluginManager().registerEvents(vampireListener, this);
        Bukkit.getPluginManager().registerEvents(werewolfListener, this);
        Bukkit.getPluginManager().registerEvents(new CurseTriggerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(this), this);

        getCommand("race").setExecutor(new RaceCommand(this));
        getCommand("vampire").setExecutor(new VampireCommand(this));
        getCommand("werewolf").setExecutor(new WerewolfCommand(this));
        getCommand("witch").setExecutor(new WitchCommand(this));
        getCommand("mystic").setExecutor(new MysticAdminCommand(this));

        setupFactionsBridge();

        startTasks();

        getLogger().info("MysticCraft has awoken. Vampires, werewolves, and witches now walk among your players.");
    }

    @Override
    public void onDisable() {
        if (dataStore != null) {
            dataStore.saveAll();
        }
        getLogger().info("MysticCraft has gone dormant.");
    }

    private void setupFactionsBridge() {
        if (Bukkit.getPluginManager().getPlugin("Factions") == null) {
            getLogger().info("No Factions plugin detected - skipping Factions bridge (this is fine if you're not using one yet).");
            return;
        }
        try {
            this.factionsBridge = new FactionsBridge(this);
            Bukkit.getPluginManager().registerEvents(factionsBridge, this);
            getLogger().info("Factions plugin detected - MysticCraft <-> Factions bridge enabled.");
        } catch (Throwable t) {
            getLogger().log(java.util.logging.Level.WARNING,
                    "Found a 'Factions' plugin but couldn't hook into it - its API may not match what MysticCraft "
                            + "was built against. Race/faction bridge features are disabled; everything else works normally.", t);
            this.factionsBridge = null;
        }
    }

    private void startTasks() {
        // Main per-second tick: sunlight, venom, ripper checks, ring detection, boundary enforcement.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = raceManager.getData(player);

                updateDaylightRingState(player, data);

                vampireListener.tickSunlight(player, data);
                humanityManager.tickRipperCheck(player);
                werewolfListener.tickToxin(player);
                boundaryManager.enforce(player);
            }
        }, 20L, 20L);

        // Moon phase check - forces shifts on full moons.
        int moonInterval = getConfig().getInt("werewolf.full-moon-check-interval-ticks", 1200);
        Bukkit.getScheduler().runTaskTimer(this, () -> moonPhaseManager.tick(), 100L, moonInterval);

        // Dawn check - reverts forced shifts. Runs every 10 seconds so it doesn't miss the window.
        Bukkit.getScheduler().runTaskTimer(this, () -> moonPhaseManager.revertAtDawn(), 100L, 200L);

        // Faction race-majority bonus check, every 30 seconds. No-op if the bridge isn't active.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (factionsBridge != null) factionsBridge.tickMajorityBonus();
        }, 200L, 600L);
    }

    private void updateDaylightRingState(Player player, PlayerData data) {
        boolean wearing = false;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (mysticItems.hasTag(stack, MysticItems.TAG_DAYLIGHT_RING)) {
                wearing = true;
                break;
            }
        }
        if (wearing != data.isDaylightRingEquipped()) {
            data.setDaylightRingEquipped(wearing);
        }
    }

    public void registerBoundary(Location center, double radius, long expiresAtMillis) {
        boundaryManager.register(center, radius, expiresAtMillis);
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public HumanityManager getHumanityManager() {
        return humanityManager;
    }

    public MoonPhaseManager getMoonPhaseManager() {
        return moonPhaseManager;
    }

    public SpellManager getSpellManager() {
        return spellManager;
    }

    public MysticItems getMysticItems() {
        return mysticItems;
    }

    public WerewolfListener getWerewolfListener() {
        return werewolfListener;
    }

    public FactionsBridge getFactionsBridge() {
        return factionsBridge;
    }
}
