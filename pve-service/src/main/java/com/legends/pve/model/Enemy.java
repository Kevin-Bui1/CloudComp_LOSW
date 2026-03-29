package com.legends.pve.model;

/** Simple enemy unit used in PvE room generation. */
public class Enemy {
    private String name;
    private int level, attack, defense, hp, maxHp, mana, maxMana;

    public Enemy(String name, int level, int attack, int defense, int hp, int mana) {
        this.name = name; this.level = level; this.attack = attack;
        this.defense = defense; this.hp = hp; this.maxHp = hp;
        this.mana = mana; this.maxMana = mana;
    }

    public String getName()     { return name; }
    public int getLevel()       { return level; }
    public int getAttack()      { return attack; }
    public int getDefense()     { return defense; }
    public int getHp()          { return hp; }
    public int getMaxHp()       { return maxHp; }
    public int getMana()        { return mana; }
    public int getMaxMana()     { return maxMana; }
}
