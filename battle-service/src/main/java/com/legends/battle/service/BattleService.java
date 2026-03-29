package com.legends.battle.service;

import com.legends.battle.dto.BattleRequest;
import com.legends.battle.dto.BattleResponse;
import com.legends.battle.dto.UnitDTO;
import com.legends.battle.model.Action;
import com.legends.battle.model.Enemy;
import com.legends.battle.model.Hero;
import com.legends.battle.model.Unit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BattleService is the glue between the REST controller and the Battle engine.
 *
 * The microservice requirement says services must be stateless from the client's
 * perspective, but we still need to track battle state between requests (you can't
 * re-send the whole battle each time). I solved this with an in-memory map:
 *
 *   battleId (String) --> Battle instance
 *
 * ConcurrentHashMap is used instead of HashMap because multiple requests could
 * hit the server at the same time and we don't want them stepping on each other.
 *
 * When a battle ends, the entry is removed to free memory.
 */
@Service
public class BattleService {

    /** Stores active battle sessions. Key = battleId chosen by the caller. */
    private final ConcurrentHashMap<String, Battle> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a new battle session, builds the turn queue, and returns the initial state.
     *
     * @param battleId  a unique string to identify this battle (e.g. "user1_battle")
     * @param request   contains both parties as lists of UnitDTOs
     */
    public BattleResponse startBattle(String battleId, BattleRequest request) {
        Battle battle = new Battle();
        battle.init(mapToUnits(request.getPlayerParty(), true),
                    mapToUnits(request.getEnemyParty(), false));
        sessions.put(battleId, battle);
        return buildResponse(battle, "Battle started! Turn order initialised.");
    }

    /**
     * Executes one action in an existing battle and returns the updated state.
     *
     * @param battleId  must match a key in the sessions map
     * @param action    what the current unit is doing this turn
     */
    public BattleResponse executeAction(String battleId, Action action) {
        Battle battle = sessions.get(battleId);
        if (battle == null) {
            BattleResponse err = new BattleResponse();
            err.setActionResult("Battle session not found: " + battleId);
            return err;
        }
        String result = battle.processTurn(action);
        BattleResponse response = buildResponse(battle, result);
        if (battle.isBattleOverFlag()) {
            sessions.remove(battleId); // clean up ended sessions
        }
        return response;
    }

    // -- Private helpers -----------------------------------------------------

    /**
     * Converts a list of UnitDTOs (from JSON) into proper Unit objects.
     * isHero=true creates Hero instances, false creates Enemy instances.
     */
    private List<Unit> mapToUnits(List<UnitDTO> dtos, boolean isHero) {
        return dtos.stream().map(dto -> {
            Unit u;
            if (isHero) {
                Hero h = new Hero(dto.getName(), dto.getLevel(), dto.getAttack(),
                        dto.getDefense(), dto.getMaxHp(), dto.getMaxMana());
                if (dto.getHeroClass() != null) h.setHeroClass(dto.getHeroClass());
                h.setHp(dto.getHp());
                h.setMana(dto.getMana());
                u = h;
            } else {
                u = new Enemy(dto.getName(), dto.getLevel(), dto.getAttack(),
                        dto.getDefense(), dto.getMaxHp(), dto.getMaxMana());
                u.setHp(dto.getHp());
                u.setMana(dto.getMana());
            }
            u.setStunned(dto.isStunned());
            return u;
        }).collect(java.util.stream.Collectors.toList());
    }

    /** Converts a Unit back into a DTO so it can be serialized to JSON. */
    private UnitDTO toDTO(Unit u) {
        UnitDTO dto = new UnitDTO();
        dto.setName(u.getName());
        dto.setLevel(u.getLevel());
        dto.setAttack(u.getAttack());
        dto.setDefense(u.getDefense());
        dto.setHp(u.getHp());
        dto.setMaxHp(u.getMaxHp());
        dto.setMana(u.getMana());
        dto.setMaxMana(u.getMaxMana());
        dto.setStunned(u.isStunned());
        dto.setAlive(u.isAlive());
        if (u instanceof Hero h) dto.setHeroClass(h.getHeroClass());
        return dto;
    }

    /** Packages the current battle state into a response object. */
    private BattleResponse buildResponse(Battle battle, String result) {
        BattleResponse response = new BattleResponse();
        response.setActionResult(result);
        response.setPlayerParty(battle.getPlayerParty().stream().map(this::toDTO).toList());
        response.setEnemyParty(battle.getEnemyParty().stream().map(this::toDTO).toList());
        response.setBattleOver(battle.isBattleOverFlag());
        response.setWinner(battle.getWinner());
        return response;
    }
}
