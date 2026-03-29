package com.legends.battle.strategy.impl;
import com.legends.battle.Battle;
import com.legends.battle.Unit;
import com.legends.battle.strategy.BattleActionStrategy;
public class DefendStrategy implements BattleActionStrategy {
    @Override public void execute(Battle battle, Unit actor) { battle.doDefend(actor); }
}
