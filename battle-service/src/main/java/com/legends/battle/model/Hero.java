package com.legends.battle.model;

/**
 * Hero represents a player-controlled unit in combat.
 *
 * It extends Unit (which has all the shared stats) and adds heroClass and experience.
 * The class bonuses and levelling logic live in the PvE service because that's where
 * levelling actually happens. By the time a Hero arrives at the Battle service it
 * already has its final stats baked in.
 */
public class Hero extends Unit {

    // One of "ORDER", "CHAOS", "WARRIOR", "MAGE" — used to pick the right CAST ability
    private String heroClass;
    private int experience;

    /**
     * Creates a hero with all base stats supplied.
     * Defaults to WARRIOR class until setHeroClass is called.
     */
    public Hero(String name, int level, int attack, int defense, int maxHp, int maxMana) {
        super(name, level, attack, defense, maxHp, maxMana);
        this.heroClass = "WARRIOR";
        this.experience = 0;
    }

    public String getHeroClass()            { return heroClass; }
    public void setHeroClass(String c)      { this.heroClass = c; }
    public int getExperience()              { return experience; }
    public void addExperience(int xp)       { this.experience += xp; }
}
