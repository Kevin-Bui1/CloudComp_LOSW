package com.legends.battle.strategy.impl;

import com.legends.battle.Battle;
import com.legends.battle.Hero;
import com.legends.battle.Unit;
import com.legends.battle.strategy.BattleActionStrategy;

import java.util.List;

public class CastStrategy implements BattleActionStrategy {
    @Override
    public void execute(Battle battle, Unit actor) {
        if (actor instanceof Hero hero) {
            List<Unit> allies = battle.getPlayerParty().contains(actor)
                    ? battle.getPlayerParty()
                    : battle.getEnemyParty();

            List<Unit> enemies = battle.getPlayerParty().contains(actor)
                    ? battle.getEnemyParty()
                    : battle.getPlayerParty();

            String result = hero.castAbility(allies, enemies);
            battle.notifyAbilityResult(actor, result);
        } else {
            new AttackStrategy().execute(battle, actor);
        }
    }
}