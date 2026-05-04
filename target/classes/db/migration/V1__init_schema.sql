-- V1__init_schema.sql — Vetline CRM
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role       AS ENUM ('MANAGER','DIRECTOR','ADMIN');
CREATE TYPE ticket_status   AS ENUM ('NEW','IN_PROGRESS','WORKING','FROZEN','ON_APPROVAL','TRANSFERRED','AWAITING_REPLY','IN_DELIVERY','DONE','CANCELLED','ARCHIVED');
CREATE TYPE history_event   AS ENUM ('STATUS_CHANGE','FIELD_UPDATE','NOTIFICATION_SENT','ARCHIVED','CREATED');
CREATE TYPE delivery_status AS ENUM ('SENT','ERROR','PENDING');

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200) NOT NULL UNIQUE,
    password        VARCHAR(100) NOT NULL,
    role            user_role NOT NULL DEFAULT 'MANAGER',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE clients (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name    VARCHAR(200) NOT NULL,
    phone        VARCHAR(20)  NOT NULL UNIQUE,
    email        VARCHAR(200),
    organization VARCHAR(200),
    telegram_id  BIGINT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name   VARCHAR(200) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE sources (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tickets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number       VARCHAR(20) NOT NULL UNIQUE,
    client_id    UUID NOT NULL REFERENCES clients(id),
    manager_id   UUID NOT NULL REFERENCES users(id),
    status       ticket_status NOT NULL DEFAULT 'NEW',
    source_id    UUID REFERENCES sources(id),
    category_id  UUID REFERENCES categories(id),
    description  TEXT,
    note_client  TEXT,
    note_manager TEXT,
    archived     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    archived_at  TIMESTAMP
);
CREATE INDEX idx_tickets_status   ON tickets(status);
CREATE INDEX idx_tickets_manager  ON tickets(manager_id);
CREATE INDEX idx_tickets_created  ON tickets(created_at);
CREATE INDEX idx_tickets_archived ON tickets(archived);

CREATE SEQUENCE ticket_seq START 1;

CREATE TABLE ticket_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    event_type  history_event NOT NULL,
    status_from ticket_status,
    status_to   ticket_status,
    user_id     UUID REFERENCES users(id),
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_history_ticket  ON ticket_history(ticket_id);

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID NOT NULL REFERENCES tickets(id),
    client_id       UUID NOT NULL REFERENCES clients(id),
    message_text    TEXT NOT NULL,
    delivery_status delivery_status NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_ticket ON notifications(ticket_id);
CREATE INDEX idx_notif_status ON notifications(delivery_status);

CREATE TABLE plan_targets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year         INT NOT NULL,
    month        INT,
    target_count INT NOT NULL,
    set_by_user  UUID REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(year, month)
);

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    ip_address  VARCHAR(45),
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Справочники ──
INSERT INTO sources(id,name) VALUES
  (gen_random_uuid(),'Телефон'),
  (gen_random_uuid(),'E-mail'),
  (gen_random_uuid(),'Мессенджер'),
  (gen_random_uuid(),'Личный визит'),
  (gen_random_uuid(),'Сайт'),
  (gen_random_uuid(),'Другое');

INSERT INTO categories(id,name) VALUES
  (gen_random_uuid(),'Диагностическое оборудование'),
  (gen_random_uuid(),'Хирургический инструментарий'),
  (gen_random_uuid(),'Расходные материалы'),
  (gen_random_uuid(),'Лабораторное оборудование'),
  (gen_random_uuid(),'Ветеринарные препараты'),
  (gen_random_uuid(),'Сервисное обслуживание'),
  (gen_random_uuid(),'Другое');

-- ── Пользователи по умолчанию ──
-- Пароль admin@vetline.ru  : Admin2025!
INSERT INTO users(id,full_name,email,password,role) VALUES
  (gen_random_uuid(),'Системный администратор','admin@vetline.ru',
   '$2a$12$Nm5dNTXlnv09fR6Ub7bWZeGJXpVtdCTuclgv.H5cV0BbpMQJVYaVm','ADMIN');

-- Пароль director@vetline.ru : Director2025!
INSERT INTO users(id,full_name,email,password,role) VALUES
  (gen_random_uuid(),'Левашов Денис Александрович','director@vetline.ru',
   '$2a$12$8QdYoVl7NR0Qb3dB1oT9seFpDkOxhCgC6L6BFGNmO2HF.abMdFl2m','DIRECTOR');
