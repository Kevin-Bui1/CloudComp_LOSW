package com.legends.pve.model;

/**
 * InnRoom is the other room type. When entered, every hero in the party is
 * fully healed and their mana is completely restored.
 *
 * The assignment also mentions item shops and hero recruitment happening at inns,
 * but those features are planned for Deliverable 2. For D1, the inn just heals.
 */
public class InnRoom extends Room {

    public InnRoom(int floor) { super(floor); }

    /**
     * Restores all heroes to full HP and mana.
     * Returns a summary of how much each hero recovered.
     */
    @Override
    public String enter(Party party) {
        StringBuilder sb = new StringBuilder("Welcome to the Inn! ");
        for (Hero h : party.getHeroes()) {
            int hpRestored   = h.getMaxHp()  - h.getHp();
            int manaRestored = h.getMaxMana() - h.getMana();
            h.setHp(h.getMaxHp());
            h.setMana(h.getMaxMana());
            sb.append(h.getName())
              .append(" restored ").append(hpRestored).append(" HP and ")
              .append(manaRestored).append(" mana. ");
        }
        return sb.toString().trim();
    }
}
