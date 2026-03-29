package com.legends.battle.service;

import com.legends.battle.model.Action;
import com.legends.battle.model.Enemy;
import com.legends.battle.model.Hero;
import com.legends.battle.model.Unit;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * This is the actual combat engine — the class that runs a single battle.
 *
 * I store two lists (player party and enemy party) plus two queues.
 * The main queue (turnQueue) holds the order units act in this round.
 * The wait queue (waitQueue) holds units that chose WAIT — they go last.
 *
 * Turn order rule from the assignment: highest level goes first, ties broken by attack.
 * After everyone has acted (turnQueue empty), the wait queue flushes into the next round.
 *
 * Important: this class holds one battle's state. BattleService keeps a map of
 * battleId -> Battle so multiple battles can run at the same time without interfering.
 */
@Service
public class Battle {

    private List<Unit> playerParty;
    private List<Unit> enemyParty;
    private Queue<Unit> turnQueue  = new LinkedList<>();
    private Queue<Unit> waitQueue  = new LinkedList<>();
    private boolean battleOver = false;

    /**
     * Sets up a new battle with both parties and builds the first turn order.
     * Called once when the /start endpoint is hit.
     */
    public void init(List<Unit> playerParty, List<Unit> enemyParty) {
        this.playerParty = new ArrayList<>(playerParty);
        this.enemyParty  = new ArrayList<>(enemyParty);
        this.battleOver  = false;
        initializeTurnOrder();
    }

    /**
     * Sorts all alive units by level (highest first), then attack (highest first),
     * and puts them in the turn queue. This runs at the start of every new round.
     */
    public void initializeTurnOrder() {
        turnQueue.clear();
        waitQueue.clear();
        List<Unit> combined = new ArrayList<>();
        combined.addAll(playerParty);
        combined.addAll(enemyParty);
        combined.stream()
                .filter(Unit::isAlive)
                .sorted(Comparator.comparingInt(Unit::getLevel).reversed()
                        .thenComparingInt(Unit::getAttack).reversed())
                .forEach(turnQueue::add);
    }

    /**
     * Handles one unit's turn. This gets called once per POST /action request.
     *
     * Flow:
     * 1. If the turn queue is empty, the round is over — flush the wait queue into it.
     * 2. Pull the next unit off the queue.
     * 3. Skip dead units (they shouldn't be here but just in case).
     * 4. If the unit is stunned, skip their turn and clear the stun.
     * 5. Otherwise, execute whichever action was passed in.
     */
    public String processTurn(Action action) {
        if (battleOver) return "Battle is already over. Winner: " + getWinner();

        // Refill turn queue from waitQueue when round ends
        if (turnQueue.isEmpty()) {
            turnQueue.addAll(waitQueue);
            waitQueue.clear();
        }

        Unit current = turnQueue.poll();
        if (current == null) return "No units available.";

        // Skip dead units silently
        if (!current.isAlive()) return processTurn(action);

        // Stunned units lose their turn
        if (current.isStunned()) {
            current.setStunned(false);
            return current.getName() + " is stunned and loses their turn.";
        }

        String result = switch (action) {
            case ATTACK  -> handleAttack(current);
            case DEFEND  -> handleDefend(current);
            case WAIT    -> handleWait(current);
            case CAST    -> handleCast(current);
        };

        // Re-add to wait queue if still alive (for next round)
        if (current.isAlive() && action != Action.WAIT) {
            // unit goes to next round automatically via initializeTurnOrder on new round
        }

        isBattleOver();
        return result;
    }

    /**
     * ATTACK: damage = attacker.attack - target.defense, minimum 1.
     * Targets the first alive enemy in the opposing list.
     */
    private String handleAttack(Unit attacker) {
        Unit target = findFirstAliveTarget(attacker);
        if (target == null) return attacker.getName() + " has no targets.";

        int damage = Math.max(1, attacker.getAttack() - target.getDefense());
        target.takeDamage(damage);
        return String.format("%s attacks %s for %d damage. %s HP: %d/%d",
                attacker.getName(), target.getName(), damage,
                target.getName(), target.getHp(), target.getMaxHp());
    }

    /**
     * DEFEND: unit skips their attack but recovers +10 HP and +5 mana.
     * Public so tests can call it directly.
     */
    public String handleDefend(Unit unit) {
        unit.restoreHp(10);
        unit.restoreMana(5);
        return String.format("%s defends. HP: %d/%d, Mana: %d/%d",
                unit.getName(), unit.getHp(), unit.getMaxHp(),
                unit.getMana(), unit.getMaxMana());
    }

    /**
     * WAIT: puts the unit at the back of the queue for this round (FIFO order).
     * Public so tests can call it directly.
     */
    public String handleWait(Unit unit) {
        waitQueue.add(unit);
        return unit.getName() + " waits.";
    }

    /**
     * CAST: routes to the correct ability based on the hero's class.
     * Enemies can't cast — they just attack instead.
     */
    private String handleCast(Unit unit) {
        if (unit instanceof Hero hero) {
            return switch (hero.getHeroClass().toUpperCase()) {
                case "WARRIOR" -> castBerserkerAttack(hero);
                case "ORDER"   -> castHeal(hero);
                case "CHAOS"   -> castFireball(hero);
                case "MAGE"    -> castReplenish(hero);
                default        -> handleAttack(unit);
            };
        }
        return handleAttack(unit); // enemies cannot cast
    }

