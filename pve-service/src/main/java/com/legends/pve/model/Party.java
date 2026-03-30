package com.legends.pve.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Party holds the player's heroes, gold, and inventory during a PvE campaign.
 *
 * The inventory is a simple name→quantity map (e.g. "Bread" → 2).
 * Items are purchased at the Inn and consumed from the campaign view.
 */
public class Party {

    private List<Hero>          heroes    = new ArrayList<>();
    private int                 gold      = 0;
    private Map<String, Integer> inventory = new HashMap<>();

    public Party() {}

    // ── Heroes ────────────────────────────────────────────────────────────

    /** Adds a hero. Returns false (and does nothing) if the party is already at 5. */
    public boolean addHero(Hero hero) {
        if (heroes.size() >= 5) return false;
        heroes.add(hero);
        return true;
    }

    public List<Hero> getHeroes() { return heroes; }

    public int getCumulativeLevel() {
        return heroes.stream().mapToInt(Hero::getLevel).sum();
    }

    public boolean hasLivingHeroes() {
        return heroes.stream().anyMatch(h -> h.getHp() > 0);
    }

    // ── Gold ──────────────────────────────────────────────────────────────

    public int getGold()            { return gold; }
    public void setGold(int gold)   { this.gold = gold; }
    public void addGold(int amount) { this.gold += amount; }

    /** Deducts gold if available. Returns false if insufficient funds. */
    public boolean deductGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }

    // ── Inventory ─────────────────────────────────────────────────────────

    public Map<String, Integer> getInventory() { return inventory; }
    public void setInventory(Map<String, Integer> inventory) { this.inventory = inventory; }

    /** Adds qty of itemName to the inventory. */
    public void addItem(String itemName, int qty) {
        inventory.merge(itemName, qty, Integer::sum);
    }

    /**
     * Removes one unit of itemName from inventory.
     * Returns false if the item is not in inventory or qty is 0.
     */
    public boolean useItem(String itemName) {
        int qty = inventory.getOrDefault(itemName, 0);
        if (qty <= 0) return false;
        if (qty == 1) inventory.remove(itemName);
        else          inventory.put(itemName, qty - 1);
        return true;
    }
}
