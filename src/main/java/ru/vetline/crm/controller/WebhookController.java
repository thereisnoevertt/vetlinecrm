package ru.vetline.crm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.vetline.crm.dto.ApiResponse;
import ru.vetline.crm.dto.WebhookTicketRequest;
import ru.vetline.crm.entity.Ticket;
import ru.vetline.crm.service.TicketService;

/**
 * REST API для приёма заявок с сайта. ТЗ Ф-01.
 * POST /api/webhook/ticket
 * Header: X-Webhook-Token: <secret>
 */
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TicketService ticketService;

    @Value("${webhook.secret-token}")
    private String secret;

    @PostMapping("/ticket")
    public ResponseEntity<ApiResponse<String>> receive(
            @RequestHeader(value = "X-Webhook-Token", required = false) String token,
            @RequestBody WebhookTicketRequest req) {

        if (!secret.equals(token)) {
            log.warn("Webhook: неверный токен авторизации");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Неверный токен авторизации"));
        }
        if (req.getName() == null || req.getPhone() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Поля name и phone обязательны"));
        }
        try {
            Ticket t = ticketService.createFromWebhook(req);
            return ResponseEntity.ok(ApiResponse.ok(t.getNumber()));
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }
}
