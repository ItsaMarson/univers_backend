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
            "SELECT er.equipment.name AS equipmentName, er.status AS reservationStatus, COUNT(er)"
                + " AS statusCount FROM EquipmentReservation er WHERE er.startTime >= :startDate"
                + " AND er.startTime < :endDatePlusOne AND (:equipmentTypeName IS NULL OR"
                + " LOWER(er.equipment.name) LIKE LOWER(CONCAT('%', :equipmentTypeName, '%')))"
                + " GROUP BY er.equipment.name, er.status ORDER BY equipmentName ASC,"
                + " reservationStatus ASC")
    List<Object[]> findTopEquipmentByReservationCount(
            @Param("startDate") Instant startDate,
            @Param("endDatePlusOne") Instant endDatePlusOne,
            @Param("equipmentTypeName") String equipmentTypeName,
            Pageable pageable);

    @Query(
            "SELECT er.requestingUser.publicId AS userPublicId, er.requestingUser.firstName AS"
                + " userFirstName, er.requestingUser.lastName AS userLastName, er.status AS"
                + " reservationStatus, COUNT(er) AS statusCount FROM EquipmentReservation er WHERE"
                + " er.startTime >= :startDate AND er.startTime < :endDatePlusOne AND (:userFilter"
                + " IS NULL OR LOWER(er.requestingUser.firstName) LIKE LOWER(CONCAT('%',"
                + " :userFilter, '%')) OR LOWER(er.requestingUser.lastName) LIKE LOWER(CONCAT('%',"
                + " :userFilter, '%')) OR LOWER(CONCAT(er.requestingUser.firstName, ' ',"
                + " er.requestingUser.lastName)) LIKE LOWER(CONCAT('%', :userFilter, '%'))) GROUP"
                + " BY er.requestingUser.publicId, er.requestingUser.firstName,"
                + " er.requestingUser.lastName, er.status ORDER BY userLastName ASC, userFirstName"
                + " ASC, reservationStatus ASC")
    List<Object[]> findUserReservationActivityByCount(
            @Param("startDate") Instant startDate,
            @Param("endDatePlusOne") Instant endDatePlusOne,
            @Param("userFilter") String userFilter,
            Pageable pageable);

    @Query(
            "SELECT er FROM EquipmentReservation er WHERE er.endTime <= :currentTime "
                    + "AND er.status IN ('PENDING', 'APPROVED', 'ONGOING')")
    List<EquipmentReservation> findExpiredActiveReservations(
            @Param("currentTime") Instant currentTime);
}
