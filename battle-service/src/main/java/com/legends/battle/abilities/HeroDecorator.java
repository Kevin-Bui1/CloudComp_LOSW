package com.legends.battle.abilities;
import com.legends.battle.Hero;
import java.util.List;
import com.legends.battle.Unit;

public abstract class HeroDecorator extends Hero {
    protected final Hero inner;
    protected HeroDecorator(Hero inner) {
        super(inner.getName(), inner.getLevel(), inner.getAttack(), inner.getDefense(), inner.getMaxHp(), inner.getMaxMana());
        this.inner = inner;
    }
    @Override public boolean isAlive()            { return inner.isAlive(); }
    @Override public boolean isStunned()          { return inner.isStunned(); }
    @Override public void setStunned(boolean s)   { inner.setStunned(s); }
    @Override public void takeDamage(int d)        { inner.takeDamage(d); }
    @Override public void restoreHp(int a)         { inner.restoreHp(a); }
    @Override public void restoreMana(int a)       { inner.restoreMana(a); }
    @Override public boolean spendMana(int a)      { return inner.spendMana(a); }
    @Override public int getHp()                   { return inner.getHp(); }
    @Override public int getMana()                 { return inner.getMana(); }
    @Override public int getMaxHp()                { return inner.getMaxHp(); }
    @Override public int getMaxMana()              { return inner.getMaxMana(); }
    @Override public void setHp(int hp)            { inner.setHp(hp); }
    @Override public void setMana(int m)           { inner.setMana(m); }
    @Override public String getName()              { return inner.getName(); }
    @Override public int getLevel()                { return inner.getLevel(); }
    @Override public String castAbility(List<Unit> allies, List<Unit> enemies) { return inner.castAbility(allies, enemies); }
}
