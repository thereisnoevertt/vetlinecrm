package ru.vetline.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vetline.crm.entity.*;
import java.util.*;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByPhone(String phone);
    Optional<Client> findByVkId(Long vkId);
    boolean existsByPhone(String phone);
}
