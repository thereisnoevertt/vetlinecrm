# Vetline CRM — Запуск на Windows без Docker

## Что потребуется

| Инструмент | Версия | Скачать |
|---|---|---|
| Java JDK | 17 или 21 (LTS) | https://adoptium.net |
| Apache Maven | 3.9.x | https://maven.apache.org/download.cgi |
| PostgreSQL | 15 или 16 | https://www.postgresql.org/download/windows/ |

> **Важно:** Telegram-бот — необязательный компонент. Если токена нет, приложение запустится, бот просто не будет работать.

---

## Шаг 1. Установка Java 17

1. Перейдите на https://adoptium.net
2. Выберите **Temurin 17 (LTS)** → **Windows** → **x64** → **JDK** → `.msi`
3. Установите, отметив **"Set JAVA_HOME"** и **"Add to PATH"**
4. Проверьте в новом `cmd`:
   ```cmd
   java -version
   ```
   Должно вывести: `openjdk version "17.x.x ..."`

---

## Шаг 2. Установка Maven

1. Скачайте `apache-maven-3.9.x-bin.zip` с https://maven.apache.org/download.cgi
2. Распакуйте, например в `C:\tools\maven`
3. Добавьте в **системные переменные**:
   - `MAVEN_HOME` = `C:\tools\maven`
   - В `Path` добавьте `%MAVEN_HOME%\bin`
4. Проверьте в новом `cmd`:
   ```cmd
   mvn -version
   ```

---

## Шаг 3. Установка и настройка PostgreSQL

1. Скачайте установщик с https://www.postgresql.org/download/windows/
2. Установите PostgreSQL 15 или 16. Запомните пароль суперпользователя `postgres`
3. После установки откройте **psql** (или pgAdmin → Query Tool) и выполните скрипт `db-init.sql`:

**Вариант A — через psql в командной строке:**
```cmd
psql -U postgres -f db-init.sql
```
Введите пароль суперпользователя `postgres`.

**Вариант B — вручную в psql или pgAdmin:**
```sql
CREATE ROLE vetline LOGIN PASSWORD 'vetline_secret_2025';
CREATE DATABASE vetline_crm OWNER vetline ENCODING 'UTF8';
GRANT ALL PRIVILEGES ON DATABASE vetline_crm TO vetline;
\connect vetline_crm
GRANT ALL ON SCHEMA public TO vetline;
```

4. Проверьте подключение:
```cmd
psql -U vetline -d vetline_crm -h localhost
```
Должна открыться строка `vetline_crm=>`.

---

## Шаг 4. Запуск приложения

### Быстрый запуск (без Telegram-бота):

Откройте `cmd` в папке проекта и выполните:

```cmd
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Или дважды кликните **`run-local.bat`**.

Через ~20 секунд откройте в браузере: **http://localhost:8080**

### Если хотите использовать Telegram-бот:

Отредактируйте `src/main/resources/application-local.yml`:
```yaml
telegram:
  bot:
    token: 1234567890:ABCDefghIJKlmnOPQrsTUVwxyz   # ← ваш токен от @BotFather
    username: VetlineCrmBot                          # ← username вашего бота
```

---

## Первый вход в систему

После запуска Flyway автоматически применит миграцию `V1__init_schema.sql` и создаст все таблицы.

Нужно создать первого администратора. В psql или pgAdmin выполните:

```sql
\connect vetline_crm

INSERT INTO users (full_name, email, password, role, active)
VALUES (
    'Администратор',
    'admin@vetline.ru',
    '$2a$12$x5KRDrCyQpvS1bnS.MeHx.sMJ0kK9sN.JQiGRuWZ5PQELNEBJHf3K',  -- пароль: Admin1234
    'ADMIN',
    true
);
```

Войдите: **http://localhost:8080/login**
- Email: `admin@vetline.ru`
- Пароль: `Admin1234`

> После входа обязательно смените пароль через профиль.

---

## Смена пароля администратора

Для генерации нового BCrypt-хеша используйте онлайн-инструмент:
https://bcrypt-generator.com (rounds = 12)

Или через Java:
```java
System.out.println(new BCryptPasswordEncoder(12).encode("ВашНовыйПароль"));
```

---

## Структура проекта (Maven Standard Layout)

```
vetline-crm/
├── src/
│   └── main/
│       ├── java/ru/vetline/crm/
│       │   ├── config/          # SecurityConfig и др.
│       │   ├── controller/      # Web-контроллеры (MVC)
│       │   ├── dto/             # DTO объекты
│       │   ├── entity/          # JPA-сущности
│       │   ├── repository/      # Spring Data репозитории
│       │   ├── service/         # Бизнес-логика
│       │   └── telegram/        # Telegram-бот
│       └── resources/
│           ├── db/migration/    # Flyway SQL-миграции
│           ├── static/          # CSS, JS
│           ├── templates/       # Thymeleaf HTML
│           ├── application.yml           # Основная конфигурация
│           └── application-local.yml    # Профиль для локальной разработки
├── db-init.sql          # SQL для создания БД и пользователя
├── run-local.bat        # Скрипт запуска для Windows
├── pom.xml
└── README-WINDOWS.md   # Этот файл
```

---

## Частые проблемы

### `Connection refused` к PostgreSQL
- Убедитесь что PostgreSQL запущен: `services.msc` → PostgreSQL
- Проверьте порт 5432 не занят другим ПО
- В файле `pg_hba.conf` (обычно `C:\Program Files\PostgreSQL\15\data\`) должна быть строка:
  `host all all 127.0.0.1/32 md5`

### `role "vetline" does not exist`
Запустите `db-init.sql` как описано в Шаге 3.

### `Flyway: Found non-empty schema(s) without schema history table`
В `application-local.yml` уже прописан `baseline-on-migrate: true`, ошибка не должна возникнуть.

### `TelegramApiException: Unauthorized`
Токен бота невалиден. Замените на `REPLACE_ME` в `application-local.yml` — приложение запустится без Telegram.

### `Port 8080 already in use`
В `application-local.yml` добавьте:
```yaml
server:
  port: 8090
```

### Java-компиляция на Java 21+ с предупреждениями Lombok
Добавьте в `pom.xml` в секцию `<properties>`:
```xml
<maven.compiler.release>17</maven.compiler.release>
```
(уже прописано через `java.version`)

---

## Сборка JAR (для деплоя)

```cmd
mvn clean package -DskipTests
java -jar target/vetline-crm-1.0.0.jar --spring.profiles.active=local
```
