package com.legends.pve.model;

/**
 * Room is an abstract class that both BattleRoom and InnRoom extend.
 *
 * I used an abstract class here because both room types share the "floor" field,
 * but what happens when you enter them is completely different. The abstract
 * enter() method forces each subclass to define its own behaviour.
 *
 * This is the polymorphism approach from OOP — PveController can call
 * room.enter(party) without knowing if it's a battle or an inn. The right
 * version of enter() runs automatically based on the actual type.
 */
public abstract class Room {

    protected int floor;

    public Room(int floor) { this.floor = floor; }

    /**
     * What happens when the party steps into this room.
     * Returns a description string that gets sent back to the client.
     */
    public abstract String enter(Party party);

    public int getFloor() { return floor; }
}
