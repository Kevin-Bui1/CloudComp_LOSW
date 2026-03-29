package com.legends.pve.model;

/**
 * Campaign tracks the overall state of a single dungeon run.
 *
 * It's a simple wrapper around the party and the current room counter.
 * The max is 30 rooms as specified in the assignment. Once currentRoom
 * hits 30, isComplete() returns true and the score gets calculated.
 */
public class Campaign {

    private Party party;
    private int currentRoom = 0;
    private static final int MAX_ROOMS = 30;

    public Campaign(Party party) {
        this.party = party;
    }

    /** Returns the campaign start message listing all party members. */
    public String start() {
        return "Campaign started! Your party: " + party.getHeroes().stream()
                .map(h -> h.getName() + " (Lvl " + h.getLevel() + ")")
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
    }

    public Party getParty() { return party; }

    public int getCurrentRoom()             { return currentRoom; }
    public void setCurrentRoom(int room)    { this.currentRoom = room; }
    public void advanceRoom()               { currentRoom++; }
    public boolean isComplete()             { return currentRoom >= MAX_ROOMS; }
    public int getMaxRooms()                { return MAX_ROOMS; }
}
