package ru.vetline.crm.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookTicketRequest {
    private String name;
    private String phone;
    private String email;
    private String description;
}
