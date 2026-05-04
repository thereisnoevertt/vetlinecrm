# Инструкция по развёртыванию Vetline CRM

## Содержание
1. [Требования](#1-требования)
2. [Быстрый старт через Docker](#2-быстрый-старт-через-docker-рекомендуется)
3. [Настройка Telegram-бота](#3-настройка-telegram-бота)
4. [Настройка интеграции с сайтом webhook](#4-настройка-интеграции-с-сайтом-webhook)
5. [Первый вход в систему](#5-первый-вход-в-систему)
6. [Настройка HTTPS через Nginx](#6-настройка-https-через-nginx)
7. [Резервное копирование](#7-резервное-копирование)
8. [Ручная установка без Docker](#8-ручная-установка-без-docker)
9. [Диагностика проблем](#9-диагностика-проблем)

---

## 1. Требования

| Параметр | Минимум |
|---|---|
| ОС | Ubuntu Server 22.04 LTS |
| CPU | 4 ядра / 2.5 ГГц |
| RAM | 8 ГБ |
| Диск | SSD 200 ГБ |
| Интернет | 50 Мбит/с |
| Docker | 24+ |
| Docker Compose | 2.20+ |

---

## 2. Быстрый старт через Docker (рекомендуется)

### Шаг 1 — Установить Docker

```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
docker --version          # проверить установку
docker compose version
```

### Шаг 2 — Скопировать проект на сервер

```bash
# Через scp (с вашего компьютера):
scp -r vetline-crm/ user@your-server:/opt/vetline-crm

# Перейти в директорию:
cd /opt/vetline-crm
```

### Шаг 3 — Создать файл переменных окружения

```bash
cp .env.example .env
nano .env
```

Заполните `.env`:

```dotenv
DB_PASSWORD=ВашНадёжныйПароль2025
TELEGRAM_BOT_TOKEN=1234567890:ABCdef...   # токен от @BotFather
TELEGRAM_BOT_USERNAME=VetlineCrmBot
WEBHOOK_SECRET_TOKEN=случайная_строка_для_сайта
APP_BASE_URL=https://crm.vetline.ru
```

### Шаг 4 — Запустить систему

```bash
docker compose up -d --build
```

Первый запуск занимает 5–10 минут (скачивание зависимостей и сборка).

### Шаг 5 — Убедиться, что система запущена

```bash
docker compose ps
# vetline_db  — running (healthy)
# vetline_crm — running (healthy)

docker compose logs -f app
# Ждать: "Started VetlineCrmApplication in X.XXX seconds"
```

Откройте браузер: **http://ваш-сервер:8080**

---

## 3. Настройка Telegram-бота

### Создать бота

1. В Telegram найти `@BotFather`
2. Отправить `/newbot`
3. Ввести имя: `Vetline CRM`
4. Ввести username: `VetlineCrmBot` (должен заканчиваться на `bot`)
5. Скопировать токен вида `1234567890:ABCdef...`
6. Вставить токен в `.env` → `TELEGRAM_BOT_TOKEN`

### Как клиент подключает уведомления

1. Клиент находит бота в Telegram по имени `@VetlineCrmBot`
2. Нажимает `/start` — бот отвечает с его Telegram ID
3. Клиент сообщает ID менеджеру
4. Менеджер в карточке заявки → блок «Клиент» → поле «Telegram ID» → сохраняет
5. Кнопка «Уведомить клиента» становится активной

---

## 4. Настройка интеграции с сайтом (webhook)

Передайте разработчику сайта:

```
URL:     https://crm.vetline.ru/api/webhook/ticket
Метод:   POST
Заголовок: X-Webhook-Token: <значение WEBHOOK_SECRET_TOKEN из .env>
Content-Type: application/json

Тело запроса:
{
  "name": "Иванов Иван Иванович",
  "phone": "+7(999)999-99-99",
  "email": "client@example.com",        (необязательно)
  "description": "Нужен УЗИ-сканер"    (необязательно)
}

Ответ при успехе:
{"success": true, "data": "VL-2025-000001"}
```

### Проверить webhook вручную

```bash
curl -X POST https://crm.vetline.ru/api/webhook/ticket \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Token: ваш_секретный_токен" \
  -d '{"name":"Тест","phone":"+7(900)000-00-01","description":"Тест с сайта"}'
```

---

## 5. Первый вход в систему

Откройте браузер: `http://ваш-сервер:8080/login`

| Роль | E-mail | Пароль |
|---|---|---|
| Администратор | admin@vetline.ru | Admin2025! |
| Генеральный директор | director@vetline.ru | Director2025! |

> ⚠️ **Немедленно смените пароли** после первого входа через раздел «Администрирование».

### Создание менеджеров

1. Войти как `admin@vetline.ru`
2. Перейти: Администрирование → Пользователи → «+ Добавить пользователя»
3. Указать ФИО, корпоративный e-mail, пароль, роль «Менеджер»

---

## 6. Настройка HTTPS через Nginx

```bash
sudo apt install -y nginx certbot python3-certbot-nginx

sudo nano /etc/nginx/sites-available/vetline-crm
```

```nginx
server {
    listen 80;
    server_name crm.vetline.ru;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 10m;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/vetline-crm /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# Получить SSL-сертификат (HTTPS, автоматически бесплатно)
sudo certbot --nginx -d crm.vetline.ru
```

Сертификат обновляется автоматически каждые 90 дней.

---

## 7. Резервное копирование

```bash
sudo nano /opt/backup-vetline.sh
```

```bash
#!/bin/bash
DIR=/var/backups/vetline-crm
mkdir -p $DIR
DATE=$(date +%Y%m%d_%H%M%S)
docker exec vetline_db pg_dump -U vetline vetline_crm | gzip > $DIR/vetline_$DATE.sql.gz
find $DIR -name "*.sql.gz" -mtime +30 -delete
echo "Резервная копия: $DIR/vetline_$DATE.sql.gz"
```

```bash
sudo chmod +x /opt/backup-vetline.sh

# Добавить в cron: ежедневно в 02:00
sudo crontab -e
# Добавить строку:
0 2 * * * /opt/backup-vetline.sh >> /var/log/vetline-backup.log 2>&1
```

### Восстановление

```bash
gunzip -c /var/backups/vetline-crm/vetline_20250101_020000.sql.gz \
  | docker exec -i vetline_db psql -U vetline vetline_crm
```

---

## 8. Ручная установка без Docker

```bash
# 1. Java 17
sudo apt install -y openjdk-17-jdk

# 2. PostgreSQL
sudo apt install -y postgresql-15
sudo -u postgres psql -c "CREATE USER vetline WITH PASSWORD 'пароль';"
sudo -u postgres psql -c "CREATE DATABASE vetline_crm OWNER vetline;"

# 3. Сборка
sudo apt install -y maven
cd /opt/vetline-crm
mvn clean package -DskipTests

# 4. Запуск как systemd-сервис
sudo nano /etc/systemd/system/vetline-crm.service
```

```ini
[Unit]
Description=Vetline CRM
After=network.target postgresql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/vetline-crm
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar target/vetline-crm-1.0.0.jar
Restart=always
RestartSec=10
Environment=SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/vetline_crm
Environment=SPRING_DATASOURCE_USERNAME=vetline
Environment=SPRING_DATASOURCE_PASSWORD=ваш_пароль
Environment=TELEGRAM_BOT_TOKEN=ваш_токен
Environment=TELEGRAM_BOT_USERNAME=VetlineCrmBot
Environment=WEBHOOK_SECRET_TOKEN=секретный_токен
Environment=APP_BASE_URL=https://crm.vetline.ru

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now vetline-crm
sudo journalctl -u vetline-crm -f
```

---

## 9. Диагностика проблем

```bash
# Логи приложения (Docker)
docker compose logs -f app

# Логи БД
docker compose logs -f postgres

# Перезапуск
docker compose restart app

# Полная пересборка после изменений кода
docker compose down && docker compose up -d --build

# Проверить webhook
curl -v -X POST http://localhost:8080/api/webhook/ticket \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Token: webhook_secret_change_me" \
  -d '{"name":"Тест","phone":"+7(900)000-0001"}'

# Проверить Telegram-бот
curl https://api.telegram.org/bot<ВАШ_ТОКЕН>/getMe
```

| Проблема | Решение |
|---|---|
| Контейнер не стартует | `docker compose logs postgres` — проверить ошибки БД |
| Ошибка 403 при webhook | Проверить `X-Webhook-Token` совпадает с `WEBHOOK_SECRET_TOKEN` |
| Telegram-уведомления не приходят | Убедиться, что `TELEGRAM_BOT_TOKEN` корректный и бот не заблокирован |
| Страница не загружается | `docker compose ps` — убедиться что `vetline_crm` имеет статус `healthy` |
