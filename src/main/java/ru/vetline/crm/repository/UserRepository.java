package ru.vetline.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vetline.crm.entity.*;
import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndActiveTrue(String email);
    Optional<User> findByEmail(String email);
    List<User> findByRoleAndActiveTrueOrderByFullNameAsc(UserRole role);
    List<User> findAllByOrderByFullNameAsc();
    boolean existsByEmail(String email);
}
