/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipmentReservationRepository extends JpaRepository<EquipmentReservation, Long> {

    Optional<EquipmentReservation> findByPublicId(UUID publicId);

    // Find overlapping reservations for a specific equipment item, excluding canceled ones
    @Query(
            "SELECT er FROM EquipmentReservation er WHERE er.equipment.id = :equipmentId "
                    + "AND er.status <> com.univers.univers_backend.Enum.Status.CANCELED "
                    + "AND er.startTime < :endTime AND er.endTime > :startTime")
    List<EquipmentReservation> findOverlappingReservations(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    List<EquipmentReservation> findByEquipment(Equipment equipment);

    List<EquipmentReservation> findByStatus(Status status);

    List<EquipmentReservation> findByRequestingUser(User requestingUser);

    @Query(
            "SELECT er FROM EquipmentReservation er WHERE er.status = 'PENDING' AND"
                    + " er.equipment.equipmentOwner.id = :equipmentOwnerId")
    List<EquipmentReservation> findPendingReservationsForEquipmentOwner(
            @Param("equipmentOwnerId") Long equipmentOwnerId);

    @Query(
            "SELECT er FROM EquipmentReservation er WHERE er.equipment.equipmentOwner.id ="
                    + " :equipmentOwnerId")
    List<EquipmentReservation> findAllReservationsForEquipmentOwner(
            @Param("equipmentOwnerId") Long equipmentOwnerId);

    List<EquipmentReservation> findByEvent_Id(Long eventId);

    List<EquipmentReservation> findByEvent_PublicId(UUID eventPublicId);
}
