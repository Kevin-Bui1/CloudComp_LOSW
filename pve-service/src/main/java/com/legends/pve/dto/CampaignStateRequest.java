package com.legends.pve.dto;

import java.util.List;
import java.util.Map;

/**
 * The full campaign state sent by the client with every mutating request.
 *
 * Because the pve-service is stateless, the client must echo back the
 * campaign state it received from the previous response. The service
 * reconstructs the in-memory Campaign/Party from this payload, applies
 * the requested operation, and returns the updated state.
 */
public class CampaignStateRequest {
    private Long   userId;
    private int    currentRoom;
    private Integer gold;
    private List<HeroRequest>        heroes;
    private Map<String, Integer>     inventory;

    public Long    getUserId()                              { return userId; }
    public void    setUserId(Long userId)                   { this.userId = userId; }
    public int     getCurrentRoom()                         { return currentRoom; }
    public void    setCurrentRoom(int currentRoom)          { this.currentRoom = currentRoom; }
    public Integer getGold()                                { return gold; }
    public void    setGold(Integer gold)                    { this.gold = gold; }
    public List<HeroRequest> getHeroes()                    { return heroes; }
    public void    setHeroes(List<HeroRequest> heroes)      { this.heroes = heroes; }
    public Map<String, Integer> getInventory()              { return inventory; }
    public void    setInventory(Map<String, Integer> inv)   { this.inventory = inv; }
}
