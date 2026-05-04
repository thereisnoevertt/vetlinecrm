package ru.vetline.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vetline.crm.entity.AuditLog;
import ru.vetline.crm.entity.User;
import ru.vetline.crm.repository.AuditLogRepository;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;

    public void log(User user, String action, String entityType, UUID entityId, String details) {
        repo.save(AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }
}
