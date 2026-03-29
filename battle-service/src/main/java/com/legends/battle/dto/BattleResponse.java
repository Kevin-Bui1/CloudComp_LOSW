package com.legends.battle.dto;

import java.util.List;

/** Response returned after each battle action. */
public class BattleResponse {
    private String actionResult;
    private List<UnitDTO> playerParty;
    private List<UnitDTO> enemyParty;
    private boolean battleOver;
    private String winner;

    public String getActionResult()              { return actionResult; }
    public void setActionResult(String r)        { this.actionResult = r; }
    public List<UnitDTO> getPlayerParty()        { return playerParty; }
    public void setPlayerParty(List<UnitDTO> p)  { this.playerParty = p; }
    public List<UnitDTO> getEnemyParty()         { return enemyParty; }
    public void setEnemyParty(List<UnitDTO> e)   { this.enemyParty = e; }
    public boolean isBattleOver()                { return battleOver; }
    public void setBattleOver(boolean b)         { this.battleOver = b; }
    public String getWinner()                    { return winner; }
    public void setWinner(String winner)         { this.winner = winner; }
}
