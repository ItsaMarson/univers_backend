/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Status;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentReservationRepository extends JpaRepository<EquipmentReservation, Long> {

    Optional<EquipmentReservation> findByPublicId(UUID publicId);

    @Query(
            "SELECT er FROM EquipmentReservation er WHERE er.equipment.id = :equipmentId "
                    + "AND er.status <> com.univers.univers_backend.Enum.Status.CANCELED "
                    + "AND er.startTime < :endTime AND er.endTime > :startTime")
    List<EquipmentReservation> findOverlappingReservations(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

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

    boolean existsByDepartment(Department department);

    List<EquipmentReservation> findByEquipmentAndStatusAndEndTimeAfterAndStartTimeBefore(
            Equipment equipment, Status status, Instant startTime, Instant endTime);

    @Query(
            "SELECT er.equipment.name AS equipmentName, COUNT(er) AS reservationCount FROM"
                + " EquipmentReservation er WHERE er.startTime >= :startDate AND er.startTime <="
                + " :endDate AND (:equipmentTypeName IS NULL OR LOWER(er.equipment.name) LIKE"
                + " LOWER(CONCAT('%', :equipmentTypeName, '%'))) GROUP BY er.equipment.name ORDER"
                + " BY reservationCount DESC")
    List<Object[]> findTopEquipmentByReservationCount(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("equipmentTypeName") String equipmentTypeName,
            Pageable pageable);
}
