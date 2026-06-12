-- Запустить от имени суперпользователя postgres:
-- psql -U postgres -f db-init.sql

-- Создать пользователя (если не существует)
DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'vetline') THEN
      CREATE ROLE vetline LOGIN PASSWORD 'vetline_secret_2025';
   END IF;
END
$$;

-- Создать базу данных (если не существует)
SELECT 'CREATE DATABASE vetline_crm OWNER vetline ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vetline_crm')\gexec

-- Выдать права
GRANT ALL PRIVILEGES ON DATABASE vetline_crm TO vetline;

-- Подключиться к БД и дать права на схему
\connect vetline_crm
GRANT ALL ON SCHEMA public TO vetline;

\echo '=== База данных vetline_crm и пользователь vetline успешно созданы ==='
