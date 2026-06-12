package ru.vetline.crm.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vetline.crm.entity.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>,
                                           JpaSpecificationExecutor<Ticket> {

    default Page<Ticket> search(boolean archived, TicketStatus status,
                                UUID managerId, String search, Pageable pageable) {
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("archived"), archived));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (managerId != null) {
                predicates.add(cb.equal(root.get("manager").get("id"), managerId));
            }
            if (search != null && !search.isBlank()) {
                // Поиск регистронезависимый по подстроке:
                //   • номер заявки (например, «141» найдёт VL-2026-000141);
                //   • ФИО клиента — фамилия / имя / отчество в любом порядке;
                //   • телефон (по точной подстроке, чтобы найти и
                //     «+7(495)123-45-67», и «4951234567»);
                //   • название организации (актуально для архива:
                //     часто помнят «ООО Виталиник», а не ФИО менеджера);
                //   • email клиента.
                String pattern = "%" + search.toLowerCase().trim() + "%";
                String phoneRaw = search.trim();
                var client = root.get("client");
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("number")),         pattern),
                    cb.like(cb.lower(client.get("fullName")),     pattern),
                    cb.like(client.get("phone"),                  "%" + phoneRaw + "%"),
                    cb.like(cb.lower(cb.coalesce(client.get("organization"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(client.get("email"),        "")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    long countByCreatedAtBetweenAndArchived(LocalDateTime from, LocalDateTime to, boolean archived);

    /** Все заявки клиента (включая архивные), новые сверху — для карточки клиента. */
    @Query("SELECT t FROM Ticket t WHERE t.client.id = :clientId ORDER BY t.createdAt DESC")
    List<Ticket> findByClientOrderByCreatedAtDesc(@Param("clientId") UUID clientId);

    /** Количество заявок в разрезе клиентов — для столбца «Заявок» в списке клиентов. */
    @Query("SELECT t.client.id, COUNT(t) FROM Ticket t GROUP BY t.client.id")
    List<Object[]> countTicketsGroupedByClient();

    @Query("""
        SELECT t.status, COUNT(t) FROM Ticket t
        WHERE t.archived = false AND t.createdAt BETWEEN :from AND :to
        GROUP BY t.status
        """)
    List<Object[]> countByStatusBetween(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("""
        SELECT CAST(t.createdAt AS date), COUNT(t)
        FROM Ticket t WHERE t.createdAt BETWEEN :from AND :to
        GROUP BY CAST(t.createdAt AS date) ORDER BY CAST(t.createdAt AS date)
        """)
    List<Object[]> dailyCountsBetween(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.archived=false AND t.status NOT IN (ru.vetline.crm.entity.TicketStatus.DONE, ru.vetline.crm.entity.TicketStatus.CANCELLED, ru.vetline.crm.entity.TicketStatus.ARCHIVED)")
    long countActive();

    @Query("""
        SELECT t.status, COUNT(t) FROM Ticket t
        WHERE t.createdAt BETWEEN :from AND :to
          AND (:managerId IS NULL OR t.manager.id = :managerId)
        GROUP BY t.status
        """)
    List<Object[]> countByStatusForReport(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("managerId") UUID managerId);

    @Query("""
        SELECT COALESCE(t.source.name,'Не указан'), COUNT(t) FROM Ticket t
        WHERE t.createdAt BETWEEN :from AND :to
          AND (:managerId IS NULL OR t.manager.id = :managerId)
        GROUP BY t.source.name
        """)
    List<Object[]> countBySource(@Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("managerId") UUID managerId);

    @Query("""
        SELECT COALESCE(t.category.name,'Не указана'), COUNT(t) FROM Ticket t
        WHERE t.createdAt BETWEEN :from AND :to
          AND (:managerId IS NULL OR t.manager.id = :managerId)
        GROUP BY t.category.name
        """)
    List<Object[]> countByCategory(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("managerId") UUID managerId);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.status = ru.vetline.crm.entity.TicketStatus.DONE
          AND t.createdAt BETWEEN :from AND :to
          AND (:managerId IS NULL OR t.manager.id = :managerId)
        """)
    long countDone(@Param("from") LocalDateTime from,
                   @Param("to") LocalDateTime to,
                   @Param("managerId") UUID managerId);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.createdAt BETWEEN :from AND :to
          AND (:managerId IS NULL OR t.manager.id = :managerId)
        """)
    long countTotal(@Param("from") LocalDateTime from,
                    @Param("to") LocalDateTime to,
                    @Param("managerId") UUID managerId);

    @Query("""
        SELECT CAST(t.createdAt AS date), COUNT(t) FROM Ticket t
        WHERE t.createdAt BETWEEN :from AND :to
          AND t.status = ru.vetline.crm.entity.TicketStatus.DONE
        GROUP BY CAST(t.createdAt AS date) ORDER BY CAST(t.createdAt AS date)
        """)
    List<Object[]> dailyDoneCountsBetween(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("""
        SELECT t.manager.fullName,
               COUNT(t),
               SUM(CASE WHEN t.status = ru.vetline.crm.entity.TicketStatus.DONE THEN 1 ELSE 0 END)
        FROM Ticket t
        WHERE t.createdAt BETWEEN :from AND :to
        GROUP BY t.manager.id, t.manager.fullName
        ORDER BY COUNT(t) DESC
        """)
    List<Object[]> topManagersBetween(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);
}
