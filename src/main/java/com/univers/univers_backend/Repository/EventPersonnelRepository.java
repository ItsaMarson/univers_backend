package com.univers.univers_backend.Repository;


import com.univers.univers_backend.Entity.EventPersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventPersonnelRepository extends JpaRepository<EventPersonnel, Long> {
    Optional<EventPersonnel> findByPublicId(UUID publicId);
}
