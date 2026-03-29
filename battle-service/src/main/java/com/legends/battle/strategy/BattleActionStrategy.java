package com.legends.battle.strategy;
import com.legends.battle.Battle;
import com.legends.battle.Unit;
/** Strategy pattern: encapsulates a single battle action. */
public interface BattleActionStrategy { void execute(Battle battle, Unit actor); }
