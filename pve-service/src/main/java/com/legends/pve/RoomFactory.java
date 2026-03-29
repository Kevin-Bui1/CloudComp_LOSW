package game.campaign;

import game.inn.InnRoom;
import java.util.Random;

public class RoomFactory {
    public static Room createRoom(int floor, int totalPartyLevel) {
        int battleChance = Math.min(90, 60 + (totalPartyLevel / 10) * 3);
        return (new Random().nextInt(100) < battleChance)
                ? new BattleRoom(floor)
                : new InnRoom(floor);
    }
}