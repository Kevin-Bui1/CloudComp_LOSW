package game.campaign;

import db.GameSaveDAO;
import game.battle.Hero;
import game.party.Party;

import java.util.List;

public class Campaign {
    private final Party party;
    private int currentRoom;
    private final int maxRooms = 30;
    private boolean finished;
    private int lastInnRoom;
    private GameSaveDAO dao;
    private int userId;
    private String partyName;

    public Campaign(List<Hero> heroes) {
        this.party = new Party(heroes);
        this.lastInnRoom = 0;
    }

    public Campaign(List<Hero> heroes, GameSaveDAO dao, int userId, String partyName) {
        this(heroes);
        this.dao = dao;
        this.userId = userId;
        this.partyName = partyName;
    }

    public void start() {
        System.out.println("[Campaign] Started with " + party.getHeroes().size() + " heroes.");
    }

    public String nextRoom() {
        if (finished || currentRoom >= maxRooms) {
            finished = true;
            int score = calculateScore();
            saveScoreToDb(score);
            return "Campaign finished!\nFinal Score: " + score;
        }

        currentRoom++;
        Room room = RoomFactory.createRoom(currentRoom, party.getTotalLevel());
        String type = (room instanceof BattleRoom) ? "⚔ Battle Room" : "🏠 Inn Room";
        String header = "Room " + currentRoom + "/" + maxRooms + " — " + type + "\n";

        String result = room.execute(party);

        if (room instanceof game.inn.InnRoom) {
            lastInnRoom = currentRoom;
        } else if (room instanceof BattleRoom) {
            boolean partyWiped = party.getHeroes().stream().noneMatch(Hero::isAlive);
            if (partyWiped) {
                currentRoom = lastInnRoom;
                result += "\nParty retreated to last inn (Room " + lastInnRoom + ").";
            }
        }

        if (currentRoom >= maxRooms) {
            finished = true;
            int score = calculateScore();
            saveScoreToDb(score);
            return header + result + "\n\nCampaign complete! Final Score: " + score;
        }

        return header + result;
    }

    public String getPartyName() {
        return partyName;
    }

    public int calculateScore() {
        int s = 0;
        for (Hero h : party.getHeroes()) {
            s += h.getLevel() * 100;
            s += h.getItemPurchaseScore();
        }
        s += party.getGold() * 10;
        return s;
    }

    public void saveProgress() {
        if (dao == null) return;

        dao.saveCampaignProgress(
                userId,
                partyName != null ? partyName : "My Party",
                currentRoom,
                party.getGold(),
                new java.util.ArrayList<>(party.getHeroes()),
                party.getInventory()
        );
    }

    private void saveScoreToDb(int score) {
        if (dao != null) dao.saveScore(userId, score);
    }

    public int getCurrentRoom() {
        return currentRoom;
    }

    public int getMaxRooms() {
        return maxRooms;
    }

    public boolean isFinished() {
        return finished;
    }

    public Party getParty() {
        return party;
    }

    public void setCurrentRoom(int r) {
        this.currentRoom = Math.max(0, Math.min(r, maxRooms));
    }

    public void setGold(int g) {
        party.setGold(g);
    }

    public int getLastInnRoom() {
        return lastInnRoom;
    }

    public void setLastInnRoom(int r) {
        this.lastInnRoom = Math.max(0, Math.min(r, maxRooms));
    }
}