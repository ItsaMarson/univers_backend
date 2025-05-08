/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.EquipmentApproval;
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentApprovalRepository extends JpaRepository<EquipmentApproval, Long> {

    Optional<EquipmentApproval> findByPublicId(UUID publicId);

    List<EquipmentApproval> findAllByEquipmentReservation(
            EquipmentReservation equipmentReservation);

    boolean existsByEquipmentReservationAndSignedByAndStatus(
            EquipmentReservation equipmentReservation, User signedBy, Status status);

    List<EquipmentApproval> findAllByEquipmentReservationAndStatus(
            EquipmentReservation equipmentReservation, Status status);
}
