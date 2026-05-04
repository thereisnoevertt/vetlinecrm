package ru.vetline.crm.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vetline.crm.entity.SupplierOrder;
import ru.vetline.crm.entity.SupplierOrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, UUID>,
                                                 JpaSpecificationExecutor<SupplierOrder> {

    default Page<SupplierOrder> search(UUID supplierId, SupplierOrderStatus status,
                                       LocalDateTime from, LocalDateTime to,
                                       Pageable pageable) {
        Specification<SupplierOrder> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (supplierId != null) p.add(cb.equal(root.get("supplier").get("id"), supplierId));
            if (status != null)     p.add(cb.equal(root.get("status"), status));
            if (from != null)       p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null)         p.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtBetween(SupplierOrderStatus status,
                                          LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT COUNT(DISTINCT o.supplier.id) FROM SupplierOrder o
        WHERE o.status = :status
          AND o.createdAt BETWEEN :from AND :to
        """)
    long countDistinctSuppliersByStatus(@Param("status") SupplierOrderStatus status,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    List<SupplierOrder> findByTicket_IdOrderByCreatedAtDesc(UUID ticketId);
}
