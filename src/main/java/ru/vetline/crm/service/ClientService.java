package ru.vetline.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vetline.crm.entity.Client;
import ru.vetline.crm.repository.ClientRepository;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepo;

    @Transactional(readOnly = true)
    public Client findById(UUID id) {
        return clientRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }

    /** Обновить VK user_id клиента (нужно для активации уведомлений, ТЗ Ф-06) */
    @Transactional
    public void updateVkId(UUID clientId, Long vkId) {
        Client c = findById(clientId);
        c.setVkId(vkId);
        clientRepo.save(c);
    }

    /** Обновить контактные данные */
    @Transactional
    public void update(UUID id, String fullName, String phone, String email,
                       String organization, Long vkId) {
        Client c = findById(id);
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setEmail(email);
        c.setOrganization(organization);
        c.setVkId(vkId);
        clientRepo.save(c);
    }
}
