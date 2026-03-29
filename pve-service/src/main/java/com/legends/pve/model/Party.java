package com.legends.pve.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Party is the group of heroes the player controls during a PvE campaign.
 *
 * Note: there's also a Party entity in data-service, but that's a JPA database
 * entity. This one is just a plain Java object used in memory during gameplay.
 * When the player saves, the PveController converts this into a save request
 * and sends it to the Data Service.
 */
public class Party {

    private List<Hero> heroes = new ArrayList<>();
    private int gold          = 0;

    public Party() {}

    /** Adds a hero to the party. The 5-hero max is enforced by the caller. */
    public void addHero(Hero hero) { heroes.add(hero); }

    public List<Hero> getHeroes() { return heroes; }

    /**
     * Sums up all hero levels. Used for:
     *  - Room probability calculation (higher = more battles)
     *  - Enemy scaling in RoomFactory
     *  - Final score display
     */
    public int getCumulativeLevel() {
        return heroes.stream().mapToInt(Hero::getLevel).sum();
    }

    /** Returns true if at least one hero is still standing (hp > 0). */
    public boolean hasLivingHeroes() {
        return heroes.stream().anyMatch(h -> h.getHp() > 0);
    }

    public int getGold()            { return gold; }
    public void setGold(int gold)   { this.gold = gold; }
    public void addGold(int amount) { this.gold += amount; }

    /** Returns false if the player doesn't have enough gold — used for shop purchases. */
    public boolean deductGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }
}
