-- ────────────────────────────────────────────────────────────────────
-- clean_tickets.sql — очистка тестовых заявок и связанных данных.
--
-- ОСТАНОВИТЕ ПРИЛОЖЕНИЕ перед выполнением (чтобы не было гонок с
-- @Scheduled-задачами уведомлений).
--
-- Скрипт удаляет:
--   • все заявки и их историю,
--   • все уведомления,
--   • все заказы поставщикам и их позиции,
--   • всех клиентов (если нужно сохранить — закомментируйте строку),
--   • сбрасывает последовательности номеров.
--
-- Справочники (источники, категории, поставщики), пользователи и
-- плановые показатели НЕ удаляются.
-- ────────────────────────────────────────────────────────────────────

BEGIN;

-- 1. Уведомления (FK на tickets, clients)
DELETE FROM notifications;

-- 2. История заявок (FK на tickets, ON DELETE CASCADE; явно для скорости)
DELETE FROM ticket_history;

-- 3. Позиции заказов поставщикам
DELETE FROM supplier_order_items;

-- 4. Заказы поставщикам (FK на tickets)
DELETE FROM supplier_orders;

-- 5. Сами заявки
DELETE FROM tickets;

-- 6. Клиенты (раскомментируйте, если хотите оставить клиентскую базу):
DELETE FROM clients;

-- 7. Сбросить последовательности нумерации
ALTER SEQUENCE ticket_seq RESTART WITH 1;
ALTER SEQUENCE supplier_order_seq RESTART WITH 1;

-- 8. Журнал аудита по сущности Ticket (опционально, для чистоты)
DELETE FROM audit_log WHERE entity_type IN ('Ticket','SupplierOrder');

COMMIT;

-- Проверка
SELECT 'tickets'                AS tbl, COUNT(*) FROM tickets
UNION ALL SELECT 'clients',              COUNT(*) FROM clients
UNION ALL SELECT 'notifications',        COUNT(*) FROM notifications
UNION ALL SELECT 'ticket_history',       COUNT(*) FROM ticket_history
UNION ALL SELECT 'supplier_orders',      COUNT(*) FROM supplier_orders
UNION ALL SELECT 'supplier_order_items', COUNT(*) FROM supplier_order_items;
