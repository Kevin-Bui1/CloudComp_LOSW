package game.battle;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Base stats at level 1: ATK 5, DEF 5, HP 100, Mana 50.
 * Per-level base growth:  +1 ATK, +1 DEF, +5 HP, +2 Mana  (before class bonuses).
 * Class stat bonuses per level-up:
 *   ORDER   : +5 Mana, +2 DEF
 *   CHAOS   : +3 ATK,  +5 HP
 *   WARRIOR : +2 ATK,  +3 DEF
 *   MAGE    : +5 Mana, +1 ATK
 *
 * XP threshold to reach level L:  Exp(L) = Exp(L-1) + 500 + 75*L + 20*L^2
 */
public class Hero extends Unit {

    //Enums

    public enum HeroClass {ORDER, CHAOS, WARRIOR, MAGE}

    public enum HybridClass {
        NONE,
        PRIEST, HERETIC, PALADIN, PROPHET,   // Order combos
        INVOKER, ROGUE, SORCERER,             // Chaos combos
        KNIGHT, WARLOCK,                      // Warrior combos
        WIZARD                                // Mage+Mage
    }

    //Fields

    private HeroClass activeClass;
    private HybridClass hybridClass;
    private boolean hybridized;
    private HeroClass specializationClass;

    private int orderLevel, chaosLevel, warriorLevel, mageLevel;
    private int currentExp;
    private int itemPurchaseScore;
    private String chosenAbility;

    //Constructors

    public Hero(String name, HeroClass startingClass) {
        super(name, 1, 5, 5, 100, 50);
        this.activeClass = startingClass;
        this.hybridClass = HybridClass.NONE;
        this.hybridized = false;
        this.specializationClass = null;
        this.currentExp = 0;
        this.itemPurchaseScore = 0;
        incrementClassLevel(startingClass);
    }

    public Hero(String name, int level, int attack, int defense, int maxHp, int maxMana) {
        super(name, level, attack, defense, maxHp, maxMana);
        this.activeClass = HeroClass.WARRIOR;
        this.hybridClass = HybridClass.NONE;
        this.hybridized = false;
    }

    public Hero(
            String name, int level, int attack, int defense, int maxHp, int maxMana,
            int hp, int mana,
            HeroClass activeClass, HybridClass hybridClass, boolean hybridized,
            int orderLevel, int chaosLevel, int warriorLevel, int mageLevel,
            int currentExp, int itemPurchaseScore
    ) {
        super(name, level, attack, defense, maxHp, maxMana);
        this.hp = hp;
        this.mana = mana;
        this.activeClass = activeClass;
        this.hybridClass = hybridClass;
        this.hybridized = hybridized;
        this.orderLevel = orderLevel;
        this.chaosLevel = chaosLevel;
        this.warriorLevel = warriorLevel;
        this.mageLevel = mageLevel;
        this.currentExp = currentExp;
        this.itemPurchaseScore = itemPurchaseScore;
    }

    //EXP & Leveling

    /**
     * Returns true if at least one level-up occurred.
     */
    public boolean addExp(int exp) {
        if (exp <= 0) return false;
        currentExp += exp;
        boolean leveledUp = false;
        while (level < 20) {
            int needed = expNeededForNextLevel(level);
            if (currentExp < needed) break;
            currentExp -= needed;
            levelUp();
            leveledUp = true;
        }
        return leveledUp;
    }

    /**
     * EXP required to level up.
     */
    private int expNeededForNextLevel(int currentLevel) {
        int L = currentLevel + 1;
        return 500 + 75 * L + 20 * L * L;
    }

    private void levelUp() {
        level++;
        // Base per-level growth
        attack += 1;
        defense += 1;
        maxHp += 5;
        maxMana += 2;
        applyClassGrowth();
        hp = maxHp;
        mana = maxMana;
        incrementClassLevel(activeClass);
        checkHybridOrSpecialization();
    }

    private void applyClassGrowth() {
        if (hybridized) {
            HeroClass[] both = hybridComponents();
            for (HeroClass c : both) applyClassBonus(c, 1);
        } else if (specializationClass != null && specializationClass == activeClass) {
            applyClassBonus(activeClass, 2);
        } else {
            applyClassBonus(activeClass, 1);
        }
    }

