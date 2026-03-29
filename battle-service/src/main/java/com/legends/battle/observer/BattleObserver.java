package game.battle.observer;
import game.battle.Action;
import game.battle.Unit;
/** Observer pattern: Battle publishes turn/damage/end events to subscribers (e.g. BattleGUI). */
public interface BattleObserver {
    default void onTurnStart(Unit unit) {}
    default void onAction(Unit actor, Action action) {}
    default void onDamage(Unit attacker, Unit target, int damage) {}
    default void onBattleEnd(String winner) {}
    void onAbilityResult(Unit actor, String result);
}
