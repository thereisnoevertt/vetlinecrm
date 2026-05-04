-- V2__supplier_orders.sql — справочник поставщиков и заказы поставщикам.
-- Прецедент №4 «Генерация заказа поставщику».
-- Цены и суммы НЕ хранятся: курс валют и индивидуальные прайсы поставщиков
-- делают эти поля нерелевантными в момент формирования заказа.

CREATE TYPE supplier_order_status AS ENUM (
    'DRAFT', 'SENT', 'CONFIRMED', 'IN_DELIVERY', 'COMPLETED', 'CANCELLED'
);

CREATE TABLE suppliers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL UNIQUE,
    contact_person  VARCHAR(200),
    phone           VARCHAR(40),
    email           VARCHAR(200),
    country         VARCHAR(100),
    notes           TEXT,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE supplier_orders (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number       VARCHAR(20) NOT NULL UNIQUE,
    ticket_id    UUID REFERENCES tickets(id),
    supplier_id  UUID NOT NULL REFERENCES suppliers(id),
    manager_id   UUID NOT NULL REFERENCES users(id),
    status       supplier_order_status NOT NULL DEFAULT 'DRAFT',
    notes        TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_supplier_orders_status   ON supplier_orders(status);
CREATE INDEX idx_supplier_orders_supplier ON supplier_orders(supplier_id);
CREATE INDEX idx_supplier_orders_ticket   ON supplier_orders(ticket_id);
CREATE INDEX idx_supplier_orders_created  ON supplier_orders(created_at);

CREATE SEQUENCE supplier_order_seq START 1;

CREATE TABLE supplier_order_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_order_id  UUID NOT NULL REFERENCES supplier_orders(id) ON DELETE CASCADE,
    name               VARCHAR(300) NOT NULL,
    quantity           NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    unit               VARCHAR(20) NOT NULL DEFAULT 'шт.',
    position           INT NOT NULL DEFAULT 1
);
CREATE INDEX idx_supplier_order_items_order ON supplier_order_items(supplier_order_id);

-- ── Демо-поставщики ──────────────────────────────────────────────────────
INSERT INTO suppliers(name, contact_person, phone, email, country, active) VALUES
  ('ООО «ВетМедТех»',    'Иванов В.С.', '+7(495)123-45-67', 'sales@vetmedtech.ru',  'Россия',   TRUE),
  ('ЗАО «СтерилПро»',    'Кузнецов А.Н.','+7(812)987-65-43','order@sterilpro.ru',    'Россия',   TRUE),
  ('ИП Смирнов Д.А.',    'Смирнов Д.А.','+7(903)555-77-88', 'smirnov@mail.ru',       'Россия',   TRUE),
  ('Mindray Animal Care','Wei Zhang',   '+86 755 8123 4567','intl@mindray.com',      'Китай',    TRUE),
  ('Eickemeyer GmbH',    'M. Schulz',   '+49 7461 96580 0', 'order@eickemeyer.de',   'Германия', TRUE);
