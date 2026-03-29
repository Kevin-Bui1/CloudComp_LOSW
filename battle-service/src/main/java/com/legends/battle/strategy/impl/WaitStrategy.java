package game.battle.strategy.impl;
import game.battle.Battle;
import game.battle.Unit;
import game.battle.strategy.BattleActionStrategy;
/** WAIT: no-op here — Battle.executeAction re-queues to waitQueue. */
public class WaitStrategy implements BattleActionStrategy {
    @Override public void execute(Battle battle, Unit actor) {}
}
