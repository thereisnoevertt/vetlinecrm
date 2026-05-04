package ru.vetline.crm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация VK-бота сообщества.
 *
 * Сам {@link ru.vetline.crm.vk.VkBot} помечен как {@code @Component} и
 * автоматически запускается через {@code @PostConstruct} при наличии
 * валидного токена. Конфигурация оставлена как точка расширения
 * (в дальнейшем — настройка пула HTTP-клиентов, метрик, ретраев).
 */
@Configuration
@Slf4j
@ConditionalOnExpression("!'${vk.group.token}'.equals('REPLACE_ME')")
public class VkBotConfig {
    public VkBotConfig() {
        log.info("VK-бот сконфигурирован (токен установлен)");
    }
}
