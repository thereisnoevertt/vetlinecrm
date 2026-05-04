package ru.vetline.crm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateSupplierOrderRequest {
    private UUID supplierId;
    private UUID ticketId;
    private String notes;
    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private BigDecimal quantity;
        private String unit;
    }
}
