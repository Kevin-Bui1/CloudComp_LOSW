package com.legends.pve.dto;

/** Payload for all inn action endpoints. Contains full campaign state + action params. */
public class InnActionRequest {
    private CampaignStateRequest state;
    private String itemName;
    private String heroName;
    private String heroClass;
    private Integer heroIndex;

    public CampaignStateRequest getState()          { return state; }
    public void setState(CampaignStateRequest s)    { this.state = s; }
    public String getItemName()                     { return itemName; }
    public void setItemName(String itemName)        { this.itemName = itemName; }
    public String getHeroName()                     { return heroName; }
    public void setHeroName(String heroName)        { this.heroName = heroName; }
    public String getHeroClass()                    { return heroClass; }
    public void setHeroClass(String heroClass)      { this.heroClass = heroClass; }
    public Integer getHeroIndex()                   { return heroIndex; }
    public void setHeroIndex(Integer heroIndex)     { this.heroIndex = heroIndex; }
}
