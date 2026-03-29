package com.legends.party;

import com.legends.battle.Hero;
import java.util.*;

/** Holds heroes, gold, and inventory. */
public class Party {
    private final List<Hero> heroes;
    private int gold;

    // 🔥 NEW: inventory (item name → quantity)
    private final Map<String, Integer> inventory = new LinkedHashMap<>();

    public Party(List<Hero> heroes) {
        this.heroes = new ArrayList<>(heroes);
        this.gold = 500;
    }

    public List<Hero> getHeroes() { return heroes; }

    public void addHero(Hero h) {
        if (h != null && heroes.size() < 5) heroes.add(h);
    }

    public int getTotalLevel() {
        int s = 0;
        for (Hero h : heroes) s += h.getLevel();
        return s;
    }

    public int getGold() { return gold; }

    public void addGold(int amt) {
        gold = Math.max(0, gold + amt);
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    //INVENTORY

    public Map<String, Integer> getInventory() {
        return inventory;
    }

    public void addItem(String name, int qty) {
        inventory.put(name, inventory.getOrDefault(name, 0) + qty);
    }

    public boolean hasItem(String name) {
        return inventory.getOrDefault(name, 0) > 0;
    }

    public boolean useItem(String name) {
        int count = inventory.getOrDefault(name, 0);
        if (count <= 0) return false;

        if (count == 1) inventory.remove(name);
        else inventory.put(name, count - 1);

        return true;
    }
}