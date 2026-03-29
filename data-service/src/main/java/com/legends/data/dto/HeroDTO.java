package com.legends.data.dto;

public class HeroDTO {
    private String name;
    private int level, attack, defense, hp, maxHp, mana, maxMana, experience;
    private String heroClass;

    public HeroDTO() {}

    public String getName()                     { return name; }
    public void setName(String n)               { this.name = n; }
    public int getLevel()                       { return level; }
    public void setLevel(int l)                 { this.level = l; }
    public int getAttack()                      { return attack; }
    public void setAttack(int a)                { this.attack = a; }
    public int getDefense()                     { return defense; }
    public void setDefense(int d)               { this.defense = d; }
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
