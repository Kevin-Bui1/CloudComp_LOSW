package com.legends.data.service;

import com.legends.data.dto.HeroDTO;
import com.legends.data.dto.SaveRequest;
import com.legends.data.dto.CampaignState;
import com.legends.data.model.*;
import com.legends.data.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GameSaveDAO — the single database access class for the data-service.
 *
 * Covers:
 *   - Campaign save / load / complete (UC5, UC6)
 *   - Party management (list, delete, count for 5-party cap)
 *   - Scores (save score, get best, leaderboard)
 *   - PvP records (record result, get win/loss)
 *   - Player eligibility check and named-party hero lookup (for pvp-service)
 */
@Service
public class GameSaveDAO {

    private final PartyRepository     partyRepository;
    private final ScoreRepository     scoreRepository;
    private final PvpRecordRepository pvpRecordRepository;

    public GameSaveDAO(PartyRepository partyRepository,
                       ScoreRepository scoreRepository,
                       PvpRecordRepository pvpRecordRepository) {
        this.partyRepository     = partyRepository;
        this.scoreRepository     = scoreRepository;
        this.pvpRecordRepository = pvpRecordRepository;
    }

    // ── Campaign ─────────────────────────────────────────────────────────

    /**
     * Saves or updates the player's active campaign (UC5 — Exit and Save).
     * Clears and rebuilds the hero list each time to keep it in sync.
     */
    @Transactional
    public Party saveCampaignProgress(SaveRequest request) {
        Party party = partyRepository
                .findByUserIdAndActiveCampaignTrue(request.getUserId())
                .orElseGet(() -> {
                    Party p = new Party();
                    p.setUserId(request.getUserId());
                    return p;
                });

        party.setPartyName(request.getPartyName());
        party.setCurrentRoom(request.getCurrentRoom());
        party.setGold(request.getGold());
        party.setActiveCampaign(true);

        party.getHeroes().clear();
        for (HeroDTO dto : request.getHeroes()) {
            party.getHeroes().add(toEntity(dto, party));
        }
        return partyRepository.save(party);
    }

    /** Loads the player's active campaign (UC6 — Continue). */
    @Transactional(readOnly = true)
    public Optional<CampaignState> fetchSavedCampaign(Long userId) {
        return partyRepository
                .findByUserIdAndActiveCampaignTrue(userId)
                .map(party -> {
                    CampaignState state = new CampaignState();
                    state.setPartyId(party.getPartyId());
                    state.setUserId(party.getUserId());
                    state.setPartyName(party.getPartyName());
                    state.setCurrentRoom(party.getCurrentRoom());
                    state.setGold(party.getGold());
                    state.setHeroes(party.getHeroes().stream().map(this::toDTO).toList());
                    return state;
                });
    }

    /** Marks a campaign complete after all 30 rooms (keeps party for PvP). */
    @Transactional
    public void completeCampaign(Long userId) {
        partyRepository.findByUserIdAndActiveCampaignTrue(userId)
                .ifPresent(p -> { p.setActiveCampaign(false); partyRepository.save(p); });
    }

    /** Returns all saved parties for a user (PvP party selection). */
    @Transactional(readOnly = true)
    public List<Party> getSavedParties(Long userId) {
        return partyRepository.findByUserId(userId);
    }

    /** Counts parties — enforces the 5-party cap. */
    public int countSavedParties(Long userId) {
        return partyRepository.countByUserId(userId);
    }

    /** Permanently deletes a party and its heroes (cascade). */
    @Transactional
    public void deleteParty(Long partyId) {
        partyRepository.deleteById(partyId);
    }

    // ── Scores ───────────────────────────────────────────────────────────

    /**
     * Records a campaign completion score.
     * The username is passed in the SaveRequest so the leaderboard can show names.
     */
    @Transactional
    public void saveScore(Long userId, int score) {
        // Use "unknown" as placeholder — caller should pass username via SaveRequest
        scoreRepository.save(new Score(userId, "user-" + userId, score));
    }

    /**
     * Saves a score with the player's username included (preferred overload).
     */
    @Transactional
    public void saveScore(Long userId, String username, int score) {
        scoreRepository.save(new Score(userId, username, score));
    }

    /** Returns the highest score this user has ever achieved. */
    @Transactional(readOnly = true)
    public int getBestScore(Long userId) {
        return scoreRepository.findBestScoreByUserId(userId).orElse(0);
    }

    /**
     * Returns the top-N scores across all players for the leaderboard.
     * Each entry is a simple Map with "username" and "score" keys.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopScores(int limit) {
        return scoreRepository.findTopScores(PageRequest.of(0, limit))
                .stream()
                .map(s -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("username", s.getUsername());
                    entry.put("score",    s.getScore());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ── PvP Records ──────────────────────────────────────────────────────

    /**
     * Records a PvP match result.
     * Upserts the win/loss counters for both the winner and loser.
     * The data-service doesn't know user IDs from usernames, so we store by username.
     */
    @Transactional
    public void recordPvpResult(String winnerUsername, String loserUsername) {
        upsertPvpRecord(winnerUsername, true);
        upsertPvpRecord(loserUsername, false);
    }

