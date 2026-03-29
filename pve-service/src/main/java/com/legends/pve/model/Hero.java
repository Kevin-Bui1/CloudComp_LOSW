package com.legends.pve.model;

/**
 * Hero is the player-controlled character in the PvE campaign.
 *
 * This is a separate class from the Hero in battle-service because the PvE service
 * needs the levelling logic (gainExperience, expToNextLevel) while the battle service
 * just needs the combat stats. They're separate microservices so they can't share code.
 *
 * Level-up stat formula from the assignment (applied every level):
 *   Base:    +1 attack, +1 defense, +5 maxHp, +2 maxMana
 *   ORDER:   +2 defense, +5 maxMana extra
 *   CHAOS:   +3 attack, +5 maxHp extra
 *   WARRIOR: +2 attack, +3 defense extra
 *   MAGE:    +1 attack, +5 maxMana extra
 */
public class Hero {

    private String name;
    private int level     = 1;
    private int attack    = 5;
    private int defense   = 5;
    private int hp        = 100;
    private int maxHp     = 100;
    private int mana      = 50;
    private int maxMana   = 50;
    private int experience = 0;
    private String heroClass = "WARRIOR";

    public Hero() {}

    public Hero(String name, String heroClass) {
        this.name = name;
        this.heroClass = heroClass;
    }

    /**
     * Returns how much total XP this hero needs to reach the next level.
     *
     * Formula: Exp(L) = sum of (500 + 75*i + 20*i^2) for i = 1 to current level
     * This makes each level-up progressively harder, which matches the assignment spec.
     */
    public int expToNextLevel() {
        int total = 0;
        for (int i = 1; i <= level; i++) {
            total += 500 + 75 * i + 20 * i * i;
        }
        return total;
    }

    /**
     * Adds XP to the hero and triggers level-ups if the threshold is crossed.
     * A single batch of XP can cause multiple level-ups (e.g. if you grind a lot).
     * Heroes cap at level 20.
     */
    public void gainExperience(int xp) {
        this.experience += xp;
        while (this.experience >= expToNextLevel() && level < 20) {
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        attack  += 1;
        defense += 1;
        maxHp   += 5;
        maxMana += 2;
        // Apply class bonus
        switch (heroClass.toUpperCase()) {
            case "ORDER"   -> { maxMana += 5; defense += 2; }
            case "CHAOS"   -> { attack  += 3; maxHp   += 5; }
            case "WARRIOR" -> { attack  += 2; defense += 3; }
            case "MAGE"    -> { maxMana += 5; attack  += 1; }
        }
        // Restore to new max on level-up
        hp   = maxHp;
        mana = maxMana;
    }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }
    public int getLevel()                       { return level; }
    public void setLevel(int level)             { this.level = level; }
    public int getAttack()                      { return attack; }
    public void setAttack(int attack)           { this.attack = attack; }
    public int getDefense()                     { return defense; }
    public void setDefense(int defense)         { this.defense = defense; }
    public int getHp()                          { return hp; }
    public void setHp(int hp)                   { this.hp = hp; }
    public int getMaxHp()                       { return maxHp; }
    public void setMaxHp(int maxHp)             { this.maxHp = maxHp; }
    public int getMana()                        { return mana; }
    public void setMana(int mana)               { this.mana = mana; }
    public int getMaxMana()                     { return maxMana; }
    public void setMaxMana(int maxMana)         { this.maxMana = maxMana; }
    public int getExperience()                  { return experience; }
    public void setExperience(int experience)   { this.experience = experience; }
    public String getHeroClass()                { return heroClass; }
    public void setHeroClass(String heroClass)  { this.heroClass = heroClass; }
}
