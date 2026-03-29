package com.legends.data.service;

import com.legends.data.dto.HeroDTO;
import com.legends.data.dto.SaveRequest;
import com.legends.data.dto.CampaignState;
import com.legends.data.model.HeroEntity;
import com.legends.data.model.Party;
import com.legends.data.repository.PartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * GameSaveDAO handles all campaign persistence — this is the only class that
 * talks to the database in the data service.
 *
 * "DAO" stands for Data Access Object, which is just a pattern where you
 * put all your database logic in one place and hide it from the rest of the code.
 * The controller calls these methods without knowing any SQL or JPA details.
 *
 * The @Transactional annotations tell Spring to wrap each method in a DB transaction.
 * If something goes wrong halfway through a save, the whole thing gets rolled back
 * instead of leaving the database in a half-updated state.
 */
@Service
public class GameSaveDAO {

    private final PartyRepository partyRepository;

    public GameSaveDAO(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    /**
     * Saves or updates the player's current campaign state (UC5 — Exit and Save).
     *
     * I check if an active campaign already exists for this user:
     *  - If yes: update it in place (no duplicate)
     *  - If no:  create a new Party record
     *
     * The hero list is cleared and rebuilt each time to make sure it stays in sync.
     * This is safe because @OneToMany with orphanRemoval=true deletes old heroes
     * automatically when they're removed from the list.
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

        // Clear old heroes and replace with current state
        party.getHeroes().clear();
        for (HeroDTO dto : request.getHeroes()) {
            HeroEntity hero = toEntity(dto, party);
            party.getHeroes().add(hero);
        }

        return partyRepository.save(party);
    }

    /**
     * Loads the player's active campaign from the database (UC6 — Continue Campaign).
     *
     * Returns an Optional so the controller can handle the "no save found" case cleanly
     * by returning a 404 response instead of crashing.
     */
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
                    state.setHeroes(party.getHeroes().stream()
                            .map(this::toDTO)
                            .toList());
                    return state;
                });
    }

    /**
     * Marks a campaign as no longer active once the player finishes all 30 rooms.
     * The party record stays in the database for PvP use — it's just flagged as done.
     */
    @Transactional
    public void completeCampaign(Long userId) {
        partyRepository.findByUserIdAndActiveCampaignTrue(userId)
                .ifPresent(party -> {
                    party.setActiveCampaign(false);
                    partyRepository.save(party);
                });
    }

    /** Counts how many parties a user has saved (used to enforce the 5-party limit). */
    public int countSavedParties(Long userId) {
        return partyRepository.countByUserId(userId);
    }

    /** Returns all saved parties for a user — used to populate the PvP party selection screen. */
    public List<Party> getSavedParties(Long userId) {
        return partyRepository.findByUserId(userId);
    }

    /**
     * Permanently deletes a party. Used when the player already has 5 parties
     * and wants to save a new one — they pick which old one to replace.
     */
    @Transactional
    public void deleteParty(Long partyId) {
        partyRepository.deleteById(partyId);
    }

    // -- Mapping helpers -----------------------------------------------------

    /** Converts a HeroDTO (from the request JSON) into a JPA entity for the database. */
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

    /** Converts a JPA entity back into a DTO so it can be sent in the response JSON. */
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
