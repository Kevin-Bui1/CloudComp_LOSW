package com.legends.data.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Party is the JPA entity for the "parties" table in MySQL.
 *
 * Each row is one saved party belonging to one user. The activeCampaign flag
 * distinguishes a campaign in progress (true) from a completed one saved for PvP (false).
 *
 * The heroes list uses @OneToMany with cascade and orphanRemoval:
 *  - CascadeType.ALL means saving/deleting a party automatically saves/deletes its heroes
 *  - orphanRemoval=true means if I remove a hero from the list and save, it gets deleted from the DB
 *
 * This is why GameSaveDAO can safely do party.getHeroes().clear() before re-adding them.
 */
@Entity
@Table(name = "parties")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long partyId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String partyName;

    private int currentRoom = 0;
    private int gold        = 0;
    private boolean activeCampaign = true;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HeroEntity> heroes = new ArrayList<>();

    public Party() {}

    public Long getPartyId()                    { return partyId; }
    public Long getUserId()                     { return userId; }
    public void setUserId(Long id)              { this.userId = id; }
    public String getPartyName()                { return partyName; }
    public void setPartyName(String n)          { this.partyName = n; }
    public int getCurrentRoom()                 { return currentRoom; }
    public void setCurrentRoom(int r)           { this.currentRoom = r; }
    public int getGold()                        { return gold; }
    public void setGold(int gold)               { this.gold = gold; }
    public boolean isActiveCampaign()           { return activeCampaign; }
    public void setActiveCampaign(boolean a)    { this.activeCampaign = a; }
    public List<HeroEntity> getHeroes()         { return heroes; }
    public void setHeroes(List<HeroEntity> h)   { this.heroes = h; }
}
