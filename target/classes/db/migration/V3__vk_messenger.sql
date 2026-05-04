-- V3__vk_messenger.sql — переход с Telegram-бота на VK-бот.
-- Изменения:
--   1. Переименование колонки clients.telegram_id → vk_id
--      (тип сохраняется BIGINT — VK user_id также 64-битный)
--   2. Существующие значения Telegram chat_id остаются, но семантически
--      обозначают VK user_id. Перед эксплуатацией ИС в продакшне
--      потребуется ручная очистка / актуализация (см. план перехода ТЗ).

ALTER TABLE clients RENAME COLUMN telegram_id TO vk_id;

-- Комментарий к колонке
COMMENT ON COLUMN clients.vk_id IS
    'VK user_id клиента — целевой получатель сообщений сообщества через VK Bot API';

-- Новый индекс для быстрого поиска получателя по vk_id (используется в LongPoll-обработчике)
CREATE INDEX IF NOT EXISTS idx_clients_vk_id ON clients(vk_id) WHERE vk_id IS NOT NULL;
