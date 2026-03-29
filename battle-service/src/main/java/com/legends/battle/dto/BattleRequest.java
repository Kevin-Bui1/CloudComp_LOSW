package com.legends.battle.dto;

import com.legends.battle.model.Action;
import java.util.List;

/** Request payload to start a new battle. */
public class BattleRequest {
    private List<UnitDTO> playerParty;
    private List<UnitDTO> enemyParty;

    public List<UnitDTO> getPlayerParty() { return playerParty; }
    public void setPlayerParty(List<UnitDTO> p) { this.playerParty = p; }
    public List<UnitDTO> getEnemyParty() { return enemyParty; }
    public void setEnemyParty(List<UnitDTO> e) { this.enemyParty = e; }
}
