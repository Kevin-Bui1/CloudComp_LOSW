package com.legends.battle.model;

/**
 * Enemy is an NPC unit. It has the same stats as a hero but no special abilities.
 * Enemies can only ATTACK, DEFEND, or WAIT — the assignment spec says they have no spells.
 *
 * I kept this as a subclass of Unit instead of using the same class for everything
 * because it makes the Battle logic cleaner (instanceof Hero checks let us know
 * whether to run ability logic or not).
 */
public class Enemy extends Unit {

    public Enemy(String name, int level, int attack, int defense, int maxHp, int maxMana) {
        super(name, level, attack, defense, maxHp, maxMana);
    }
}
