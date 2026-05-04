package ru.vetline.crm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTicketRequest {

    @NotBlank(message = "ФИО обязательно")
    @Size(max = 200)
    private String fullName;

    @NotBlank(message = "Телефон обязателен")
    private String phone;

    @Email(message = "Некорректный формат e-mail")
    private String email;

    @Size(max = 200)
    private String organization;

    private UUID sourceId;
    private UUID categoryId;

    @Size(max = 2000)
    private String description;

    @Size(max = 1000)
    private String noteClient;
}
