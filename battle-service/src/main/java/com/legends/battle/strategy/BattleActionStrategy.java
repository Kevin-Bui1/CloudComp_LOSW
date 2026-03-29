package game.battle.strategy;
import game.battle.Battle;
import game.battle.Unit;
/** Strategy pattern: encapsulates a single battle action. */
public interface BattleActionStrategy { void execute(Battle battle, Unit actor); }
