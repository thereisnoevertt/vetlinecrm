package ru.vetline.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vetline.crm.entity.Supplier;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByActiveTrueOrderByNameAsc();
    List<Supplier> findAllByOrderByNameAsc();
}
