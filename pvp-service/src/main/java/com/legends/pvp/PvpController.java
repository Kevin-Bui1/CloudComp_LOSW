package com.legends.pvp;

import db.GameSaveDAO;
import com.legends.battle.Hero;
import com.legends.battle.Unit;
import ui.battle.BattleGUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PvpController {

    private final GameSaveDAO dao;
    private final PvpManager pvpManager;

    public PvpController(GameSaveDAO dao, PvpManager pvpManager) {
        this.dao = dao;
        this.pvpManager = pvpManager;
    }

    public boolean canStartBattle(String playerUsername, String opponentUsername,
                                  String playerPartyName, String opponentPartyName) {
        if (playerUsername == null || playerUsername.isBlank()) return false;
        if (opponentUsername == null || opponentUsername.isBlank()) return false;
        if (playerPartyName == null || playerPartyName.isBlank()) return false;
        if (opponentPartyName == null || opponentPartyName.isBlank()) return false;
        if (!pvpManager.canInvite(playerUsername, opponentUsername)) return false;

        int playerId = dao.getUserIdByUsername(playerUsername);
        int opponentId = dao.getUserIdByUsername(opponentUsername);

        if (playerId == -1 || opponentId == -1) return false;

        List<Hero> playerHeroes = dao.loadCampaignHeroes(playerId, playerPartyName);
        List<Hero> opponentHeroes = dao.loadCampaignHeroes(opponentId, opponentPartyName);

        return !playerHeroes.isEmpty() && !opponentHeroes.isEmpty();
    }

    public void startBattle(String playerUsername, String opponentUsername,
                            String playerPartyName, String opponentPartyName) {
        int playerId = dao.getUserIdByUsername(playerUsername);
        int opponentId = dao.getUserIdByUsername(opponentUsername);

        if (playerId == -1 || opponentId == -1) {
            JOptionPane.showMessageDialog(null, "Could not load one or both users.");
            return;
        }

        List<Hero> playerHeroes = dao.loadCampaignHeroes(playerId, playerPartyName);
        List<Hero> opponentHeroes = dao.loadCampaignHeroes(opponentId, opponentPartyName);

        if (playerHeroes.isEmpty()) {
            JOptionPane.showMessageDialog(null, playerUsername + " has no valid heroes in party " + playerPartyName + ".");
            return;
        }

        if (opponentHeroes.isEmpty()) {
            JOptionPane.showMessageDialog(null, opponentUsername + " has no valid heroes in party " + opponentPartyName + ".");
            return;
        }

        List<Unit> playerUnits = new ArrayList<>(playerHeroes);
        List<Unit> opponentUnits = new ArrayList<>(opponentHeroes);

        SwingUtilities.invokeLater(() -> {
            BattleGUI gui = new BattleGUI(
                    playerUnits,
                    opponentUnits,
                    () -> {
                        boolean playerWon = playerUnits.stream().anyMatch(Unit::isAlive);
                        if (playerWon) {
                            pvpManager.recordMatchResult(playerUsername, opponentUsername);
                        } else {
                            pvpManager.recordMatchResult(opponentUsername, playerUsername);
                        }
                    },
                    playerUsername,
                    opponentUsername
            );

            gui.setVisible(true);
        });
    }
}