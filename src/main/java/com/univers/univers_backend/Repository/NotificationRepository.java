/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Notification;
import com.univers.univers_backend.Entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientAndDeletedFalseOrderByCreatedAtDesc(
            User recipient, Pageable pageable);

    long countByRecipientAndIsReadFalseAndDeletedFalse(User recipient);

    @Modifying
    @Query(
            "UPDATE Notification n SET n.isRead = true WHERE n.id IN :ids AND n.recipient ="
                    + " :recipient")
    void markAsRead(List<Long> ids, User recipient);

    @Modifying
    @Query(
            "UPDATE Notification n SET n.isRead = true WHERE n.recipient = :recipient AND n.isRead"
                    + " = false AND n.deleted = false")
    void markAllAsRead(User recipient);

    @Modifying
    @Query(
            "UPDATE Notification n SET n.deleted = true WHERE n.id IN :ids AND n.recipient ="
                    + " :recipient")
    void markAsDeleted(List<Long> ids, User recipient);
}