    private void applyClassBonus(HeroClass cls, int multiplier) {
        switch (cls) {
            case ORDER -> {
                maxMana += 5 * multiplier;
                defense += 2 * multiplier;
            }
            case CHAOS -> {
                attack += 3 * multiplier;
                maxHp += 5 * multiplier;
            }
            case WARRIOR -> {
                attack += 2 * multiplier;
                defense += 3 * multiplier;
            }
            case MAGE -> {
                maxMana += 5 * multiplier;
                attack += 1 * multiplier;
            }
        }
    }

    private void incrementClassLevel(HeroClass cls) {
        switch (cls) {
            case ORDER -> orderLevel++;
            case CHAOS -> chaosLevel++;
            case WARRIOR -> warriorLevel++;
            case MAGE -> mageLevel++;
        }
    }

    private void checkHybridOrSpecialization() {
        if (hybridized) return;
        int maxed = 0;
        HeroClass firstMax = null, secondMax = null;
        for (HeroClass c : HeroClass.values()) {
            if (getClassLevel(c) >= 5) {
                maxed++;
                if (firstMax == null) firstMax = c;
                else if (secondMax == null) secondMax = c;
            }
        }
        if (maxed >= 2 && firstMax != null && secondMax != null) {
            hybridClass = resolveHybridClass(firstMax, secondMax);
            hybridized = true;
            specializationClass = null;
        } else if (maxed == 1 && specializationClass == null) {
            specializationClass = firstMax;
        }
    }

    private HeroClass[] hybridComponents() {
        return switch (hybridClass) {
            case PRIEST -> new HeroClass[]{HeroClass.ORDER, HeroClass.ORDER};
            case HERETIC -> new HeroClass[]{HeroClass.ORDER, HeroClass.CHAOS};
            case PALADIN -> new HeroClass[]{HeroClass.ORDER, HeroClass.WARRIOR};
            case PROPHET -> new HeroClass[]{HeroClass.ORDER, HeroClass.MAGE};
            case INVOKER -> new HeroClass[]{HeroClass.CHAOS, HeroClass.CHAOS};
            case ROGUE -> new HeroClass[]{HeroClass.CHAOS, HeroClass.WARRIOR};
            case SORCERER -> new HeroClass[]{HeroClass.CHAOS, HeroClass.MAGE};
            case KNIGHT -> new HeroClass[]{HeroClass.WARRIOR, HeroClass.WARRIOR};
            case WARLOCK -> new HeroClass[]{HeroClass.WARRIOR, HeroClass.MAGE};
            case WIZARD -> new HeroClass[]{HeroClass.MAGE, HeroClass.MAGE};
            default -> new HeroClass[]{activeClass, activeClass};
        };
    }

    private static HybridClass resolveHybridClass(HeroClass a, HeroClass b) {
        if (a.ordinal() > b.ordinal()) {
            HeroClass t = a;
            a = b;
            b = t;
        }
        return switch (a) {
            case ORDER -> switch (b) {
                case ORDER -> HybridClass.PRIEST;
                case CHAOS -> HybridClass.HERETIC;
                case WARRIOR -> HybridClass.PALADIN;
                case MAGE -> HybridClass.PROPHET;
                default -> HybridClass.NONE;
            };
            case CHAOS -> switch (b) {
                case CHAOS -> HybridClass.INVOKER;
                case WARRIOR -> HybridClass.ROGUE;
                case MAGE -> HybridClass.SORCERER;
                default -> HybridClass.NONE;
            };
            case WARRIOR -> switch (b) {
                case WARRIOR -> HybridClass.KNIGHT;
                case MAGE -> HybridClass.WARLOCK;
                default -> HybridClass.NONE;
            };
            case MAGE -> HybridClass.WIZARD;
            default -> HybridClass.NONE;
        };
    }

    //Combat / Abilities

    public String castAbility(List<Unit> allies, List<Unit> enemies) {
        String ability = (chosenAbility != null) ? chosenAbility : defaultAbilityName();
        if (ability == null) return name + " has no castable abilities.";
        return switch (ability) {
            case "Protect" -> castProtect(allies);
            case "Fire Shield" -> castFireShield(allies);
            case "Heal" -> castHeal(allies);
            case "Heal (all)" -> castHealAll(allies);
            case "Fireball" -> castFireball(enemies, false);
            case "Fireball (x2)" -> castFireball(enemies, true);
            case "Chain Lightning" -> castChainLightning(enemies, 0.25);
            case "Chain Lightning+" -> castChainLightning(enemies, 0.50);
            case "Berserker Attack" -> castBerserkerAttack(enemies, false, false);
            case "Berserker Attack (stun)" -> castBerserkerAttack(enemies, true, false);
            case "Berserker Attack (heal)" -> castBerserkerAttack(enemies, false, true);
            case "Sneak Attack" -> castSneakAttack(enemies);
            case "Replenish" -> castReplenish(allies, 80);
            case "Replenish (40)" -> castReplenish(allies, 40);
            case "Mana Burn" -> castManaBurn(enemies);
            default -> name + " cannot use " + ability + ".";
        };
    }