    /**
     * Warrior ability: Berserker Attack.
     * Hits the primary target for full damage, then up to 2 more enemies for 25%.
     * Costs 60 mana. If mana is not enough the action is blocked.
     */
    private String castBerserkerAttack(Hero hero) {
        if (!hero.spendMana(60)) return hero.getName() + " has insufficient mana for Berserker Attack.";
        List<Unit> targets = getAliveTargetsFor(hero);
        if (targets.isEmpty()) return hero.getName() + " has no targets.";
        StringBuilder sb = new StringBuilder();
        int primary = Math.max(1, hero.getAttack() - targets.get(0).getDefense());
        targets.get(0).takeDamage(primary);
        sb.append(hero.getName()).append(" Berserker attacks ").append(targets.get(0).getName())
          .append(" for ").append(primary).append(" damage.");
        for (int i = 1; i < Math.min(3, targets.size()); i++) {
            int splash = Math.max(1, primary / 4);
            targets.get(i).takeDamage(splash);
            sb.append(" ").append(targets.get(i).getName()).append(" hit for ").append(splash).append(".");
        }
        return sb.toString();
    }

    /**
     * Order ability: Heal.
     * Finds the ally with the lowest current HP and restores 25% of their max HP.
     * Costs 35 mana.
     */
    private String castHeal(Hero hero) {
        if (!hero.spendMana(35)) return hero.getName() + " has insufficient mana for Heal.";
        List<Unit> allies = getAliveAlliesFor(hero);
        if (allies.isEmpty()) return "No allies to heal.";
        Unit target = allies.stream().min(Comparator.comparingInt(Unit::getHp)).orElse(hero);
        int amount = target.getMaxHp() / 4;
        target.restoreHp(amount);
        return hero.getName() + " heals " + target.getName() + " for " + amount + " HP.";
    }

    /**
     * Chaos ability: Fireball.
     * Hits up to 3 enemies with normal attack damage. Costs 30 mana.
     */
    private String castFireball(Hero hero) {
        if (!hero.spendMana(30)) return hero.getName() + " has insufficient mana for Fireball.";
        List<Unit> targets = getAliveTargetsFor(hero);
        StringBuilder sb = new StringBuilder(hero.getName() + " casts Fireball:");
        for (int i = 0; i < Math.min(3, targets.size()); i++) {
            int dmg = Math.max(1, hero.getAttack() - targets.get(i).getDefense());
            targets.get(i).takeDamage(dmg);
            sb.append(" ").append(targets.get(i).getName()).append(" -").append(dmg).append("HP;");
        }
        return sb.toString();
    }

    /**
     * Mage ability: Replenish.
     * Gives every ally +30 mana and gives the caster an extra +60 on top of that.
     * Costs 80 mana (deducted before the bonuses are applied).
     */
    private String castReplenish(Hero hero) {
        if (!hero.spendMana(80)) return hero.getName() + " has insufficient mana for Replenish.";
        List<Unit> allies = getAliveAlliesFor(hero);
        allies.forEach(a -> a.restoreMana(30));
        hero.restoreMana(60);
        return hero.getName() + " casts Replenish. All allies +30 mana, self +60 mana.";
    }

    // -- Helper methods ------------------------------------------------------

    // Returns the first alive enemy on the opposing side
    private Unit findFirstAliveTarget(Unit attacker) {
        List<Unit> targets = getAliveTargetsFor(attacker);
        return targets.isEmpty() ? null : targets.get(0);
    }

    // Figures out which list is the enemy for a given unit, then filters to alive only
    private List<Unit> getAliveTargetsFor(Unit unit) {
        boolean isPlayer = playerParty.contains(unit);
        return (isPlayer ? enemyParty : playerParty).stream().filter(Unit::isAlive).toList();
    }

    // Same idea but returns allies instead of enemies
    private List<Unit> getAliveAlliesFor(Unit unit) {
        boolean isPlayer = playerParty.contains(unit);
        return (isPlayer ? playerParty : enemyParty).stream().filter(Unit::isAlive).toList();
    }

    /**
     * Returns true if every unit in the given party has 0 HP.
     * Used to check both sides after each turn.
     */
    public boolean noLivingUnits(List<Unit> party) {
        return party.stream().noneMatch(Unit::isAlive);
    }

    /**
     * Checks if the battle is over and sets the flag.
     * Called at the end of every processTurn().
     */
    public boolean isBattleOver() {
        battleOver = noLivingUnits(playerParty) || noLivingUnits(enemyParty);
        return battleOver;
    }

    /**
     * Returns a string saying who won, or "Battle not over." if it's still going.
     */
    public String getWinner() {
        if (noLivingUnits(enemyParty))  return "Heroes win!";
        if (noLivingUnits(playerParty)) return "Enemies win!";
        return "Battle not over.";
    }

    /** Returns a snapshot of the current turn queue so the UI can display turn order. */
    public List<Unit> getTurnOrder() {
        return new ArrayList<>(turnQueue);
    }

    public List<Unit> getPlayerParty() { return playerParty; }
    public List<Unit> getEnemyParty()  { return enemyParty; }
    public boolean isBattleOverFlag()  { return battleOver; }
}
