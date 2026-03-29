package game.battle.strategy.impl;
import game.battle.Battle;
import game.battle.Unit;
import game.battle.strategy.BattleActionStrategy;
public class DefendStrategy implements BattleActionStrategy {
    @Override public void execute(Battle battle, Unit actor) { battle.doDefend(actor); }
}