    private String castProtect(List<Unit> allies) {
        if (!spendMana(25)) return name + " needs 25 mana for Protect.";
        StringBuilder sb = new StringBuilder(name + " casts Protect!\n");
        for (Unit u : allies) {
            if (!u.isAlive()) continue;
            int s = (int) (u.getMaxHp() * 0.10);
            u.restoreHp(s);
            sb.append("  ").append(u.getName()).append(" +").append(s).append(" HP shield\n");
        }
        return sb.toString().trim();
    }

    private String castFireShield(List<Unit> allies) {
        if (!spendMana(25)) return name + " needs 25 mana for Fire Shield.";
        StringBuilder sb = new StringBuilder(name + " casts Fire Shield!\n");
        for (Unit u : allies) {
            if (!u.isAlive()) continue;
            int s = (int) (u.getMaxHp() * 0.10);
            u.restoreHp(s);
            sb.append("  ").append(u.getName()).append(" +").append(s).append(" HP (reflects 10%)\n");
        }
        return sb.toString().trim();
    }

    private String castHeal(List<Unit> allies) {
        if (!spendMana(35)) return name + " needs 35 mana for Heal.";
        Unit t = lowestHpAlly(allies);
        if (t == null) return name + " has no allies to heal.";
        int heal = (int) (t.getMaxHp() * 0.25);
        t.restoreHp(heal);
        return name + " heals " + t.getName() + " for " + heal + " HP!";
    }

    private String castHealAll(List<Unit> allies) {
        if (!spendMana(35)) return name + " needs 35 mana for Heal.";
        StringBuilder sb = new StringBuilder(name + " heals all allies!\n");
        for (Unit u : allies) {
            if (!u.isAlive()) continue;
            int h = (int) (u.getMaxHp() * 0.25);
            u.restoreHp(h);
            sb.append("  ").append(u.getName()).append(" +").append(h).append(" HP\n");
        }
        return sb.toString().trim();
    }

    private String castFireball(List<Unit> enemies, boolean doubled) {
        if (!spendMana(30)) return name + " needs 30 mana for Fireball.";
        List<Unit> living = enemies.stream().filter(Unit::isAlive).toList();
        if (living.isEmpty()) return name + " has no targets.";
        int count = Math.min(3, living.size());
        StringBuilder sb = new StringBuilder(name + " launches Fireball!\n");
        for (int i = 0; i < count; i++) {
            Unit t = living.get(i);
            int dmg = Math.max(0, attack - t.getDefense());
            if (doubled) dmg *= 2;
            t.takeDamage(dmg);
            sb.append("  ").append(t.getName()).append(" -").append(dmg).append(" HP\n");
        }
        return sb.toString().trim();
    }

    private String castChainLightning(List<Unit> enemies, double falloff) {
        if (!spendMana(40)) return name + " needs 40 mana for Chain Lightning.";
        List<Unit> living = new java.util.ArrayList<>(enemies.stream().filter(Unit::isAlive).toList());
        if (living.isEmpty()) return name + " has no targets.";
        Collections.shuffle(living, new Random());
        StringBuilder sb = new StringBuilder(name + " casts Chain Lightning!\n");
        double mult = 1.0;
        for (Unit t : living) {
            int dmg = (int) (Math.max(0, attack - t.getDefense()) * mult);
            t.takeDamage(dmg);
            sb.append("  ").append(t.getName()).append(" -").append(dmg).append(" HP\n");
            mult *= falloff;
        }
        return sb.toString().trim();
    }

