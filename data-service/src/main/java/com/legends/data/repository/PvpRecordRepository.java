package com.legends.data.repository;

import com.legends.data.model.PvpRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PvpRecordRepository extends JpaRepository<PvpRecord, Long> {
    Optional<PvpRecord> findByUserId(Long userId);
    Optional<PvpRecord> findByUsername(String username);
}
