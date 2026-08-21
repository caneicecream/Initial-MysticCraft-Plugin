package com.canopycreations.mysticcraft.data;

import com.canopycreations.mysticcraft.races.Race;

import java.util.UUID;

/**
 * Holds the full supernatural state for a single player.
 * Persisted to disk by DataStore.
 */
public class PlayerData {

    private final UUID uuid;
    private Race race = Race.HUMAN;

    // Vampire state
    private int humanity = 100;
    private boolean daylightRingEquipped = false;
    private long lastCompulsionMillis = 0L;
    private boolean vampireAbilitiesActive = true;
    private boolean hasVampireBloodInSystem = false;
    private long vampireBloodExpiresAtMillis = 0L;
    private boolean pendingTransition = false;
    private boolean transitioning = false;
    private long transitionDeadlineMillis = 0L;
    private boolean originalVampire = false;

    // Werewolf state
    private boolean isShifted = false;
    private boolean hasTriggeredCurse = false; // has taken their first human life / activated the curse
    private long lastShiftMillis = 0L;
    private com.canopycreations.mysticcraft.lore.Bloodline bloodline = null;
    private boolean latentWolfGene = false;    // carries the bloodline but doesn't know it - shows as HUMAN
    private boolean geneRollDone = false;      // ensures the roll only ever happens once per player

    // Witch state
    private long lastSpellMillis = 0L;
    private int spellsCastToday = 0;

    // Meta
    private long lastRaceSwitchMillis = 0L;
    private long turnedAtMillis = 0L;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
        this.turnedAtMillis = System.currentTimeMillis();
    }

    public int getHumanity() {
        return humanity;
    }

    public void setHumanity(int humanity) {
        this.humanity = Math.max(0, Math.min(100, humanity));
    }

    public boolean isDaylightRingEquipped() {
        return daylightRingEquipped;
    }

    public void setDaylightRingEquipped(boolean daylightRingEquipped) {
        this.daylightRingEquipped = daylightRingEquipped;
    }

    public long getLastCompulsionMillis() {
        return lastCompulsionMillis;
    }

    public void setLastCompulsionMillis(long lastCompulsionMillis) {
        this.lastCompulsionMillis = lastCompulsionMillis;
    }

    public boolean isVampireAbilitiesActive() {
        return vampireAbilitiesActive;
    }

    public void setVampireAbilitiesActive(boolean vampireAbilitiesActive) {
        this.vampireAbilitiesActive = vampireAbilitiesActive;
    }

    public boolean hasVampireBloodInSystem() {
        return hasVampireBloodInSystem;
    }

    public void setHasVampireBloodInSystem(boolean hasVampireBloodInSystem) {
        this.hasVampireBloodInSystem = hasVampireBloodInSystem;
    }

    public long getVampireBloodExpiresAtMillis() {
        return vampireBloodExpiresAtMillis;
    }

    public void setVampireBloodExpiresAtMillis(long vampireBloodExpiresAtMillis) {
        this.vampireBloodExpiresAtMillis = vampireBloodExpiresAtMillis;
    }

    public boolean isPendingTransition() {
        return pendingTransition;
    }

    public void setPendingTransition(boolean pendingTransition) {
        this.pendingTransition = pendingTransition;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public void setTransitioning(boolean transitioning) {
        this.transitioning = transitioning;
    }

    public long getTransitionDeadlineMillis() {
        return transitionDeadlineMillis;
    }

    public void setTransitionDeadlineMillis(long transitionDeadlineMillis) {
        this.transitionDeadlineMillis = transitionDeadlineMillis;
    }

    public boolean isOriginalVampire() {
        return originalVampire;
    }

    public void setOriginalVampire(boolean originalVampire) {
        this.originalVampire = originalVampire;
    }

    public boolean isShifted() {
        return isShifted;
    }

    public void setShifted(boolean shifted) {
        isShifted = shifted;
    }

    public boolean hasTriggeredCurse() {
        return hasTriggeredCurse;
    }

    public void setTriggeredCurse(boolean triggeredCurse) {
        this.hasTriggeredCurse = triggeredCurse;
    }

    public long getLastShiftMillis() {
        return lastShiftMillis;
    }

    public void setLastShiftMillis(long lastShiftMillis) {
        this.lastShiftMillis = lastShiftMillis;
    }

    public com.canopycreations.mysticcraft.lore.Bloodline getBloodline() {
        return bloodline;
    }

    public void setBloodline(com.canopycreations.mysticcraft.lore.Bloodline bloodline) {
        this.bloodline = bloodline;
    }

    public boolean hasLatentWolfGene() {
        return latentWolfGene;
    }

    public void setLatentWolfGene(boolean latentWolfGene) {
        this.latentWolfGene = latentWolfGene;
    }

    public boolean isGeneRollDone() {
        return geneRollDone;
    }

    public void setGeneRollDone(boolean geneRollDone) {
        this.geneRollDone = geneRollDone;
    }

    public long getLastSpellMillis() {
        return lastSpellMillis;
    }

    public void setLastSpellMillis(long lastSpellMillis) {
        this.lastSpellMillis = lastSpellMillis;
    }

    public int getSpellsCastToday() {
        return spellsCastToday;
    }

    public void setSpellsCastToday(int spellsCastToday) {
        this.spellsCastToday = spellsCastToday;
    }

    public long getLastRaceSwitchMillis() {
        return lastRaceSwitchMillis;
    }

    public void setLastRaceSwitchMillis(long lastRaceSwitchMillis) {
        this.lastRaceSwitchMillis = lastRaceSwitchMillis;
    }

    public long getTurnedAtMillis() {
        return turnedAtMillis;
    }
}
