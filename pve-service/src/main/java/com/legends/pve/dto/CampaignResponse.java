package com.legends.pve.dto;

import com.legends.pve.model.Campaign;
import com.legends.pve.model.Enemy;
import java.util.List;

/** Full campaign state response returned to the client. */
public class CampaignResponse {
    private boolean success;
    private String message;
    private String roomType;   // "BATTLE", "INN", or null
    private int currentRoom;
    private int gold;
    private int cumulativeLevel;
    private List<HeroRequest> heroes;
    private List<Enemy> enemies;   // populated only for BATTLE rooms
    private int expReward;
    private int goldReward;

    public static CampaignResponse of(Campaign c, String message, String roomType) {
        CampaignResponse r = new CampaignResponse();
        r.success = true;
        r.message = message;
        r.roomType = roomType;
        r.currentRoom = c.getCurrentRoom();
        r.gold = c.getParty().getGold();
        r.cumulativeLevel = c.getParty().getCumulativeLevel();
        return r;
    }

    public static CampaignResponse error(String message) {
        CampaignResponse r = new CampaignResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess()                      { return success; }
    public String getMessage()                      { return message; }
    public String getRoomType()                     { return roomType; }
    public int getCurrentRoom()                     { return currentRoom; }
    public int getGold()                            { return gold; }
    public int getCumulativeLevel()                 { return cumulativeLevel; }
    public List<HeroRequest> getHeroes()            { return heroes; }
    public void setHeroes(List<HeroRequest> h)      { this.heroes = h; }
    public List<Enemy> getEnemies()                 { return enemies; }
    public void setEnemies(List<Enemy> enemies)     { this.enemies = enemies; }
    public int getExpReward()                       { return expReward; }
    public void setExpReward(int expReward)         { this.expReward = expReward; }
    public int getGoldReward()                      { return goldReward; }
    public void setGoldReward(int goldReward)       { this.goldReward = goldReward; }
}