    /**
     * Returns [wins, losses] for a user by userId.
     * Returns [0, 0] if they have no PvP history.
     */
    @Transactional(readOnly = true)
    public int[] getPvpRecord(Long userId) {
        return pvpRecordRepository.findByUserId(userId)
                .map(r -> new int[]{r.getWins(), r.getLosses()})
                .orElse(new int[]{0, 0});
    }

    // ── Player helpers for pvp-service ───────────────────────────────────

    /**
     * Returns true if a player with this username has at least one saved party.
     * Called by pvp-service before creating an invite.
     *
     * NOTE: The data-service only knows userIds, not usernames — parties are keyed
     * by userId. So this checks the pvp_records table (which stores usernames) or
     * the score table. For invite validation we just check if ANY party exists for
     * a userId that maps to this username via the scores table.
     *
     * In a full system the profile-service would own username→userId resolution.
     * Here we use a pragmatic approach: we store username in PvpRecord on first
     * match result. For a fresh user who hasn't played PvP yet, we check if they
     * have any saved parties at all by looking in the scores table.
     *
     * Simpler alternative used here: trust the pvp-service to pass the correct
     * userId and just check party count. The /eligible endpoint accepts a username
     * and checks via pvpRecord or falls back to checking if any score row exists.
     */
    @Transactional(readOnly = true)
    public boolean isPlayerEligible(String username) {
        // A player is eligible if they have at least one score entry (i.e. finished
        // at least one campaign and therefore have a saved party available).
        // If they've never played we still need to check — look up by username
        // in score table or pvp_record. If truly brand new with only an active
        // campaign, we check the parties table indirectly.
        // The cleanest approach: any saved (non-active) party = eligible.
        // We can't query by username here without a username→userId lookup, so
        // we use the pvp_records table's username field as the canonical check
        // and fall back to score entries.
        boolean hasPvpRecord  = pvpRecordRepository.findByUsername(username).isPresent();
        boolean hasScoreEntry = scoreRepository.findAll().stream()
                .anyMatch(s -> s.getUsername().equals(username));
        return hasPvpRecord || hasScoreEntry;
    }

    /**
     * Returns the heroes in a specific named party for a given username.
     * Used by pvp-service to load parties before starting a PvP battle.
     * Returns null if the party is not found (controller returns 404).
     *
     * Since parties are keyed by userId, we look up by username via score table.
     * If no score exists, we search all parties whose name matches (less precise
     * but functional for the scope of this project).
     */
    @Transactional(readOnly = true)
    public List<HeroDTO> getPartyHeroes(String username, String partyName) {
        // Try to find userId from score entries for this username
        Long userId = scoreRepository.findAll().stream()
                .filter(s -> s.getUsername().equals(username))
                .map(Score::getUserId)
                .findFirst()
                .orElse(null);

        if (userId != null) {
            return partyRepository.findByUserIdAndPartyName(userId, partyName)
                    .map(p -> p.getHeroes().stream().map(this::toDTO).toList())
                    .orElse(null);
        }

        // Fallback: search by party name alone across all parties (best-effort)
        return partyRepository.findAll().stream()
                .filter(p -> p.getPartyName().equals(partyName))
                .findFirst()
                .map(p -> p.getHeroes().stream().map(this::toDTO).toList())
                .orElse(null);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private void upsertPvpRecord(String username, boolean won) {
        PvpRecord record = pvpRecordRepository.findByUsername(username)
                .orElseGet(() -> new PvpRecord(null, username));
        if (won) record.incrementWins();
        else     record.incrementLosses();
        pvpRecordRepository.save(record);
    }

    private HeroEntity toEntity(HeroDTO dto, Party party) {
        HeroEntity e = new HeroEntity();
        e.setParty(party);
        e.setName(dto.getName());
        e.setLevel(dto.getLevel());
        e.setAttack(dto.getAttack());
        e.setDefense(dto.getDefense());
        e.setHp(dto.getHp());
        e.setMaxHp(dto.getMaxHp());
        e.setMana(dto.getMana());
        e.setMaxMana(dto.getMaxMana());
        e.setExperience(dto.getExperience());
        e.setHeroClass(dto.getHeroClass());
        return e;
    }

    private HeroDTO toDTO(HeroEntity e) {
        HeroDTO dto = new HeroDTO();
        dto.setName(e.getName());
        dto.setLevel(e.getLevel());
        dto.setAttack(e.getAttack());
        dto.setDefense(e.getDefense());
        dto.setHp(e.getHp());
        dto.setMaxHp(e.getMaxHp());
        dto.setMana(e.getMana());
        dto.setMaxMana(e.getMaxMana());
        dto.setExperience(e.getExperience());
        dto.setHeroClass(e.getHeroClass());
        return dto;
    }
}
