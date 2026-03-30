package com.legends.data.repository;

import com.legends.data.model.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    /** In-progress campaign for save/load. */
    Optional<Party> findByUserIdAndActiveCampaignTrue(Long userId);

    /** All parties for a user — PvP selection and party-count checks. */
    List<Party> findByUserId(Long userId);

    /** Count parties — enforce the 5-party cap. */
    int countByUserId(Long userId);

    /** Look up a specific named party by userId — used when loading PvP heroes. */
    Optional<Party> findByUserIdAndPartyName(Long userId, String partyName);
}
