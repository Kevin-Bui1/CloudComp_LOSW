package game.battle;

public abstract class Unit {
    protected String name;
    protected int level, attack, defense, hp, maxHp, mana, maxMana;
    protected boolean stunned;

    public Unit(String name, int level, int attack, int defense, int maxHp, int maxMana) {
        this.name = name; this.level = level; this.attack = attack;
        this.defense = defense; this.maxHp = maxHp; this.maxMana = maxMana;
        this.hp = maxHp; this.mana = maxMana;
    }

    public boolean isAlive()   { return hp > 0; }
    public boolean isStunned() { return stunned; }
    public void setStunned(boolean s) { stunned = s; }

    public void takeDamage(int d)    { hp = Math.max(0, hp - Math.max(0, d)); }
    public void restoreHp(int a)     { if (a > 0) hp = Math.min(maxHp, hp + a); }
    public void restoreMana(int a)   { if (a > 0) mana = Math.min(maxMana, mana + a); }
    public boolean spendMana(int a)  { if (a < 0 || mana < a) return false; mana -= a; return true; }
    public void setHp(int v)         { hp = Math.max(0, Math.min(v, maxHp)); }
    public void setMana(int v)       { mana = Math.max(0, Math.min(v, maxMana)); }

    public String getName()   { return name; }
    public int getLevel()     { return level; }
    public int getAttack()    { return attack; }
    public int getDefense()   { return defense; }
    public int getHp()        { return hp; }
    public int getMaxHp()     { return maxHp; }
    public int getMana()      { return mana; }
    public int getMaxMana()   { return maxMana; }
}
