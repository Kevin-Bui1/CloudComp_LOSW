package com.legends.pve.dto;

/** Payload for POST /{userId}/battle/resolve — sent after a battle finishes. */
public class BattleResolveRequest {
    private CampaignStateRequest state;
    private boolean playerWon;
    private int expReward;
    private int goldReward;

    public CampaignStateRequest getState()          { return state; }
    public void setState(CampaignStateRequest s)    { this.state = s; }
    public boolean isPlayerWon()                    { return playerWon; }
    public void setPlayerWon(boolean playerWon)     { this.playerWon = playerWon; }
    public int getExpReward()                       { return expReward; }
    public void setExpReward(int expReward)         { this.expReward = expReward; }
    public int getGoldReward()                      { return goldReward; }
    public void setGoldReward(int goldReward)       { this.goldReward = goldReward; }
}
