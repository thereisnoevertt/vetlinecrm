package ru.vetline.crm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.vetline.crm.dto.CreateSupplierOrderRequest;
import ru.vetline.crm.entity.*;
import ru.vetline.crm.repository.*;
import ru.vetline.crm.service.SupplierOrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Прецедент №4 — заказы поставщикам. */
@Controller
@RequestMapping("/supplier-orders")
@RequiredArgsConstructor
@Slf4j
public class SupplierOrderController {

    private final SupplierOrderService    service;
    private final SupplierOrderRepository orderRepo;
    private final SupplierRepository      supplierRepo;
    private final TicketRepository        ticketRepo;
    private final UserRepository          userRepo;

    // ── Список заказов ────────────────────────────────────────────────────────
    @GetMapping
    public String list(@RequestParam(required = false) UUID supplierId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) UUID selected,
                       Model model,
                       @AuthenticationPrincipal UserDetails principal) {

        YearMonth ym = YearMonth.now();
        LocalDate fromD = (from != null) ? from : ym.atDay(1);
        LocalDate toD   = (to   != null) ? to   : ym.atEndOfMonth();

        LocalDateTime fromDt = fromD.atStartOfDay();
        LocalDateTime toDt   = toD.atTime(LocalTime.MAX);

        SupplierOrderStatus st = parseStatus(status);
        Page<SupplierOrder> orders = service.search(supplierId, st, fromDt, toDt, page);

        long countPeriod = orderRepo.countByCreatedAtBetween(fromDt, toDt);
        long awaiting    = orderRepo.countDistinctSuppliersByStatus(
                SupplierOrderStatus.SENT, fromDt, toDt);

        SupplierOrder selectedOrder = null;
        if (selected != null) {
            selectedOrder = orderRepo.findById(selected).orElse(null);
        } else if (!orders.isEmpty()) {
            selectedOrder = orders.getContent().get(0);
        }

        model.addAttribute("orders",        orders);
        model.addAttribute("suppliers",     supplierRepo.findByActiveTrueOrderByNameAsc());
        model.addAttribute("statuses",      SupplierOrderStatus.values());
        model.addAttribute("filterSupplier", supplierId);
        model.addAttribute("filterStatus",   status);
        model.addAttribute("from",           fromD);
        model.addAttribute("to",             toD);
        model.addAttribute("countPeriod",    countPeriod);
        model.addAttribute("awaitingCount",  awaiting);
        model.addAttribute("selectedOrder",  selectedOrder);
        model.addAttribute("monthLabel",     monthLabel(fromD));
        model.addAttribute("section",        "supplier-orders");
        return "supplier_orders/list";
    }

    // ── Карточка заказа ───────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String view(@PathVariable UUID id, Model model) {
        SupplierOrder order = service.findById(id);
        model.addAttribute("order",     order);
        model.addAttribute("section",   "supplier-orders");
        model.addAttribute("statuses",  order.getStatus().getAllowedTransitions());
        return "supplier_orders/detail";
    }

    // ── Форма создания (опционально на основании заявки) ─────────────────────
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) UUID ticketId, Model model) {
        Ticket ticket = (ticketId != null) ? ticketRepo.findById(ticketId).orElse(null) : null;
        model.addAttribute("ticket",    ticket);
        model.addAttribute("suppliers", supplierRepo.findByActiveTrueOrderByNameAsc());
        model.addAttribute("section",   "supplier-orders");
        return "supplier_orders/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam UUID supplierId,
                         @RequestParam(required = false) UUID ticketId,
                         @RequestParam(required = false) String notes,
                         @RequestParam(name = "name",     required = false) List<String> names,
                         @RequestParam(name = "quantity", required = false) List<String> quantities,
                         @RequestParam(name = "unit",     required = false) List<String> units,
                         RedirectAttributes ra,
                         @AuthenticationPrincipal UserDetails principal) {
        try {
            CreateSupplierOrderRequest req = new CreateSupplierOrderRequest();
            req.setSupplierId(supplierId);
            req.setTicketId(ticketId);
            req.setNotes(notes);
            List<CreateSupplierOrderRequest.Item> items = new ArrayList<>();
            int n = names != null ? names.size() : 0;
            for (int i = 0; i < n; i++) {
                String name = names.get(i);
                if (name == null || name.isBlank()) continue;
                String qStr = (quantities != null && i < quantities.size()) ? quantities.get(i) : "";
                String unit = (units      != null && i < units.size())      ? units.get(i)      : "шт.";
                BigDecimal qty;
                try { qty = new BigDecimal(qStr.replace(',', '.').trim()); }
                catch (Exception ex) { continue; }
                CreateSupplierOrderRequest.Item it = new CreateSupplierOrderRequest.Item();
                it.setName(name); it.setQuantity(qty); it.setUnit(unit);
                items.add(it);
            }
            req.setItems(items);

            SupplierOrder order = service.create(req, resolve(principal));
            ra.addFlashAttribute("success",
                    "Заказ " + order.getNumber() + " сохранён");
            return "redirect:/supplier-orders/" + order.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return ticketId != null
                    ? "redirect:/supplier-orders/new?ticketId=" + ticketId
                    : "redirect:/supplier-orders/new";
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable UUID id,
                               @RequestParam(required = false) String newStatus,
                               RedirectAttributes ra,
                               @AuthenticationPrincipal UserDetails principal) {
        if (newStatus == null || newStatus.isBlank()) {
            ra.addFlashAttribute("error", "Не выбран новый статус");
            return "redirect:/supplier-orders/" + id;
        }
        try {
            SupplierOrderStatus target;
            try { target = SupplierOrderStatus.valueOf(newStatus); }
            catch (IllegalArgumentException ex) {
                ra.addFlashAttribute("error", "Неизвестный статус: " + newStatus);
                return "redirect:/supplier-orders/" + id;
            }
            SupplierOrder order = service.changeStatus(id, target, resolve(principal));
            ra.addFlashAttribute("success",
                    "Статус: " + order.getStatus().getDisplayName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier-orders/" + id;
    }

    /** Защита от прямого GET-перехода по адресу формы — редиректим в карточку. */
    @GetMapping("/{id}/status")
    public String statusGetFallback(@PathVariable UUID id) {
        return "redirect:/supplier-orders/" + id;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private User resolve(UserDetails p) {
        return userRepo.findByEmailAndActiveTrue(p.getUsername()).orElseThrow();
    }

    private SupplierOrderStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return SupplierOrderStatus.valueOf(s); } catch (Exception e) { return null; }
    }

    private static final String[] MONTHS = {
            "январь","февраль","март","апрель","май","июнь",
            "июль","август","сентябрь","октябрь","ноябрь","декабрь"
    };
    private String monthLabel(LocalDate d) {
        return MONTHS[d.getMonthValue() - 1] + " " + d.getYear();
    }
}
