package com.legends.data.model;

import jakarta.persistence.*;

/**
 * HeroEntity is the JPA entity for the "heroes" table.
 *
 * Each hero belongs to exactly one party (@ManyToOne). The party_id column
 * in the database is the foreign key that links them.
 *
 * I use FetchType.LAZY on the party relationship so Hibernate doesn't load
 * the whole party object every time I load a single hero — it only loads it
 * if the code actually calls getParty().
 *
 * The default stats here (level 1, 5/5 attack/defense, 100 HP, 50 mana) match
 * the assignment spec's starting values exactly.
 */
@Entity
@Table(name = "heroes")
public class HeroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long heroId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    private String name;
    private int level      = 1;
    private int attack     = 5;
    private int defense    = 5;
    private int hp         = 100;
    private int maxHp      = 100;
    private int mana       = 50;
    private int maxMana    = 50;
    private int experience = 0;
    private String heroClass = "WARRIOR";

    public HeroEntity() {}

    public Long getHeroId()                     { return heroId; }
    public Party getParty()                     { return party; }
    public void setParty(Party party)           { this.party = party; }
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
