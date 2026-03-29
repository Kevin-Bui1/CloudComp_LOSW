package com.legends.battle.model;

/**
 * The four actions a unit can take on their turn, straight from the assignment spec:
 *
 * ATTACK  — deal damage using the formula: attacker.attack - defender.defense (min 1)
 * DEFEND  — skip your turn but recover +10 HP and +5 mana
 * WAIT    — push your turn to the end of the round (FIFO with other waiters)
 * CAST    — use your class's special ability (only heroes, costs mana)
 */
public enum Action {
    ATTACK,
    DEFEND,
    WAIT,
    CAST
}
