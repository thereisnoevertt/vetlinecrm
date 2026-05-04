package ru.vetline.crm.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vetline.crm.dto.CreateSupplierOrderRequest;
import ru.vetline.crm.entity.*;
import ru.vetline.crm.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

/** Прецедент №4 — генерация и сопровождение заказов поставщикам. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierOrderService {

    private final SupplierOrderRepository orderRepo;
    private final SupplierRepository      supplierRepo;
    private final TicketRepository        ticketRepo;
    private final AuditService            audit;

    @Transactional(readOnly = true)
    public Page<SupplierOrder> search(UUID supplierId, SupplierOrderStatus status,
                                      LocalDateTime from, LocalDateTime to, int page) {
        return orderRepo.search(supplierId, status, from, to,
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public SupplierOrder findById(UUID id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + id));
    }

    @Transactional
    public SupplierOrder create(CreateSupplierOrderRequest req, User currentUser) {
        if (req.getSupplierId() == null)
            throw new IllegalArgumentException("Не выбран поставщик");
        if (req.getItems() == null || req.getItems().isEmpty())
            throw new IllegalArgumentException("Добавьте хотя бы одну товарную позицию");

        Supplier supplier = supplierRepo.findById(req.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Поставщик не найден"));

        Ticket ticket = null;
        if (req.getTicketId() != null) {
            ticket = ticketRepo.findById(req.getTicketId()).orElse(null);
        }

        SupplierOrder order = SupplierOrder.builder()
                .number(generateNumber())
                .ticket(ticket)
                .supplier(supplier)
                .manager(currentUser)
                .status(SupplierOrderStatus.DRAFT)
                .notes(req.getNotes())
                .build();

        int pos = 1;
        for (CreateSupplierOrderRequest.Item it : req.getItems()) {
            if (it.getName() == null || it.getName().isBlank()) continue;
            BigDecimal qty = it.getQuantity();
            if (qty == null || qty.signum() <= 0) continue;
            String unit = (it.getUnit() == null || it.getUnit().isBlank()) ? "шт." : it.getUnit();
            order.getItems().add(SupplierOrderItem.builder()
                    .order(order).name(it.getName().trim())
                    .quantity(qty).unit(unit).position(pos++).build());
        }
        if (order.getItems().isEmpty())
            throw new IllegalArgumentException("Добавьте хотя бы одну товарную позицию");

        order = orderRepo.save(order);
        audit.log(currentUser, "SUPPLIER_ORDER_CREATED", "SupplierOrder", order.getId(),
                "ticket=" + (ticket != null ? ticket.getNumber() : "—"));
        log.info("Создан заказ поставщику {} (поставщик={})", order.getNumber(), supplier.getName());
        return order;
    }

    @Transactional
    public SupplierOrder changeStatus(UUID orderId, SupplierOrderStatus newStatus, User user) {
        SupplierOrder order = findById(orderId);
        if (!order.getStatus().getAllowedTransitions().contains(newStatus))
            throw new IllegalStateException(
                    "Недопустимый переход: «" + order.getStatus().getDisplayName() +
                    "» → «" + newStatus.getDisplayName() + "»");
        SupplierOrderStatus old = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepo.save(order);
        audit.log(user, "SUPPLIER_ORDER_STATUS", "SupplierOrder", order.getId(),
                "from=" + old + " to=" + newStatus);
        return order;
    }

    private synchronized String generateNumber() {
        long seq = orderRepo.count() + 1;
        return String.format("ЗП-%d-%04d", Year.now().getValue() % 100, seq);
    }
}
