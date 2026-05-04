package ru.vetline.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vetline.crm.entity.PlanTarget;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanTargetRepository extends JpaRepository<PlanTarget, UUID> {
    Optional<PlanTarget> findByYearAndMonth(int year, Integer month);
}
