package com.legends.pve.model;

import java.util.List;

/**
 * BattleRoom is one of the two room types. When entered, the party encounters enemies.
 *
 * The actual fighting happens in the Battle Service — BattleRoom just holds the
 * enemy list and calculates the rewards. PveController sends the enemy data to
 * the Battle Service via HTTP when the fight starts.
 *
 * Reward formulas from the assignment:
 *   Experience per enemy = 50 * enemyLevel
 *   Gold per enemy       = 75 * enemyLevel
 */
public class BattleRoom extends Room {

    private List<Enemy> enemies;

    public BattleRoom(int floor, List<Enemy> enemies) {
        super(floor);
        this.enemies = enemies;
    }

    /**
     * Returns a description of the enemies in the room.
     * The actual combat is handled separately by the Battle Service.
     */
    @Override
    public String enter(Party party) {
        StringBuilder sb = new StringBuilder("A battle room! You encounter: ");
        for (Enemy e : enemies) {
            sb.append(e.getName()).append(" (Lvl ").append(e.getLevel()).append(") ");
        }
        return sb.toString().trim();
    }

    /** Total XP reward for clearing this room (split among surviving heroes after battle). */
    public int calculateTotalExp() {
        return enemies.stream().mapToInt(e -> 50 * e.getLevel()).sum();
    }

    /** Total gold reward for clearing this room. */
    public int calculateTotalGold() {
        return enemies.stream().mapToInt(e -> 75 * e.getLevel()).sum();
    }

    public List<Enemy> getEnemies() { return enemies; }
}
