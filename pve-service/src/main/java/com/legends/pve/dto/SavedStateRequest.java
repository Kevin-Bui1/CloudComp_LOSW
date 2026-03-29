package com.legends.pve.dto;

import java.util.List;

/** Payload containing a previously saved campaign state (from Data Service). */
public class SavedStateRequest {
    private int currentRoom;
    private int gold;
    private List<HeroRequest> heroes;

    public int getCurrentRoom()                 { return currentRoom; }
    public void setCurrentRoom(int r)           { this.currentRoom = r; }
    public int getGold()                        { return gold; }
    public void setGold(int gold)               { this.gold = gold; }
    public List<HeroRequest> getHeroes()        { return heroes; }
    public void setHeroes(List<HeroRequest> h)  { this.heroes = h; }
}
