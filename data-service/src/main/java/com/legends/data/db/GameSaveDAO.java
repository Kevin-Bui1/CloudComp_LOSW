package db;

import util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameSaveDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseConnector.getInstance().getConnection();
    }

    // USER METHODS
    public void createUser(String username, String password) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO Users(username,password) VALUES(?,?)")) {
            s.setString(1, username);
            s.setString(2, PasswordUtil.hashPassword(password));
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean authenticate(String username, String password) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT password FROM Users WHERE username=?")) {
            s.setString(1, username);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("password");
                return stored.equals(PasswordUtil.hashPassword(password));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getUserIdByUsername(String username) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT user_id FROM Users WHERE username=?")) {
            s.setString(1, username);
            ResultSet rs = s.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // CAMPAIGN SAVE - USE OF AI
    public void saveCampaignProgress(int userId, String partyName, int room, int gold, List<game.battle.Unit> units) {
        saveCampaignProgress(userId, partyName, room, gold, units, new LinkedHashMap<>());
    }

    public void saveCampaignProgress(int userId, String partyName, int room, int gold,
                                     List<game.battle.Unit> units, Map<String, Integer> inventory) {
        deleteParty(userId, partyName);

        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO Parties(user_id,party_name,current_room,gold) VALUES(?,?,?,?)")) {
            s.setInt(1, userId);
            s.setString(2, partyName);
            s.setInt(3, room);
            s.setInt(4, gold);
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        saveHeroes(userId, partyName, units);
        saveInventory(userId, partyName, inventory);
    }

    private void saveHeroes(int userId, String partyName, List<game.battle.Unit> units) {
        String sql = """
            INSERT INTO PartyHeroes(
                user_id, party_name, hero_index, hero_name,
                level, attack, defense, hp, max_hp, mana, max_mana,
                active_class, hybrid_class, hybridized,
                order_level, chaos_level, warrior_level, mage_level,
                current_exp, item_purchase_score
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            int index = 0;
            for (game.battle.Unit u : units) {
                if (!(u instanceof game.battle.Hero h)) continue;

                s.setInt(1, userId);
                s.setString(2, partyName);
                s.setInt(3, index++);
                s.setString(4, h.getName());
                s.setInt(5, h.getLevel());
                s.setInt(6, h.getAttack());
                s.setInt(7, h.getDefense());
                s.setInt(8, h.getHp());
                s.setInt(9, h.getMaxHp());
                s.setInt(10, h.getMana());
                s.setInt(11, h.getMaxMana());
                s.setString(12, h.getActiveClass().name());
                s.setString(13, h.getHybridClass().name());
                s.setBoolean(14, h.isHybridized());
                s.setInt(15, h.getClassLevel(game.battle.Hero.HeroClass.ORDER));
                s.setInt(16, h.getClassLevel(game.battle.Hero.HeroClass.CHAOS));
                s.setInt(17, h.getClassLevel(game.battle.Hero.HeroClass.WARRIOR));
                s.setInt(18, h.getClassLevel(game.battle.Hero.HeroClass.MAGE));
                s.setInt(19, h.getCurrentExp());
                s.setInt(20, h.getItemPurchaseScore());
                s.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveInventory(int userId, String partyName, Map<String, Integer> inventory) {
        if (inventory == null || inventory.isEmpty()) return;

        String sql = """
            INSERT INTO PartyInventory(user_id, party_name, item_name, quantity)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                if (entry.getValue() == null || entry.getValue() <= 0) continue;

                s.setInt(1, userId);
                s.setString(2, partyName);
                s.setString(3, entry.getKey());
                s.setInt(4, entry.getValue());
                s.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int[] loadCampaignProgress(int userId, String partyName) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT current_room, gold FROM Parties WHERE user_id=? AND party_name=?")) {
            s.setInt(1, userId);
            s.setString(2, partyName);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                return new int[]{rs.getInt("current_room"), rs.getInt("gold")};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<game.battle.Hero> loadCampaignHeroes(int userId, String partyName) {
        List<game.battle.Hero> heroes = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT * FROM PartyHeroes WHERE user_id=? AND party_name=? ORDER BY hero_index")) {
            s.setInt(1, userId);
            s.setString(2, partyName);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                heroes.add(new game.battle.Hero(
                        rs.getString("hero_name"),
                        rs.getInt("level"), rs.getInt("attack"), rs.getInt("defense"),
                        rs.getInt("max_hp"), rs.getInt("max_mana"),
                        rs.getInt("hp"), rs.getInt("mana"),
                        game.battle.Hero.HeroClass.valueOf(rs.getString("active_class")),
                        game.battle.Hero.HybridClass.valueOf(rs.getString("hybrid_class")),
                        rs.getBoolean("hybridized"),
                        rs.getInt("order_level"), rs.getInt("chaos_level"),
                        rs.getInt("warrior_level"), rs.getInt("mage_level"),
                        rs.getInt("current_exp"), rs.getInt("item_purchase_score")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return heroes;
    }

    public Map<String, Integer> loadCampaignInventory(int userId, String partyName) {
        Map<String, Integer> inventory = new LinkedHashMap<>();

        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT item_name, quantity FROM PartyInventory WHERE user_id=? AND party_name=? ORDER BY item_name")) {
            s.setInt(1, userId);
            s.setString(2, partyName);
            ResultSet rs = s.executeQuery();

            while (rs.next()) {
                inventory.put(rs.getString("item_name"), rs.getInt("quantity"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return inventory;
    }

    public List<String> getSavedParties(int userId) {
        List<String> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT party_name FROM Parties WHERE user_id=? ORDER BY party_name")) {
            s.setInt(1, userId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) list.add(rs.getString("party_name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getIncompleteCampaigns(int userId) {
        List<String> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT party_name FROM Parties WHERE user_id=? AND current_room < 30 ORDER BY current_room DESC, party_name")) {
            s.setInt(1, userId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) list.add(rs.getString("party_name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void deleteParty(int userId, String partyName) {
        try (Connection c = getConnection()) {
            try (PreparedStatement deleteInventory = c.prepareStatement(
                    "DELETE FROM PartyInventory WHERE user_id=? AND party_name=?")) {
                deleteInventory.setInt(1, userId);
                deleteInventory.setString(2, partyName);
                deleteInventory.executeUpdate();
            }

            try (PreparedStatement deleteHeroes = c.prepareStatement(
                    "DELETE FROM PartyHeroes WHERE user_id=? AND party_name=?")) {
                deleteHeroes.setInt(1, userId);
                deleteHeroes.setString(2, partyName);
                deleteHeroes.executeUpdate();
            }

            try (PreparedStatement deleteParty = c.prepareStatement(
                    "DELETE FROM Parties WHERE user_id=? AND party_name=?")) {
                deleteParty.setInt(1, userId);
                deleteParty.setString(2, partyName);
                deleteParty.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getBestScore(int userId) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT MAX(score) AS best_score FROM Scores WHERE user_id=?")) {
            s.setInt(1, userId);
            ResultSet rs = s.executeQuery();
            if (rs.next()) return rs.getInt("best_score");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean hasSavedParty(int userId) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT 1 FROM Parties WHERE user_id=? LIMIT 1")) {
            s.setInt(1, userId);
            return s.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void recordPvpResult(String winnerUsername, String loserUsername) {
        int winnerId = getUserIdByUsername(winnerUsername);
        int loserId = getUserIdByUsername(loserUsername);

        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO PvpMatches(winner_id, loser_id) VALUES(?, ?)")) {
            s.setInt(1, winnerId);
            s.setInt(2, loserId);
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE Users SET wins = COALESCE(wins,0)+1 WHERE user_id=?")) {
            s.setInt(1, winnerId);
            s.executeUpdate();
        } catch (SQLException ignored) {
        }

        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE Users SET losses = COALESCE(losses,0)+1 WHERE user_id=?")) {
            s.setInt(1, loserId);
            s.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void saveScore(int userId, int score) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO Scores(user_id, score) VALUES(?, ?)")) {
            s.setInt(1, userId);
            s.setInt(2, score);
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTopScores(int limit) {
        List<String> results = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT U.username, S.score " +
                             "FROM Scores S JOIN Users U ON S.user_id = U.user_id " +
                             "ORDER BY S.score DESC LIMIT ?")) {
            s.setInt(1, limit);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("username") + " - " + rs.getInt("score"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    /** Load win/loss record. */
    public int[] getPvpRecord(int userId) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT " +
                             "(SELECT COUNT(*) FROM PvpMatches WHERE winner_id=?) AS wins, " +
                             "(SELECT COUNT(*) FROM PvpMatches WHERE loser_id=?) AS losses")) {
            s.setInt(1, userId);
            s.setInt(2, userId);
            ResultSet rs = s.executeQuery();
            if (rs.next()) return new int[]{rs.getInt("wins"), rs.getInt("losses")};
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new int[]{0, 0};
    }
}