    private String castBerserkerAttack(List<Unit> enemies, boolean stun, boolean healSelf) {
        if (!spendMana(60)) return name + " needs 60 mana for Berserker Attack.";
        List<Unit> living = new java.util.ArrayList<>(enemies.stream().filter(Unit::isAlive).toList());
        if (living.isEmpty()) return name + " has no targets.";
        StringBuilder sb = new StringBuilder();
        if (healSelf) {
            int h = (int) (maxHp * 0.10);
            restoreHp(h);
            sb.append(name).append(" heals self +").append(h).append(" HP\n");
        }
        Unit primary = living.get(0);
        int dmg = Math.max(0, attack - primary.getDefense());
        primary.takeDamage(dmg);
        sb.append(name).append(" Berserker Attacks ").append(primary.getName()).append(" for ").append(dmg).append("!\n");
        Random rng = new Random();
        for (int i = 1; i <= Math.min(2, living.size() - 1); i++) {
            Unit t = living.get(i);
            int splash = (int) (dmg * 0.25);
            t.takeDamage(splash);
            sb.append("  Splash ").append(t.getName()).append(" -").append(splash).append(" HP");
            if (stun && rng.nextDouble() < 0.5 && t.isAlive()) {
                t.setStunned(true);
                sb.append(" [STUNNED]");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String castSneakAttack(List<Unit> enemies) {
        List<Unit> living = new java.util.ArrayList<>(enemies.stream().filter(Unit::isAlive).toList());
        if (living.isEmpty()) return name + " has no targets.";
        Unit t = living.get(0);
        int dmg = Math.max(0, attack - t.getDefense());
        t.takeDamage(dmg);
        StringBuilder sb = new StringBuilder(name + " attacks " + t.getName() + " for " + dmg + "!\n");
        if (new Random().nextDouble() < 0.5) {
            Unit e2 = living.get(new Random().nextInt(living.size()));
            int bonus = (int) (dmg * 0.5);
            e2.takeDamage(bonus);
            sb.append("  Sneak hit ").append(e2.getName()).append(" -").append(bonus).append(" HP!\n");
        }
        return sb.toString().trim();
    }

    private String castReplenish(List<Unit> allies, int cost) {
        if (!spendMana(cost)) return name + " needs " + cost + " mana for Replenish.";
        StringBuilder sb = new StringBuilder(name + " casts Replenish!\n");
        for (Unit u : allies) {
            if (!u.isAlive()) continue;
            int gain = (u == this) ? 60 : 30;
            u.restoreMana(gain);
            sb.append("  ").append(u.getName()).append(" +").append(gain).append(" Mana\n");
        }
        return sb.toString().trim();
    }

    private String castManaBurn(List<Unit> enemies) {
        List<Unit> living = enemies.stream().filter(Unit::isAlive).toList();
        if (living.isEmpty()) return name + " has no targets.";
        Unit t = living.get(0);
        int dmg = Math.max(0, attack - t.getDefense());
        t.takeDamage(dmg);
        int burn = (int) (t.getMaxMana() * 0.10);
        t.spendMana(burn);
        return name + " attacks " + t.getName() + " for " + dmg + " and burns " + burn + " mana!";
    }

    private Unit lowestHpAlly(List<Unit> allies) {
        Unit result = null;
        for (Unit u : allies) {
            if (!u.isAlive()) continue;
            if (result == null || u.getHp() < result.getHp()) result = u;
        }
        return result;
    }

    private String defaultAbilityName() {
        if (hybridized) {
            return switch (hybridClass) {
                case PRIEST -> getMana() >= 35 ? "Heal (all)" : null;
                case HERETIC -> getMana() >= 25 ? "Fire Shield" : null;
                case PALADIN -> getMana() >= 60 ? "Berserker Attack (heal)" : null;
                case PROPHET -> getMana() >= 80 ? "Replenish" : null;
                case INVOKER -> getMana() >= 40 ? "Chain Lightning+" : null;
                case ROGUE -> "Sneak Attack";
                case SORCERER -> getMana() >= 30 ? "Fireball (x2)" : null;
                case KNIGHT -> getMana() >= 60 ? "Berserker Attack (stun)" : null;
                case WARLOCK -> "Mana Burn";
                case WIZARD -> getMana() >= 40 ? "Replenish (40)" : null;
                default -> null;
            };
        }
        return switch (activeClass) {
            case ORDER -> getMana() >= 35 ? "Heal" : (getMana() >= 25 ? "Protect" : null);
            case CHAOS -> getMana() >= 40 ? "Chain Lightning" : (getMana() >= 30 ? "Fireball" : null);
            case WARRIOR -> getMana() >= 60 ? "Berserker Attack" : null;
            case MAGE -> getMana() >= 80 ? "Replenish" : null;
        };
    }

    public void setChosenAbility(String chosenAbility) {
        this.chosenAbility = chosenAbility;
    }

    public String getChosenAbility() {
        return chosenAbility;
    }

    private String formatAbilityLabel(String abilityName, int manaCost) {
        return abilityName + " (" + manaCost + " Mana)";
    }

    public String stripAbilityLabel(String label) {
        int idx = label.indexOf(" (");
        return (idx >= 0) ? label.substring(0, idx) : label;
    }

    public String[] getAvailableAbilityNames() {
        if (hybridized) {
            String def = defaultAbilityName();
            if (def == null) return new String[0];

            return switch (def) {
                case "Heal (all)" -> new String[]{formatAbilityLabel("Heal (all)", 35)};
                case "Fire Shield" -> new String[]{formatAbilityLabel("Fire Shield", 25)};
                case "Berserker Attack (heal)" -> new String[]{formatAbilityLabel("Berserker Attack (heal)", 60)};
                case "Replenish" -> new String[]{formatAbilityLabel("Replenish", 80)};
                case "Chain Lightning+" -> new String[]{formatAbilityLabel("Chain Lightning+", 40)};
                case "Sneak Attack" -> new String[]{"Sneak Attack (Passive/0 Mana)"};
                case "Fireball (x2)" -> new String[]{formatAbilityLabel("Fireball (x2)", 30)};
                case "Berserker Attack (stun)" -> new String[]{formatAbilityLabel("Berserker Attack (stun)", 60)};
                case "Mana Burn" -> new String[]{"Mana Burn (Passive/0 Mana)"};
                case "Replenish (40)" -> new String[]{formatAbilityLabel("Replenish (40)", 40)};
                default -> new String[]{def};
            };
        }

        java.util.List<String> names = new java.util.ArrayList<>();
        switch (activeClass) {
            case ORDER -> {
                if (getMana() >= 25) names.add(formatAbilityLabel("Protect", 25));
                if (getMana() >= 35) names.add(formatAbilityLabel("Heal", 35));
            }
            case CHAOS -> {
                if (getMana() >= 30) names.add(formatAbilityLabel("Fireball", 30));
                if (getMana() >= 40) names.add(formatAbilityLabel("Chain Lightning", 40));
            }
            case WARRIOR -> {
                if (getMana() >= 60) names.add(formatAbilityLabel("Berserker Attack", 60));
            }
            case MAGE -> {
                if (getMana() >= 80) names.add(formatAbilityLabel("Replenish", 80));
            }
        }
        return names.toArray(new String[0]);
    }

    //Class switching

    public void setActiveClass(HeroClass cls) {
        if (!hybridized) this.activeClass = cls;
    }

    //Items

    public void addItemPurchaseScore(int amount) {
        itemPurchaseScore += amount;
    }

    public int getItemPurchaseScore() {
        return itemPurchaseScore;
    }

    //Getters

    public HeroClass getActiveClass() {
        return activeClass;
    }

    public HybridClass getHybridClass() {
        return hybridClass;
    }

    public boolean isHybridized() {
        return hybridized;
    }

    public HeroClass getSpecializationClass() {
        return specializationClass;
    }

    public int getClassLevel(HeroClass cls) {
        return switch (cls) {
            case ORDER -> orderLevel;
            case CHAOS -> chaosLevel;
            case WARRIOR -> warriorLevel;
            case MAGE -> mageLevel;
        };
    }

    public int getCurrentExp() {
        return currentExp;
    }

    /**
     * Remove exp from the current level's progress.
     */
    public void loseExp(int amount) {
        currentExp = Math.max(0, currentExp - amount);
    }

    public int getExpToNextLevel() {
        if (level >= 20) return 0;
        return expNeededForNextLevel(level) - currentExp;
    }

    public int getExpNeededForNextLevelTotal() {
        if (level >= 20) return 0;
        return 500 + 75 * (level + 1) + 20 * (level + 1) * (level + 1);
    }

    public String getClassDisplayName() {
        if (hybridized) return hybridClass.name();
        if (specializationClass != null) return activeClass.name() + "*";
        return activeClass.name();
    }

    @Override
    public String toString() {
        return String.format(
                "%s [%s] Lv%d\nHP:%d/%d  Mana:%d/%d  ATK:%d DEF:%d\nEXP:%d (next: %d)",
                name, getClassDisplayName(), level,
                hp, maxHp, mana, maxMana, attack, defense,
                currentExp, getExpToNextLevel());
    }
}