package ru.vetline.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vetline.crm.entity.Client;
import ru.vetline.crm.entity.Ticket;
import ru.vetline.crm.repository.TicketRepository;
import ru.vetline.crm.service.ClientService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Просмотр клиентской базы и связанных с клиентом заявок.
 * Раздел доступен всем авторизованным пользователям в режиме «только чтение».
 */
@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService    clientService;
    private final TicketRepository ticketRepo;

    /** Список клиентов с поиском и числом заявок по каждому. */
    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Client> clients = clientService.search(search, page);

        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : ticketRepo.countTicketsGroupedByClient()) {
            counts.put((UUID) row[0], (Long) row[1]);
        }

        model.addAttribute("clients",      clients);
        model.addAttribute("ticketCounts", counts);
        model.addAttribute("search",       search);
        model.addAttribute("section",      "clients");
        return "clients/list";
    }

    /** Карточка клиента: реквизиты + список всех его заявок. */
    @GetMapping("/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Client client = clientService.findById(id);
        List<Ticket> tickets = ticketRepo.findByClientOrderByCreatedAtDesc(id);

        model.addAttribute("client",  client);
        model.addAttribute("tickets", tickets);
        model.addAttribute("section", "clients");
        return "clients/detail";
    }
}
