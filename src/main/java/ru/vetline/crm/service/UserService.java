package ru.vetline.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vetline.crm.entity.User;
import ru.vetline.crm.entity.UserRole;
import ru.vetline.crm.repository.UserRepository;

import java.util.List;
import java.util.UUID;

/** Управление пользователями. ТЗ п. 4.3.4 — роль Администратор. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepo.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepo.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    /** Создать нового пользователя (Admin → ТЗ п.4.3.4) */
    @Transactional
    public User create(String fullName, String email, String rawPassword, UserRole role) {
        if (userRepo.existsByEmail(email))
            throw new IllegalArgumentException("Пользователь с e-mail «" + email + "» уже существует");
        return userRepo.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(encoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build());
    }

    /** Сменить пароль */
    @Transactional
    public void changePassword(UUID id, String rawPassword) {
        User u = findById(id);
        u.setPassword(encoder.encode(rawPassword));
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        userRepo.save(u);
    }

    /** Деактивировать / активировать */
    @Transactional
    public void setActive(UUID id, boolean active) {
        User u = findById(id);
        u.setActive(active);
        if (active) { u.setFailedAttempts(0); u.setLockedUntil(null); }
        userRepo.save(u);
    }

    /** Обновить данные */
    @Transactional
    public void update(UUID id, String fullName, UserRole role) {
        User u = findById(id);
        u.setFullName(fullName);
        u.setRole(role);
        userRepo.save(u);
    }
}
