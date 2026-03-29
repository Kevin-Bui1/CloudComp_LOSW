package game.battle;

import game.battle.observer.BattleObserver;
import game.battle.strategy.impl.AttackStrategy;
import game.battle.strategy.impl.CastStrategy;
import game.battle.strategy.impl.DefendStrategy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class Battle {
    private final List<Unit> playerParty;
    private final List<Unit> enemyParty;

    private final Deque<Unit> turnQueue = new ArrayDeque<>();
    private final Deque<Unit> waitQueue = new ArrayDeque<>();
    private final List<BattleObserver> observers = new ArrayList<>();

    private boolean battleOver = false;

    // prevent long battles
    private int noProgressTurns = 0;
    private int lastTotalHp = -1;

    public Battle(List<Unit> playerParty, List<Unit> enemyParty) {
        this.playerParty = playerParty;
        this.enemyParty = enemyParty;
        initTurnOrder();
        lastTotalHp = computeTotalLivingHp();
    }

    public void addObserver(BattleObserver observer) {
        if (observer != null) observers.add(observer);
    }

    public void removeObserver(BattleObserver observer) {
        observers.remove(observer);
    }

    public List<Unit> getPlayerParty() {
        return playerParty;
    }

    public List<Unit> getEnemyParty() {
        return enemyParty;
    }

    public boolean isBattleOver() {
        return battleOver || allDead(playerParty) || allDead(enemyParty);
    }

    public String getWinner() {
        if (!allDead(playerParty) && allDead(enemyParty)) return "Players";
        if (allDead(playerParty) && !allDead(enemyParty)) return "Enemies";
        return "None";
    }

    public Unit getCurrentUnit() {
        refillTurnQueueIfNeeded();

        while (!turnQueue.isEmpty()) {
            Unit current = turnQueue.peek();
            if (current != null && current.isAlive()) {
                return current;
            }
            turnQueue.poll();
        }
        return null;
    }

    public void executeAction(Action action) {
        if (battleOver) return;

        refillTurnQueueIfNeeded();

        Unit current = turnQueue.poll();
        if (current == null || !current.isAlive()) return;

        if (current.isStunned()) {
            current.setStunned(false);
            if (current.isAlive()) {
                waitQueue.offer(current);
            }
            updateStallCounter();
            return;
        }

        notifyAction(current, action);

        switch (action) {
            case ATTACK -> new AttackStrategy().execute(this, current);
            case DEFEND -> {
                new DefendStrategy().execute(this, current);

                if (playerParty.contains(current)) {
                    notifyAbilityResult(current, current.getName() + " recovers 10 HP and 5 Mana");
                } else {
                    notifyAbilityResult(current, current.getName() + " recovers 10 HP");
                }
            }
            case CAST -> new CastStrategy().execute(this, current);
            case WAIT -> {
                // Intentionally no immediate action; unit goes to end of round below.
            }
        }

        if (isBattleOver()) {
            battleOver = true;
            notifyBattleEnd(getWinner());
            return;
        }

        if (current.isAlive()) {
            waitQueue.offer(current);
        }

        updateStallCounter();
    }

    public void doAttack(Unit attacker) {
        Unit target = chooseTarget(attacker);
        doAttackTarget(attacker, target);
    }

    public void doAttackTarget(Unit attacker, Unit target) {
        if (target == null || !target.isAlive()) {
            doAttack(attacker);
            return;
        }
        int dmg = Math.max(0, attacker.getAttack() - target.getDefense());
        target.takeDamage(dmg);
        notifyDamage(attacker, target, dmg);
    }

    public void doDefend(Unit unit) {
        unit.restoreHp(10);

        if (playerParty.contains(unit)) {
            unit.restoreMana(5);
        }
    }

    public void startBattle() {
        while (!isBattleOver()) {
            Unit current = getCurrentUnit();
            if (current == null) break;

            Action action = chooseActionFor(current);
            executeAction(action);
        }

        battleOver = true;
        notifyBattleEnd(getWinner());
    }

    private void initTurnOrder() {
        turnQueue.clear();
        waitQueue.clear();

        List<Unit> players = new ArrayList<>();
        List<Unit> enemies = new ArrayList<>();

        for (Unit u : playerParty) {
            if (u.isAlive()) players.add(u);
        }
        for (Unit u : enemyParty) {
            if (u.isAlive()) enemies.add(u);
        }

        Comparator<Unit> byPriority = (a, b) -> {
            if (b.getLevel() != a.getLevel()) {
                return b.getLevel() - a.getLevel();
            }
            return b.getAttack() - a.getAttack();
        };

        players.sort(byPriority);
        enemies.sort(byPriority);

        if (players.isEmpty() && enemies.isEmpty()) return;
        if (players.isEmpty()) {
            turnQueue.addAll(enemies);
            return;
        }
        if (enemies.isEmpty()) {
            turnQueue.addAll(players);
            return;
        }

        boolean playerFirst = byPriority.compare(players.get(0), enemies.get(0)) <= 0;

        int i = 0;
        int j = 0;
        boolean playerTurn = playerFirst;

        while (i < players.size() || j < enemies.size()) {
            if (playerTurn) {
                if (i < players.size()) {
                    turnQueue.offer(players.get(i++));
                } else if (j < enemies.size()) {
                    turnQueue.offer(enemies.get(j++));
                }
            } else {
                if (j < enemies.size()) {
                    turnQueue.offer(enemies.get(j++));
                } else if (i < players.size()) {
                    turnQueue.offer(players.get(i++));
                }
            }
            playerTurn = !playerTurn;
        }
    }

    private void refillTurnQueueIfNeeded() {
        if (!turnQueue.isEmpty()) return;

        while (!waitQueue.isEmpty()) {
            Unit u = waitQueue.poll();
            if (u != null && u.isAlive()) {
                turnQueue.offer(u);
            }
        }
    }

    private Action chooseActionFor(Unit unit) {
        if (playerParty.contains(unit)) {
            return Action.ATTACK;
        }
        return chooseEnemyAction(unit);
    }

    private Action chooseEnemyAction(Unit enemy) {
        java.util.Random rand = new java.util.Random();

        int livingPlayers = countLiving(playerParty);
        int livingEnemies = countLiving(enemyParty);

        // prevent battle stall
        if (noProgressTurns >= 6) {
            return Action.ATTACK;
        }

        // In a 1v1 enemies should never defend.
        if (livingPlayers == 1 && livingEnemies == 1) {
            return Action.ATTACK;
        }

        double hpRatio = (double) enemy.getHp() / enemy.getMaxHp();
        int roll = rand.nextInt(100);

        // If enemies outnumber the player defend should be rare.
        if (livingEnemies > livingPlayers) {
            if (hpRatio < 0.30) {
                if (roll < 15) return Action.DEFEND;
                if (roll < 20) return Action.WAIT;
                return Action.ATTACK;
            } else {
                if (roll < 5) return Action.DEFEND;
                if (roll < 10) return Action.WAIT;
                return Action.ATTACK;
            }
        }

        // Even or disadvantaged enemy side
        if (hpRatio < 0.30) {
            if (roll < 25) return Action.DEFEND;
            if (roll < 35) return Action.WAIT;
            return Action.ATTACK;
        }

        if (hpRatio < 0.60) {
            if (roll < 10) return Action.DEFEND;
            if (roll < 15) return Action.WAIT;
            return Action.ATTACK;
        }

        // High HP -> mostly attack
        if (roll < 5) return Action.DEFEND;
        if (roll < 10) return Action.WAIT;
        return Action.ATTACK;
    }

    public Action getEnemyAction(Unit enemy) {
        return chooseEnemyAction(enemy);
    }

    private Unit chooseTarget(Unit attacker) {
        List<Unit> opposing = playerParty.contains(attacker) ? enemyParty : playerParty;
        Unit best = null;
        for (Unit u : opposing) {
            if (!u.isAlive()) continue;
            if (best == null || u.getHp() < best.getHp()) {
                best = u;
            }
        }
        return best;
    }

    private int countLiving(List<Unit> units) {
        int count = 0;
        for (Unit u : units) {
            if (u.isAlive()) count++;
        }
        return count;
    }

    private int computeTotalLivingHp() {
        int total = 0;
        for (Unit u : playerParty) {
            if (u.isAlive()) total += u.getHp();
        }
        for (Unit u : enemyParty) {
            if (u.isAlive()) total += u.getHp();
        }
        return total;
    }

    private void updateStallCounter() {
        int totalHp = computeTotalLivingHp();

        if (lastTotalHp != -1 && totalHp >= lastTotalHp) {
            noProgressTurns++;
        } else {
            noProgressTurns = 0;
        }

        lastTotalHp = totalHp;
    }

    private boolean allDead(List<Unit> units) {
        for (Unit u : units) {
            if (u.isAlive()) return false;
        }
        return true;
    }

    public void notifyAction(Unit actor, Action action) {
        for (BattleObserver o : observers) {
            o.onAction(actor, action);
        }
    }

    public void notifyDamage(Unit attacker, Unit target, int damage) {
        for (BattleObserver o : observers) {
            o.onDamage(attacker, target, damage);
        }
    }

    public void notifyAbilityResult(Unit actor, String result) {
        for (BattleObserver o : observers) {
            o.onAbilityResult(actor, result);
        }
    }

    public void notifyBattleEnd(String winner) {
        for (BattleObserver o : observers) {
            o.onBattleEnd(winner);
        }
    }
}