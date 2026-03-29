package com.legends.pve.dto;

/** Hero creation/restore request payload. */
public class HeroRequest {
    private String name, heroClass;
    private int level = 1, attack = 5, defense = 5;
    private int hp = 100, maxHp = 100, mana = 50, maxMana = 50;

    public HeroRequest() {}
    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }
    public String getHeroClass()                { return heroClass; }
    public void setHeroClass(String heroClass)  { this.heroClass = heroClass; }
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
}
