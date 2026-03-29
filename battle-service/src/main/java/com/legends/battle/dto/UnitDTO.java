package com.legends.battle.dto;

/** Serialisable snapshot of a Unit for REST transport. */
public class UnitDTO {
    private String name;
    private int level;
    private int attack;
    private int defense;
    private int hp;
    private int maxHp;
    private int mana;
    private int maxMana;
    private boolean stunned;
    private boolean alive;
    private String heroClass; // null for enemies

    public UnitDTO() {}

    public String getName()               { return name; }
    public void setName(String name)      { this.name = name; }
    public int getLevel()                 { return level; }
    public void setLevel(int level)       { this.level = level; }
    public int getAttack()                { return attack; }
    public void setAttack(int attack)     { this.attack = attack; }
    public int getDefense()               { return defense; }
    public void setDefense(int defense)   { this.defense = defense; }
    public int getHp()                    { return hp; }
    public void setHp(int hp)             { this.hp = hp; }
    public int getMaxHp()                 { return maxHp; }
    public void setMaxHp(int maxHp)       { this.maxHp = maxHp; }
    public int getMana()                  { return mana; }
    public void setMana(int mana)         { this.mana = mana; }
    public int getMaxMana()               { return maxMana; }
    public void setMaxMana(int maxMana)   { this.maxMana = maxMana; }
    public boolean isStunned()            { return stunned; }
    public void setStunned(boolean s)     { this.stunned = s; }
    public boolean isAlive()              { return alive; }
    public void setAlive(boolean alive)   { this.alive = alive; }
    public String getHeroClass()          { return heroClass; }
    public void setHeroClass(String c)    { this.heroClass = c; }
}
