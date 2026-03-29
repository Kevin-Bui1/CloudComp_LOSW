package com.legends.battle.model;

/**
 * Unit is the base class for everything that fights — heroes and enemies both extend this.
 *
 * I put all the shared combat stuff here (HP, mana, attack, defense, shield, stun)
 * so I don't have to copy the same code into both Hero and Enemy.
 * This is just basic OOP inheritance — Hero and Enemy add their own stuff on top.
 */
public abstract class Unit {

    protected String name;
    protected int level;
    protected int attack;
    protected int defense;
    protected int hp;
    protected int maxHp;
    protected int mana;
    protected int maxMana;
    protected boolean stunned;
    protected int shield;   // extra HP buffer from the Order "Protect" ability

    /**
     * Constructor used by both Hero and Enemy when they're created.
     * HP and mana start at their max values.
     */
    public Unit(String name, int level, int attack, int defense, int maxHp, int maxMana) {
        this.name     = name;
        this.level    = level;
        this.attack   = attack;
        this.defense  = defense;
        this.hp       = maxHp;
        this.maxHp    = maxHp;
        this.mana     = maxMana;
        this.maxMana  = maxMana;
        this.stunned  = false;
        this.shield   = 0;
    }

    // -- Combat methods ------------------------------------------------------

    /** Returns true as long as the unit has HP remaining. */
    public boolean isAlive() { return hp > 0; }

    public boolean isStunned() { return stunned; }

    public void setStunned(boolean stunned) { this.stunned = stunned; }

    /**
     * Applies incoming damage. Shield absorbs first, then HP takes the rest.
     * HP is capped at 0 so it never goes negative.
     */
    public void takeDamage(int damage) {
        if (damage <= 0) return;
        if (shield > 0) {
            int absorbed = Math.min(shield, damage);
            shield -= absorbed;
            damage -= absorbed;
        }
        hp = Math.max(0, hp - damage);
    }

    /**
     * Heals the unit. Won't go above maxHp.
     */
    public void restoreHp(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    /**
     * Restores mana. Won't go above maxMana.
     */
    public void restoreMana(int amount) {
        mana = Math.min(maxMana, mana + amount);
    }

    /**
     * Tries to spend mana for an ability.
     * Returns false if there isn't enough — the caller then blocks the action.
     */
    public boolean spendMana(int cost) {
        if (mana < cost) return false;
        mana -= cost;
        return true;
    }

    /**
     * Adds a shield layer (from the Order class Protect spell).
     * Shield takes damage before HP does.
     */
    public void addShield(int amount) { shield += amount; }

    // -- Getters and setters -------------------------------------------------

    public String getName()   { return name; }
    public int getLevel()     { return level; }
    public int getAttack()    { return attack; }
    public int getDefense()   { return defense; }
    public int getHp()        { return hp; }
    public int getMaxHp()     { return maxHp; }
    public int getMana()      { return mana; }
    public int getMaxMana()   { return maxMana; }
    public int getShield()    { return shield; }

    public void setHp(int hp)       { this.hp = Math.max(0, Math.min(maxHp, hp)); }
    public void setMana(int mana)   { this.mana = Math.max(0, Math.min(maxMana, mana)); }
    public void setAttack(int a)    { this.attack = a; }
    public void setDefense(int d)   { this.defense = d; }
}
