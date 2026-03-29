package game.battle.strategy.impl;
import game.battle.Battle;
import game.battle.Unit;
import game.battle.strategy.BattleActionStrategy;
import ui.battle.BattleGUI;
/** Respects BattleGUI.selectedTarget when set by the player's target picker. */
public class AttackStrategy implements BattleActionStrategy {
    @Override public void execute(Battle battle, Unit actor) {
        Unit chosen = BattleGUI.selectedTarget;
        if (chosen!=null&&chosen.isAlive()) battle.doAttackTarget(actor, chosen);
        else                                battle.doAttack(actor);
    }
}
