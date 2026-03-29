package game.campaign;

import game.battle.*;
import game.party.Party;
import ui.battle.BattleGUI;

import javax.swing.*;
import java.util.*;

/**
 * Rewards (win):  +75g per enemy level, +50 EXP per enemy level split among survivors.
 * Penalty (loss): -10% gold.
 */
public class BattleRoom extends Room {
    public BattleRoom(int floor) { super(floor); }

    @Override
    public String execute(Party party) {
        List<Unit> enemies   = generateEnemies(party);
        List<Unit> heroUnits = new ArrayList<>(party.getHeroes());

        StringBuilder result = new StringBuilder("⚔ Enemies encountered:\n");
        for (Unit e : enemies)
            result.append(String.format("  %s  Lv%d  HP:%d  ATK:%d  DEF:%d\n",
                e.getName(), e.getLevel(), e.getMaxHp(), e.getAttack(), e.getDefense()));

        final Object lock = new Object();
        final boolean[] won = {false};

        SwingUtilities.invokeLater(() -> {
            BattleGUI gui = new BattleGUI(heroUnits, enemies, () -> {
                won[0] = heroUnits.stream().anyMatch(Unit::isAlive);
                synchronized (lock) { lock.notifyAll(); }
            });
            gui.setVisible(true);
        });
        synchronized (lock) { try { lock.wait(); } catch (InterruptedException ignored) {} }

        if (won[0]) {
            int gold=0, exp=0;
            for (Unit e:enemies) { gold+=75*e.getLevel(); exp+=50*e.getLevel(); }
            party.addGold(gold);
            List<Hero> survivors=new ArrayList<>();
            for (Unit u:heroUnits) if(u.isAlive()&&u instanceof Hero h) survivors.add(h);
            if (!survivors.isEmpty()) {
                int each=exp/survivors.size();
                for (Hero h:survivors) if(h.addExp(each)) result.append(h.getName()).append(" levelled up!\n");
            }
            result.append("\nVictory! +").append(gold).append("g, +").append(exp).append(" EXP.\n");
        } else {
            //-10% gold
            int penalty = (int)(party.getGold() * 0.10);
            party.addGold(-penalty);

            //-30% exp in hero's current level
            List<Hero> survivors = new ArrayList<>();
            for (Unit u : heroUnits) if (u.isAlive() && u instanceof Hero h) survivors.add(h);
            for (Hero h : survivors) {
                int expPenalty = (int)(h.getCurrentExp() * 0.30);
                h.loseExp(expPenalty);
            }

            result.append("\nDefeated! Lost ").append(penalty).append("g and 30% of current level EXP.\n");
        }
        return result.toString();
    }

    private List<Unit> generateEnemies(Party party) {
        List<Unit> enemies = new ArrayList<>();
        Random rand = new Random();

        int playerTotalLevel = Math.max(1, party.getTotalLevel());

        int enemyCount;
        if (playerTotalLevel <= 2) {
            enemyCount = (rand.nextInt(100) < 30) ? 2 : 1;
        } else if (playerTotalLevel <= 5) {
            enemyCount = rand.nextInt(3) + 1;      // 1-3
        } else if (playerTotalLevel <= 10) {
            enemyCount = rand.nextInt(4) + 1;      // 1-4
        } else {
            enemyCount = rand.nextInt(5) + 1;      // 1-5
        }

        int minTotal;
        if (playerTotalLevel <= 5) {
            minTotal = Math.max(1, playerTotalLevel - 1);
        } else if (playerTotalLevel <= 15) {
            minTotal = Math.max(1, playerTotalLevel - 3);
        } else {
            minTotal = Math.max(1, playerTotalLevel - 8);
        }

        int maxTotal = playerTotalLevel + 1;
        int enemyTotalLevel = rand.nextInt(maxTotal - minTotal + 1) + minTotal;

        int[] levels = new int[enemyCount];
        for (int i = 0; i < enemyCount; i++) {
            levels[i] = 1;
        }

        int remaining = Math.max(0, enemyTotalLevel - enemyCount);
        while (remaining > 0) {
            int idx = rand.nextInt(enemyCount);
            if (levels[idx] < 10) {
                levels[idx]++;
                remaining--;
            }
        }

        String[] enemyTypes = {"Goblin", "Ogre", "Skeleton", "Orc", "Bandit", "Slime"};
        Map<String, Integer> nameCounts = new HashMap<>();

        for (int i = 0; i < enemyCount; i++) {
            int lvl = levels[i];

            String base = enemyTypes[rand.nextInt(enemyTypes.length)];
            int num = nameCounts.getOrDefault(base, 0) + 1;
            nameCounts.put(base, num);
            String name = base + "_" + num;

            int attack = 6 + lvl;
            int defense = 2 + (lvl / 2);
            int hp = 45 + lvl * 12;

            enemies.add(new Enemy(
                    name,
                    lvl,
                    attack,
                    defense,
                    hp,
                    0
            ));
        }

        return enemies;
    }
}
