package com.legends.battle.strategy.impl;
import com.legends.battle.Battle;
import com.legends.battle.Unit;
import com.legends.battle.strategy.BattleActionStrategy;
/** WAIT: no-op here — Battle.executeAction re-queues to waitQueue. */
public class WaitStrategy implements BattleActionStrategy {
    @Override public void execute(Battle battle, Unit actor) {}
}
