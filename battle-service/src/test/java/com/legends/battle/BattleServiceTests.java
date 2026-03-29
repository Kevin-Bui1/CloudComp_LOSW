package com.legends.battle;

import com.legends.battle.model.*;
import com.legends.battle.service.Battle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Battle engine.
 *
 * I test Battle directly (not through the HTTP layer) because I want to check
 * the combat logic in isolation. Each test sets up a small scenario and
 * verifies that the output matches what the assignment spec says should happen.
 *
 * @BeforeEach creates a fresh battle before every test so they don't interfere.
 * Default setup: Arthur (level 3, 15 atk, 5 def) vs Goblin (level 1, 8 atk, 3 def).
 */
class BattleServiceTests {

    private Battle battle;
    private Hero   hero;
    private Enemy  enemy;

    @BeforeEach
    void setUp() {
        hero  = new Hero("Arthur", 3, 15, 5, 100, 80);
        enemy = new Enemy("Goblin", 1, 8, 3, 50, 20);
        battle = new Battle();
        battle.init(List.of(hero), List.of(enemy));
    }

    // BS-TC-01: Attacking an enemy reduces their HP
    @Test
    void testAttack_reducesEnemyHp() {
        int hpBefore = enemy.getHp();
        battle.processTurn(Action.ATTACK);
        assertTrue(enemy.getHp() < hpBefore, "Enemy HP should decrease after being attacked");
    }

    // BS-TC-02: HP should never go below zero (no negative HP)
    @Test
    void testAttack_hpNeverBelowZero() {
        Hero strongHero = new Hero("Titan", 10, 999, 5, 200, 100);
        Enemy weakEnemy = new Enemy("Rat", 1, 2, 0, 5, 0);
        Battle b = new Battle();
        b.init(List.of(strongHero), List.of(weakEnemy));
        b.processTurn(Action.ATTACK);
        assertEquals(0, weakEnemy.getHp(), "HP should not go below 0");
    }

    // BS-TC-03: DEFEND should restore +10 HP and +5 mana as per the spec
    @Test
    void testDefend_restoresHpAndMana() {
        hero.setHp(50);
        hero.setMana(10);
        battle.handleDefend(hero);
        assertEquals(60, hero.getHp(),   "Defend should restore +10 HP");
        assertEquals(15, hero.getMana(), "Defend should restore +5 mana");
    }

    // BS-TC-04: WAIT should put the unit in the wait queue (message says "waits")
    @Test
    void testWait_placesUnitInWaitQueue() {
        String result = battle.handleWait(hero);
        assertTrue(result.contains("waits"), "WAIT action message should say 'waits'");
    }

    // BS-TC-05: Battle should end when all enemy HP reaches 0
    @Test
    void testBattleOver_whenEnemyDefeated() {
        enemy.takeDamage(999);
        assertTrue(battle.isBattleOver(), "Battle should be over when enemy has 0 HP");
        assertEquals("Heroes win!", battle.getWinner());
    }

    // BS-TC-06: Battle should end when all hero HP reaches 0
    @Test
    void testBattleOver_whenHeroDefeated() {
        hero.takeDamage(999);
        assertTrue(battle.isBattleOver(), "Battle should be over when hero has 0 HP");
        assertEquals("Enemies win!", battle.getWinner());
    }

    // BS-TC-07: noLivingUnits should return false when there are still alive units
    @Test
    void testNoLivingUnits_false_whenPartyAlive() {
        assertFalse(battle.noLivingUnits(List.of(hero)),
                "noLivingUnits should return false when hero is alive");
    }

    // BS-TC-08: Verify the damage formula: damage = attacker.attack - defender.defense
    @Test
    void testDamageFormula_correctCalculation() {
        // hero.attack=15, enemy.defense=3 → expected damage=12
        int hpBefore = enemy.getHp();
        battle.processTurn(Action.ATTACK);
        int expectedDamage = Math.max(1, hero.getAttack() - enemy.getDefense());
        assertEquals(hpBefore - expectedDamage, enemy.getHp(),
                "Damage should equal attacker.attack - defender.defense");
    }

    // BS-TC-09: The highest level unit should always go first in turn order
    @Test
    void testTurnOrder_highLevelFirst() {
        Hero lowLevel  = new Hero("Novice", 1, 5, 5, 100, 50);
        Hero highLevel = new Hero("Master", 9, 20, 8, 200, 80);
        Battle b = new Battle();
        b.init(List.of(lowLevel, highLevel), List.of(new Enemy("e", 1, 5, 5, 50, 0)));
        List<com.legends.battle.model.Unit> order = b.getTurnOrder();
        assertEquals("Master", order.get(0).getName(),
                "Highest level unit should act first");
    }

    // BS-TC-10: A stunned unit should lose their turn and the stun flag should clear
    @Test
    void testStun_skipsUnitTurn() {
        hero.setStunned(true);
        String result = battle.processTurn(Action.ATTACK);
        assertTrue(result.contains("stunned"), "Stunned unit message should mention stun");
        assertFalse(hero.isStunned(), "Stun flag should be cleared after skipped turn");
    }

    // BS-TC-11: Shield should absorb damage before HP takes any hit
    @Test
    void testShield_absorbesDamageFirst() {
        hero.addShield(20);
        hero.takeDamage(15);
        assertEquals(100, hero.getHp(), "Shield should absorb damage before HP");
        assertEquals(5,   hero.getShield(), "Remaining shield should be 5");
    }

    // BS-TC-12: CAST should fail and return an error message if the unit has no mana
    @Test
    void testCast_insufficientMana_blockedAction() {
        hero.setMana(0);
        String result = battle.processTurn(Action.CAST);
        assertTrue(result.contains("insufficient mana"),
                "CAST should fail when unit has no mana");
    }
}
