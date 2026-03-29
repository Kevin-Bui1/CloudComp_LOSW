package com.legends.battle.strategy.impl;
import com.legends.battle.Battle;
import com.legends.battle.Unit;
import com.legends.battle.strategy.BattleActionStrategy;
import ui.battle.BattleGUI;
/** Respects BattleGUI.selectedTarget when set by the player's target picker. */
public class AttackStrategy implements BattleActionStrategy {
    @Override public void execute(Battle battle, Unit actor) {
        Unit chosen = BattleGUI.selectedTarget;
        if (chosen!=null&&chosen.isAlive()) battle.doAttackTarget(actor, chosen);
        else                                battle.doAttack(actor);
    }
}
