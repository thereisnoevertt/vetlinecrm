package ru.vetline.crm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.vetline.crm.dto.CreateTicketRequest;
import ru.vetline.crm.entity.*;
import ru.vetline.crm.repository.*;
import ru.vetline.crm.service.*;

import java.util.UUID;

/** UI-контроллер заявок. Ф-02 — Ф-07. */
@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService      ticketService;
    private final NotificationService notifService;
    private final ClientService      clientService;
    private final SourceRepository   sourceRepo;
    private final CategoryRepository categoryRepo;
    private final UserRepository     userRepo;

    // ── Ф-03: Все активные заявки ─────────────────────────────────────────────
    @GetMapping
    public String listAll(@RequestParam(required = false) String status,
                          @RequestParam(required = false) String managerId,
                          @RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "0") int page,
                          Model model,
                          @AuthenticationPrincipal UserDetails principal) {
        User me = resolve(principal);
        boolean isDirector = me.getRole() == UserRole.DIRECTOR;
        UUID mgrId = isDirector && managerId != null && !managerId.isBlank()
                ? UUID.fromString(managerId) : null;

        Page<Ticket> tickets = ticketService.search(false, parseStatus(status), mgrId, search, page);
        model.addAttribute("tickets", tickets);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("managers",
                userRepo.findByRoleAndActiveTrueOrderByFullNameAsc(UserRole.MANAGER));
        model.addAttribute("currentUser", me);
        model.addAttribute("isDirector",  isDirector);
        model.addAttribute("filterStatus",  status);
        model.addAttribute("filterManager", managerId);
        model.addAttribute("search", search);
        model.addAttribute("section", "all");
        return "tickets/list";
    }

    // ── Ф-03: Мои заявки ─────────────────────────────────────────────────────
    @GetMapping("/my")
    public String listMy(@RequestParam(required = false) String status,
                         @RequestParam(required = false) String search,
                         @RequestParam(defaultValue = "0") int page,
                         Model model,
                         @AuthenticationPrincipal UserDetails principal) {
        User me = resolve(principal);
        Page<Ticket> tickets = ticketService.search(
                false, parseStatus(status), me.getId(), search, page);
        model.addAttribute("tickets", tickets);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("currentUser", me);
        model.addAttribute("isDirector", me.getRole() == UserRole.DIRECTOR);
        model.addAttribute("managers",
                userRepo.findByRoleAndActiveTrueOrderByFullNameAsc(UserRole.MANAGER));
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterManager", null);
        model.addAttribute("search", search);
        model.addAttribute("section", "my");
        return "tickets/list";
    }

    // ── Ф-07: Архив ───────────────────────────────────────────────────────────
    @GetMapping("/archive")
    public String archive(@RequestParam(required = false) String status,
                          @RequestParam(required = false) String managerId,
                          @RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "0") int page,
                          Model model,
                          @AuthenticationPrincipal UserDetails principal) {
        User me = resolve(principal);
        boolean isDirector = me.getRole() == UserRole.DIRECTOR;
        UUID mgrId = isDirector && managerId != null && !managerId.isBlank()
                ? UUID.fromString(managerId) : null;
        Page<Ticket> tickets = ticketService.search(true, parseStatus(status), mgrId, search, page);
        model.addAttribute("tickets", tickets);
        model.addAttribute("statuses",
                new TicketStatus[]{TicketStatus.DONE, TicketStatus.CANCELLED});
        model.addAttribute("managers",
                userRepo.findByRoleAndActiveTrueOrderByFullNameAsc(UserRole.MANAGER));
        model.addAttribute("currentUser", me);
        model.addAttribute("isDirector",  isDirector);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterManager", managerId);
        model.addAttribute("search", search);
        model.addAttribute("section", "archive");
        return "tickets/list";
    }

    // ── Ф-04: Карточка заявки ─────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String view(@PathVariable UUID id, Model model,
                       @AuthenticationPrincipal UserDetails principal) {
        User me = resolve(principal);
        Ticket ticket = ticketService.findById(id);
        model.addAttribute("ticket",  ticket);
        model.addAttribute("currentUser", me);
        model.addAttribute("allowedTransitions", ticket.getStatus().getAllowedTransitions());
        model.addAttribute("canArchive", ticket.canBeArchived() && !ticket.isArchived());
        model.addAttribute("canNotify",  ticket.getClient().getVkId() != null);
        model.addAttribute("isDirector", me.getRole() == UserRole.DIRECTOR);
        return "tickets/detail";
    }

    // ── Ф-02: Форма создания ─────────────────────────────────────────────────
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form",       new CreateTicketRequest());
        model.addAttribute("sources",    sourceRepo.findAllByOrderByNameAsc());
        model.addAttribute("categories", categoryRepo.findByActiveTrueOrderByNameAsc());
        return "tickets/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") CreateTicketRequest form,
                         BindingResult errors, Model model, RedirectAttributes ra,
                         @AuthenticationPrincipal UserDetails principal) {
        if (errors.hasErrors()) {
            model.addAttribute("sources",    sourceRepo.findAllByOrderByNameAsc());
            model.addAttribute("categories", categoryRepo.findByActiveTrueOrderByNameAsc());
            return "tickets/form";
        }
        Ticket t = ticketService.createManually(form, resolve(principal));
        ra.addFlashAttribute("success", "Заявка " + t.getNumber() + " создана");
        return "redirect:/tickets/" + t.getId();
    }

    // ── Ф-05: Смена статуса ───────────────────────────────────────────────────
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable UUID id,
                               @RequestParam String newStatus,
                               RedirectAttributes ra,
                               @AuthenticationPrincipal UserDetails principal) {
        try {
            Ticket t = ticketService.changeStatus(
                    id, TicketStatus.valueOf(newStatus), resolve(principal));
            ra.addFlashAttribute("success",
                    "Статус изменён на «" + t.getStatus().getDisplayName() + "»");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    // ── Ф-06: Отправить уведомление ──────────────────────────────────────────
    @PostMapping("/{id}/notify")
    public String notify(@PathVariable UUID id, RedirectAttributes ra,
                         @AuthenticationPrincipal UserDetails principal) {
        Ticket ticket = ticketService.findById(id);
        if (ticket.getClient().getVkId() == null) {
            ra.addFlashAttribute("error",
                    "VK ID клиента не заполнен. Откройте карточку клиента и введите ID.");
        } else {
            notifService.sendNotification(ticket, resolve(principal));
            ra.addFlashAttribute("success", "Уведомление поставлено в очередь отправки");
        }
        return "redirect:/tickets/" + id;
    }

    // ── Ф-07: Архивировать ────────────────────────────────────────────────────
    @PostMapping("/{id}/archive")
    public String doArchive(@PathVariable UUID id,
                            @RequestParam(required = false) String comment,
                            RedirectAttributes ra,
                            @AuthenticationPrincipal UserDetails principal) {
        try {
            ticketService.archive(id, comment, resolve(principal));
            ra.addFlashAttribute("success", "Заявка перемещена в архив");
            return "redirect:/tickets";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tickets/" + id;
        }
    }

    // ── Редактирование VK ID клиента ─────────────────────────────────────────
    @PostMapping("/{ticketId}/client/vk")
    public String saveVkId(@PathVariable UUID ticketId,
                           @RequestParam(required = false) Long vkId,
                           RedirectAttributes ra) {
        Ticket ticket = ticketService.findById(ticketId);
        clientService.updateVkId(ticket.getClient().getId(), vkId);
        ra.addFlashAttribute("success", "VK ID клиента сохранён");
        return "redirect:/tickets/" + ticketId;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private User resolve(UserDetails p) {
        return userRepo.findByEmailAndActiveTrue(p.getUsername()).orElseThrow();
    }
    private TicketStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return TicketStatus.valueOf(s); } catch (Exception e) { return null; }
    }
}
