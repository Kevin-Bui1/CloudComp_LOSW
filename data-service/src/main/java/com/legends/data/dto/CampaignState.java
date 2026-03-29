package com.legends.data.dto;

import java.util.List;
public class CampaignState {
    private Long partyId;
    private Long userId;
    private String partyName;
    private int currentRoom;
    private int gold;
    private List<HeroDTO> heroes;

    public Long getPartyId()                    { return partyId; }
    public void setPartyId(Long partyId)        { this.partyId = partyId; }
    public Long getUserId()                     { return userId; }
    public void setUserId(Long userId)          { this.userId = userId; }
    public String getPartyName()                { return partyName; }
    public void setPartyName(String n)          { this.partyName = n; }
    public int getCurrentRoom()                 { return currentRoom; }
    public void setCurrentRoom(int r)           { this.currentRoom = r; }
    public int getGold()                        { return gold; }
    public void setGold(int gold)               { this.gold = gold; }
    public List<HeroDTO> getHeroes()            { return heroes; }
    public void setHeroes(List<HeroDTO> h)      { this.heroes = h; }
}
