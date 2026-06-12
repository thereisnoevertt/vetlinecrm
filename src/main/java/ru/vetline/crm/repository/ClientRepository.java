package ru.vetline.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vetline.crm.entity.*;
import java.util.*;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByPhone(String phone);
    Optional<Client> findByVkId(Long vkId);
    boolean existsByPhone(String phone);

    /**
     * Поиск клиентов по подстроке (регистронезависимо): ФИО, телефон,
     * организация, e-mail. Пустая строка :q (LIKE '%%') возвращает всех.
     */
    @Query("""
        SELECT c FROM Client c
        WHERE LOWER(c.fullName) LIKE CONCAT('%', :q, '%')
           OR c.phone LIKE CONCAT('%', :q, '%')
           OR LOWER(COALESCE(c.organization, '')) LIKE CONCAT('%', :q, '%')
           OR LOWER(COALESCE(c.email, '')) LIKE CONCAT('%', :q, '%')
        """)
    Page<Client> search(@Param("q") String q, Pageable pageable);
}
