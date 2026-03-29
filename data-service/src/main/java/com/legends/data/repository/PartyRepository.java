package com.legends.data.repository;

import com.legends.data.model.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * PartyRepository is the database access layer for the parties table.
 *
 * Like UserRepository in the profile service, Spring Data JPA generates the SQL
 * automatically from the method names. I only declare the custom queries I need.
 *
 * findByUserIdAndActiveCampaignTrue is the key one — it finds the campaign the
 * player is currently playing (as opposed to completed ones saved for PvP).
 */
public interface PartyRepository extends JpaRepository<Party, Long> {

    /** Used by GameSaveDAO to load or update the current in-progress campaign. */
    Optional<Party> findByUserIdAndActiveCampaignTrue(Long userId);

    /** Returns all parties for a user — needed for PvP party selection. */
    List<Party> findByUserId(Long userId);

    /** Counts a user's parties — used to enforce the max-5 limit. */
    int countByUserId(Long userId);
}
