package game.campaign;
import game.party.Party;
/**
 * factory-created for each step of the 30-room campaign.
 * execute() MUST be called from a background thread (SwingWorker), not the EDT,
 * because BattleRoom and InnRoom block until the player finishes interacting.
 */
public abstract class Room {
    protected final int floor;
    public Room(int floor) { this.floor = floor; }
    public abstract String execute(Party party);
    public int getFloor() { return floor; }
}
