# -*- coding: utf-8 -*-
"""Генерация 9 рисунков для §2.1 и §2.2 ВКР.

Все изображения — векторно-чистые, в монохромно-серой палитре, с акцентом
синим (#2563eb) и зелёным (#16a34a) — соответствует UI ИС «Ветлайн ВК».
"""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = r'C:\vetline\diagrams'
os.makedirs(OUT, exist_ok=True)

FONT_REG  = r'C:\Windows\Fonts\segoeui.ttf'
FONT_BOLD = r'C:\Windows\Fonts\segoeuib.ttf'
FONT_IT   = r'C:\Windows\Fonts\segoeuii.ttf'

# Палитра (близка к UI приложения)
BG       = '#ffffff'
INK      = '#0f172a'   # тёмно-синий
G900     = '#0f172a'
G700     = '#334155'
G500     = '#64748b'
G400     = '#94a3b8'
G300     = '#cbd5e1'
G200     = '#e2e8f0'
G100     = '#f1f5f9'
G50      = '#f8fafc'
BLUE     = '#2563eb'
BLUE_L   = '#dbeafe'
GREEN    = '#16a34a'
GREEN_L  = '#dcfce7'
AMBER    = '#d97706'
AMBER_L  = '#fef3c7'
RED      = '#dc2626'
RED_L    = '#fee2e2'
PURPLE   = '#a855f7'
PURPLE_L = '#f3e8ff'
TEAL     = '#0d9488'
TEAL_L   = '#ccfbf1'

def f(size, bold=False, italic=False):
    path = FONT_BOLD if bold else (FONT_IT if italic else FONT_REG)
    return ImageFont.truetype(path, size)

def text_w(draw, text, font):
    return draw.textbbox((0, 0), text, font=font)[2]

def text_h(draw, text, font):
    bb = draw.textbbox((0, 0), text, font=font)
    return bb[3] - bb[1]

def rrect(draw, xy, fill=None, outline=None, width=1, radius=8):
    """Скруглённый прямоугольник."""
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)

def box(draw, x, y, w, h, title, sub=None,
        fill='#ffffff', outline=G300, accent=BLUE, font_size=14, sub_size=11,
        bold_title=True):
    rrect(draw, (x, y, x + w, y + h), fill=fill, outline=outline, width=2, radius=10)
    # Левая акцентная полоса
    draw.rectangle((x, y, x + 4, y + h), fill=accent)
    fnt = f(font_size, bold=bold_title)
    fns = f(sub_size)
    title_w = text_w(draw, title, fnt)
    title_lines = [title]
    # Простой word wrap
    if title_w > w - 18:
        words, cur = title.split(' '), ''
        title_lines = []
        for w_ in words:
            test = (cur + ' ' + w_).strip()
            if text_w(draw, test, fnt) > w - 18:
                if cur: title_lines.append(cur)
                cur = w_
            else:
                cur = test
        if cur: title_lines.append(cur)
    cy = y + 8
    for line in title_lines:
        draw.text((x + 12, cy), line, font=fnt, fill=INK)
        cy += text_h(draw, line, fnt) + 2
    if sub:
        draw.text((x + 12, cy + 2), sub, font=fns, fill=G500)

def arrow(draw, x1, y1, x2, y2, color=G500, width=2, label=None, font_size=10,
          bidir=False):
    x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
    draw.line((x1, y1, x2, y2), fill=color, width=width)
    # Стрелка на конце (треугольник)
    import math
    ang = math.atan2(y2 - y1, x2 - x1)
    L = 9
    a1 = ang + math.radians(155)
    a2 = ang - math.radians(155)
    draw.polygon([
        (x2, y2),
        (x2 + L * math.cos(a1), y2 + L * math.sin(a1)),
        (x2 + L * math.cos(a2), y2 + L * math.sin(a2)),
    ], fill=color)
    if bidir:
        ang2 = ang + math.pi
        b1 = ang2 + math.radians(155)
        b2 = ang2 - math.radians(155)
        draw.polygon([
            (x1, y1),
            (x1 + L * math.cos(b1), y1 + L * math.sin(b1)),
            (x1 + L * math.cos(b2), y1 + L * math.sin(b2)),
        ], fill=color)
    if label:
        mx, my = (x1 + x2) // 2, (y1 + y2) // 2
        fnt = f(font_size)
        tw = text_w(draw, label, fnt)
        th = text_h(draw, label, fnt)
        pad = 3
        draw.rectangle((mx - tw // 2 - pad, my - th // 2 - pad,
                        mx + tw // 2 + pad, my + th // 2 + pad), fill='#ffffff')
        draw.text((mx - tw // 2, my - th // 2 - 1), label, font=fnt, fill=G700)

def caption(draw, w, y, text):
    """Подпись внизу (центрированный italic)."""
    fnt = f(12, italic=True)
    tw = text_w(draw, text, fnt)
    draw.text(((w - tw) // 2, y), text, font=fnt, fill=G500)

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.1.1 — Контекстная DFD (уровень 0)
# ═════════════════════════════════════════════════════════════════════════
def fig_2_1_1():
    W, H = 1100, 720
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)
    # Заголовок отсутствует — фронтиспис без шапки

    # Центральный круг — система
    cx, cy, r = W // 2, H // 2, 130
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=BLUE_L, outline=BLUE, width=3)
    fnt = f(20, bold=True)
    fns = f(13)
    txt1 = 'ИС'
    txt2 = '«Ветлайн ВК»'
    sub  = 'Уровень 0'
    d.text((cx - text_w(d, txt1, fnt) // 2, cy - 30), txt1, font=fnt, fill=BLUE)
    d.text((cx - text_w(d, txt2, fnt) // 2, cy - 5), txt2, font=fnt, fill=BLUE)
    d.text((cx - text_w(d, sub, fns) // 2, cy + 30), sub, font=fns, fill=G500)

    # Внешние сущности (квадраты)
    def ext(x, y, w_, h_, label):
        d.rectangle((x, y, x + w_, y + h_), fill=G100, outline=G700, width=2)
        fnt2 = f(13, bold=True)
        # word wrap
        words = label.split(' ')
        lines, cur = [], ''
        for w_w in words:
            test = (cur + ' ' + w_w).strip()
            if text_w(d, test, fnt2) > w_ - 16:
                if cur: lines.append(cur)
                cur = w_w
            else:
                cur = test
        if cur: lines.append(cur)
        ty = y + (h_ - len(lines) * 18) // 2
        for ln in lines:
            d.text((x + (w_ - text_w(d, ln, fnt2)) // 2, ty), ln, font=fnt2, fill=INK)
            ty += 18

    ent_w, ent_h = 180, 70
    # 4 угла
    e1 = (60, 80, ent_w, ent_h, 'Клиент (через сайт)')
    e2 = (60, H - 80 - ent_h, ent_w, ent_h, 'Менеджер отдела продаж')
    e3 = (W - 60 - ent_w, 80, ent_w, ent_h, 'Генеральный директор')
    e4 = (W - 60 - ent_w, H - 80 - ent_h, ent_w, ent_h, 'Telegram Bot API')

    for (x, y, w_, h_, lbl) in [e1, e2, e3, e4]:
        ext(x, y, w_, h_, lbl)

    # Стрелки
    # Клиент → ИС (заявка через webhook)
    arrow(d, 60 + ent_w, 80 + ent_h // 2, cx - r * 0.85, cy - r * 0.5,
          color=BLUE, width=2, label='заявка (POST webhook)')
    # ИС ← Менеджер (входы) и → Менеджер (выводы)
    arrow(d, 60 + ent_w, H - 80 - ent_h // 2, cx - r * 0.85, cy + r * 0.5,
          color=G700, width=2, bidir=True,
          label='UI: заявки, заказы, отчёты')
    # ИС → Директор (дашборд)
    arrow(d, cx + r * 0.85, cy - r * 0.5, W - 60 - ent_w, 80 + ent_h // 2,
          color=PURPLE, width=2, bidir=True,
          label='дашборд / отчёты')
    # ИС → Telegram (уведомления)
    arrow(d, cx + r * 0.85, cy + r * 0.5, W - 60 - ent_w, H - 80 - ent_h // 2,
          color=GREEN, width=2, bidir=True,
          label='SendMessage / updates')

    # Легенда
    d.text((20, H - 20), '□ — внешние сущности; ○ — автоматизируемая ИС', font=f(10, italic=True), fill=G500)

    img.save(os.path.join(OUT, '2_1_1_dfd_context.png'), 'PNG')
    print('OK 2.1.1')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.1.2 — DFD уровня 1 (декомпозиция)
# ═════════════════════════════════════════════════════════════════════════
def fig_2_1_2():
    W, H = 1300, 900
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)

    # Внешние сущности слева/справа
    def ext(x, y, w_, h_, label, fill=G100):
        d.rectangle((x, y, x + w_, y + h_), fill=fill, outline=G700, width=2)
        fnt = f(12, bold=True)
        # центрирование
        words = label.split(' ')
        lines, cur = [], ''
        for w_w in words:
            test = (cur + ' ' + w_w).strip()
            if text_w(d, test, fnt) > w_ - 12:
                if cur: lines.append(cur)
                cur = w_w
            else:
                cur = test
        if cur: lines.append(cur)
        ty = y + (h_ - len(lines) * 16) // 2
        for ln in lines:
            d.text((x + (w_ - text_w(d, ln, fnt)) // 2, ty), ln, font=fnt, fill=INK)
            ty += 16

    # Процессы — круги с номером
    def proc(x, y, code, name, color=BLUE):
        r = 56
        d.ellipse((x - r, y - r, x + r, y + r), fill='#ffffff', outline=color, width=3)
        d.ellipse((x - r + 6, y - r + 6, x + r - 6, y + r - 6), outline=color, width=1)
        fnt_c = f(14, bold=True)
        fnt_n = f(11)
        d.text((x - text_w(d, code, fnt_c) // 2, y - 25), code, font=fnt_c, fill=color)
        # name word wrap
        words = name.split(' ')
        lines, cur = [], ''
        for w_w in words:
            test = (cur + ' ' + w_w).strip()
            if text_w(d, test, fnt_n) > 90:
                if cur: lines.append(cur)
                cur = w_w
            else:
                cur = test
        if cur: lines.append(cur)
        ty = y - 6
        for ln in lines:
            d.text((x - text_w(d, ln, fnt_n) // 2, ty), ln, font=fnt_n, fill=INK)
            ty += 14

    # Хранилища — открытые «коробки»
    def store(x, y, w_, h_, code, label, fill=AMBER_L):
        d.rectangle((x, y, x + w_, y + h_), fill=fill, outline=G700, width=2)
        d.line((x, y, x + w_, y), fill=G700, width=2)
        d.line((x, y + h_, x + w_, y + h_), fill=G700, width=2)
        d.rectangle((x, y, x + 36, y + h_), fill='#ffffff', outline=G700, width=2)
        fnt = f(11, bold=True)
        d.text((x + 6, y + (h_ - 14) // 2), code, font=fnt, fill=G700)
        fnt2 = f(12)
        d.text((x + 44, y + (h_ - 14) // 2), label, font=fnt2, fill=INK)

    # Layout
    # Внешние сущности
    ext(30,  120, 180, 60, 'Клиент (сайт)')
    ext(30,  430, 180, 60, 'Менеджер')
    ext(W - 210, 120, 180, 60, 'Директор')
    ext(W - 210, 720, 180, 60, 'Telegram Bot API')

    # Процессы
    proc(380, 150, '1.0', 'Приём заявки', BLUE)
    proc(380, 430, '2.0', 'Управление ЖЦ заявки', BLUE)
    proc(700, 280, '4.0', 'Заказ поставщику', TEAL)
    proc(700, 580, '3.0', 'Уведомление клиенту', GREEN)
    proc(1020, 280, '5.0', 'Формирование отчёта', PURPLE)
    proc(1020, 540, '6.0', 'Отображение дашборда', PURPLE)

    # Хранилища (внизу)
    sw, sh = 200, 38
    store(80, 800, sw, sh, 'D1', 'Tickets',          AMBER_L)
    store(310, 800, sw, sh, 'D2', 'Clients',          AMBER_L)
    store(540, 800, sw, sh, 'D3', 'TicketHistory',   AMBER_L)
    store(770, 800, sw, sh, 'D4', 'Notifications',   AMBER_L)
    store(1000, 800, sw, sh, 'D5', 'SupplierOrders', AMBER_L)
    store(80, 850, sw, sh, 'D6', 'Suppliers',        AMBER_L)
    store(310, 850, sw, sh, 'D7', 'PlanTargets',     AMBER_L)

    # Стрелки потоков
    arrow(d, 210, 150, 380 - 56, 150, color=BLUE, width=2, label='JSON webhook')
    arrow(d, 210, 460, 380 - 56, 430, color=G700, width=2, label='форма заявки')
    arrow(d, 380, 150 + 56, 380, 430 - 56, color=G500, width=2, label='Новая заявка')
    arrow(d, 380 + 56, 430, 700 - 56, 280, color=TEAL, width=2, label='WORKING → заказ')
    arrow(d, 380 + 56, 430 + 20, 700 - 56, 580 - 30, color=GREEN, width=2,
          label='смена статуса')
    arrow(d, 700 + 56, 580, W - 210, 720 + 30, color=GREEN, width=2,
          label='SendMessage')
    arrow(d, 700, 580 - 56, 700, 280 + 56, color=G500, width=2, bidir=True)
    arrow(d, 380, 430 + 56, 1020 - 56, 540, color=PURPLE, width=2,
          label='агрегация')
    arrow(d, 1020 + 56, 280, W - 210, 150, color=PURPLE, width=2, label='xlsx/pdf')
    arrow(d, 1020 + 56, 540, W - 210, 150 + 30, color=PURPLE, width=2,
          label='HTML')

    # Связи к хранилищам (тонкие)
    arrow(d, 380, 150 + 56, 180, 800,         color=G400, width=1)
    arrow(d, 380, 430 + 56, 180 + 100, 800,   color=G400, width=1)
    arrow(d, 380, 430 + 56, 410, 800,         color=G400, width=1)
    arrow(d, 700, 580 + 56, 870, 800,         color=G400, width=1)
    arrow(d, 700, 280 + 56, 1100, 800,        color=G400, width=1)
    arrow(d, 700, 280 - 56, 180, 850,         color=G400, width=1)
    arrow(d, 1020, 540 + 56, 410, 850,        color=G400, width=1)

    # Легенда
    fnt_leg = f(11, italic=True)
    d.text((20, 30), 'DFD-1: 6 процессов, 7 хранилищ, 4 внешние сущности',
           font=f(13, bold=True), fill=INK)
    d.text((20, 52), 'Цвет процесса: синий — операционный; бирюзовый — закупки; '
           'зелёный — коммуникация; фиолетовый — аналитика',
           font=fnt_leg, fill=G500)

    img.save(os.path.join(OUT, '2_1_2_dfd_level1.png'), 'PNG')
    print('OK 2.1.2')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.1.3 — Компонентная архитектура
# ═════════════════════════════════════════════════════════════════════════
def fig_2_1_3():
    W, H = 1200, 820
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)

    # Заголовки слоёв
    def layer(x, y, w_, h_, name, fill, accent):
        rrect(d, (x, y, x + w_, y + h_), fill=fill, outline=accent, width=2, radius=12)
        fnt = f(12, bold=True)
        d.text((x + 12, y + 8), name, font=fnt, fill=accent)

    # Звено 1: Клиент
    layer(40, 60, 280, 720, 'КЛИЕНТСКОЕ ЗВЕНО', G50, G700)
    box(d, 60, 110, 240, 70, 'Браузер пользователя',
        'HTML + Thymeleaf SSR', fill='#ffffff', outline=G300, accent=BLUE)
    box(d, 60, 200, 240, 60, 'Chart.js (CDN)',
        'визуализация дашборда / отчётов', fill='#ffffff', outline=G300, accent=BLUE)
    box(d, 60, 280, 240, 60, 'CSS / main.css',
        'оформление UI', fill='#ffffff', outline=G300, accent=BLUE)

    # Звено 2: Сервер приложений
    layer(360, 60, 480, 720, 'СЕРВЕР ПРИЛОЖЕНИЙ (Spring Boot 3.2.5)', '#f0f9ff', BLUE)

    box(d, 380, 110, 440, 80, 'Слой контроллеров',
        'Auth / Ticket / SupplierOrder / Report / Dashboard / Admin / Webhook',
        fill='#ffffff', outline=G300, accent=BLUE, font_size=13, sub_size=10)
    box(d, 380, 210, 440, 110,
        'Слой сервисов (бизнес-логика)',
        'TicketService · SupplierOrderService · NotificationService · '
        'ReportService · PdfReportService · DashboardService · AuditService',
        fill='#ffffff', outline=G300, accent=BLUE, font_size=13, sub_size=10)
    box(d, 380, 340, 440, 90, 'Слой репозиториев (Spring Data JPA)',
        '10 интерфейсов: Ticket-, SupplierOrder-, Supplier-, '
        'Client-, User-, Notification-, …',
        fill='#ffffff', outline=G300, accent=BLUE, font_size=13, sub_size=10)
    box(d, 380, 450, 440, 80, 'Объектная модель (JPA Entity)',
        '15 сущностей · 5 ENUM-типов',
        fill='#ffffff', outline=G300, accent=BLUE, font_size=13, sub_size=10)
    box(d, 380, 550, 210, 100, 'Spring Security',
        'BCrypt · RBAC · сессии · CSRF',
        fill='#ffffff', outline=G300, accent=PURPLE, font_size=13, sub_size=10)
    box(d, 610, 550, 210, 100, 'Audit / Logger',
        'AuditLog · Logback',
        fill='#ffffff', outline=G300, accent=AMBER, font_size=13, sub_size=10)
    box(d, 380, 670, 440, 80, 'VetlineBot (long polling)',
        'Telegram-уведомления + команда /start',
        fill='#ffffff', outline=G300, accent=GREEN, font_size=13, sub_size=10)

    # Звено 3: Хранение
    layer(880, 60, 280, 360, 'ХРАНИЛИЩЕ', G50, G700)
    box(d, 900, 110, 240, 110, 'PostgreSQL 15+',
        'JPA + JDBC',
        fill='#ffffff', outline=G300, accent=AMBER, font_size=14, sub_size=11)
    box(d, 900, 240, 240, 80, 'Flyway 10.10',
        'V1__init_schema.sql\nV2__supplier_orders.sql',
        fill='#ffffff', outline=G300, accent=AMBER, font_size=13, sub_size=10)
    box(d, 900, 340, 240, 60, 'pgcrypto',
        'gen_random_uuid()',
        fill='#ffffff', outline=G300, accent=AMBER, font_size=13, sub_size=10)

    # Внешние сервисы
    layer(880, 440, 280, 340, 'ВНЕШНИЕ СЕРВИСЫ', '#f0fdf4', GREEN)
    box(d, 900, 490, 240, 90, 'Telegram Bot API',
        'long polling · SendMessage', fill='#ffffff', outline=G300, accent=GREEN,
        font_size=13, sub_size=10)
    box(d, 900, 600, 240, 80, 'Webhook сайта (POST)',
        'JSON: name / phone / email',
        fill='#ffffff', outline=G300, accent=GREEN, font_size=13, sub_size=10)
    box(d, 900, 700, 240, 70, 'CDN: Chart.js 4.4',
        'cdnjs.cloudflare.com',
        fill='#ffffff', outline=G300, accent=GREEN, font_size=13, sub_size=10)

    # Связи
    arrow(d, 320, 145, 360, 145, color=BLUE, width=2, label='HTTPS')
    arrow(d, 320, 230, 360, 250, color=BLUE, width=2)
    arrow(d, 820, 380, 880, 165, color=AMBER, width=2, label='JDBC')
    arrow(d, 820, 705, 880, 530, color=GREEN, width=2, bidir=True)
    arrow(d, 320, 145, 380, 130, color=BLUE, width=1)
    # webhook вход
    arrow(d, 900, 640, 820, 145, color=GREEN, width=2, label='webhook')

    # Заголовок
    d.text((40, 20), 'Компонентная архитектура ИС «Ветлайн ВК» (трёхзвенная)',
           font=f(15, bold=True), fill=INK)
    d.text((40, 42), 'Клиент (браузер) — Сервер приложений (Spring Boot) — '
                     'СУБД PostgreSQL · Внешние сервисы',
           font=f(11, italic=True), fill=G500)

    img.save(os.path.join(OUT, '2_1_3_components.png'), 'PNG')
    print('OK 2.1.3')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.2.1 — Концептуальная ER-диаграмма
# ═════════════════════════════════════════════════════════════════════════
def fig_2_2_1():
    W, H = 1300, 900
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)

    def ent(x, y, w_, h_, name, attrs, color=BLUE):
        rrect(d, (x, y, x + w_, y + h_), fill='#ffffff', outline=color, width=2, radius=8)
        # шапка
        rrect(d, (x, y, x + w_, y + 32), fill=color, outline=color, width=1, radius=8)
        d.rectangle((x, y + 22, x + w_, y + 32), fill=color)
        fnt = f(13, bold=True)
        d.text((x + 12, y + 8), name, font=fnt, fill='#ffffff')
        # атрибуты
        fa = f(11)
        fk = f(11, bold=True)
        cy = y + 40
        for label, key in attrs:
            kx = x + 12
            if key == 'PK':
                d.text((kx, cy), 'PK', font=fk, fill=color); kx += 24
            elif key == 'FK':
                d.text((kx, cy), 'FK', font=fk, fill=BLUE);  kx += 24
            elif key == 'BK':
                d.text((kx, cy), 'BK', font=fk, fill=AMBER); kx += 24
            d.text((kx, cy), label, font=fa, fill=INK)
            cy += 16

    def link(x1, y1, x2, y2, label='1 : N', side='h'):
        d.line((x1, y1, x2, y2), fill=G500, width=2)
        # Метка кардинальности
        mx, my = (x1 + x2) // 2, (y1 + y2) // 2
        fnt = f(10, bold=True)
        tw = text_w(d, label, fnt)
        d.rectangle((mx - tw // 2 - 3, my - 9, mx + tw // 2 + 3, my + 9), fill='#ffffff')
        d.text((mx - tw // 2, my - 8), label, font=fnt, fill=BLUE)

    # Layout — 12 сущностей
    # Колонка 1
    ent(40,  100, 220, 110, 'User',
        [('id : UUID', 'PK'), ('email : VARCHAR', 'BK'),
         ('full_name : VARCHAR', None), ('role : ENUM', None)], color=PURPLE)

    ent(40, 250, 220, 130, 'Client',
        [('id : UUID', 'PK'), ('phone : VARCHAR', 'BK'),
         ('full_name : VARCHAR', None), ('email : VARCHAR', None),
         ('telegram_id : BIGINT', None)], color=GREEN)

    ent(40, 420, 220, 90, 'Source',
        [('id : UUID', 'PK'), ('name : VARCHAR', 'BK')], color=AMBER)

    ent(40, 545, 220, 100, 'Category',
        [('id : UUID', 'PK'), ('name : VARCHAR', 'BK'),
         ('active : BOOLEAN', None)], color=AMBER)

    # Центральная сущность: Ticket (большая)
    ent(360, 270, 280, 240, 'Ticket',
        [('id : UUID', 'PK'),
         ('number : VARCHAR (VL-YYYY-...)', 'BK'),
         ('client_id : UUID', 'FK'),
         ('manager_id : UUID', 'FK'),
         ('source_id : UUID', 'FK'),
         ('category_id : UUID', 'FK'),
         ('status : ENUM', None),
         ('description : TEXT', None),
         ('archived : BOOLEAN', None),
         ('created_at : TIMESTAMP', None),
         ('updated_at : TIMESTAMP', None)], color=BLUE)

    ent(740, 100, 240, 130, 'TicketHistory',
        [('id : UUID', 'PK'), ('ticket_id : UUID', 'FK'),
         ('event_type : ENUM', None),
         ('status_from : ENUM', None),
         ('status_to : ENUM', None),
         ('user_id : UUID', 'FK')], color=BLUE)

    ent(740, 280, 240, 130, 'Notification',
        [('id : UUID', 'PK'), ('ticket_id : UUID', 'FK'),
         ('client_id : UUID', 'FK'),
         ('message_text : TEXT', None),
         ('delivery_status : ENUM', None)], color=GREEN)

    ent(740, 460, 240, 110, 'PlanTarget',
        [('id : UUID', 'PK'),
         ('year : INT, month : INT', 'BK'),
         ('target_count : INT', None),
         ('set_by_user : UUID', 'FK')], color=PURPLE)

    ent(740, 620, 240, 110, 'AuditLog',
        [('id : UUID', 'PK'), ('user_id : UUID', 'FK'),
         ('action : VARCHAR', None),
         ('entity_type : VARCHAR', None),
         ('entity_id : UUID', None)], color=G700)

    ent(1040, 100, 240, 110, 'Supplier',
        [('id : UUID', 'PK'), ('name : VARCHAR', 'BK'),
         ('country : VARCHAR', None),
         ('contact_person : VARCHAR', None)], color=TEAL)

    ent(1040, 260, 240, 150, 'SupplierOrder',
        [('id : UUID', 'PK'),
         ('number : VARCHAR (ЗП-YY-...)', 'BK'),
         ('ticket_id : UUID (опц.)', 'FK'),
         ('supplier_id : UUID', 'FK'),
         ('manager_id : UUID', 'FK'),
         ('status : ENUM', None),
         ('notes : TEXT', None)], color=TEAL)

    ent(1040, 460, 240, 110, 'SupplierOrderItem',
        [('id : UUID', 'PK'),
         ('supplier_order_id : UUID', 'FK'),
         ('name : VARCHAR', None),
         ('quantity : NUMERIC', None),
         ('unit : VARCHAR', None)], color=TEAL)

    # Связи
    # Client → Ticket
    link(260, 310, 360, 310, '1 : N')
    # User → Ticket
    link(260, 155, 360, 290, '1 : N')
    # Source → Ticket
    link(260, 460, 360, 380, '1 : N')
    # Category → Ticket
    link(260, 590, 360, 430, '1 : N')
    # Ticket → TicketHistory
    link(640, 290, 740, 165, '1 : N')
    # Ticket → Notification
    link(640, 350, 740, 345, '1 : N')
    # User → PlanTarget
    link(260, 180, 740, 510, '1 : N')
    # User → AuditLog
    link(260, 200, 740, 670, '1 : N')
    # Ticket → SupplierOrder
    link(640, 380, 1040, 310, '1 : N (опц.)')
    # Supplier → SupplierOrder
    link(1160, 210, 1160, 260, '1 : N')
    # SupplierOrder → SupplierOrderItem
    link(1160, 410, 1160, 460, '1 : N')
    # User → SupplierOrder
    link(260, 165, 1040, 280, '1 : N')

    # Легенда
    d.text((40, 30), 'Концептуальная ER-диаграмма ИС «Ветлайн ВК»',
           font=f(15, bold=True), fill=INK)
    d.text((40, 56), 'PK — первичный ключ · FK — внешний ключ · BK — бизнес-ключ '
                     '(уникальный)',
           font=f(11, italic=True), fill=G500)
    d.text((40, 75), 'Цвет: синий — основная сущность · бирюзовый — закупки · '
                     'зелёный — клиенты/коммуникации · фиолетовый — пользователи · '
                     'оранжевый — справочники',
           font=f(11, italic=True), fill=G500)

    # Подвал
    d.text((40, H - 30),
           '12 сущностей: 9 в миграции V1__init_schema.sql · 3 в V2__supplier_orders.sql',
           font=f(11, italic=True), fill=G500)

    img.save(os.path.join(OUT, '2_2_1_er_concept.png'), 'PNG')
    print('OK 2.2.1')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.2.2 — Физическая модель (UML Class Diagram)
# ═════════════════════════════════════════════════════════════════════════
def fig_2_2_2():
    W, H = 1300, 900
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)

    def cls(x, y, w_, name, fields, indexes=None, accent=BLUE):
        rh = 26
        fields_h = 14 * len(fields)
        idx_h = 14 * (len(indexes) + 1) if indexes else 0
        h_ = rh + 6 + fields_h + 4 + idx_h + 4
        rrect(d, (x, y, x + w_, y + h_), fill='#ffffff', outline=accent, width=2, radius=4)
        # шапка
        d.rectangle((x, y, x + w_, y + rh), fill=accent)
        fnt = f(12, bold=True)
        d.text((x + 8, y + 6), '«table» ' + name, font=fnt, fill='#ffffff')
        # поля
        cy = y + rh + 4
        fnt_f = f(10)
        fnt_k = f(10, bold=True)
        for typ, name_, mark in fields:
            if mark == 'PK':
                d.text((x + 8, cy), 'PK', font=fnt_k, fill=accent)
            elif mark == 'FK':
                d.text((x + 8, cy), 'FK', font=fnt_k, fill=BLUE)
            label_clean = name_.replace('⤳', '→')
            d.text((x + 30, cy), label_clean, font=fnt_f, fill=INK)
            tw = text_w(d, typ, fnt_f)
            d.text((x + w_ - tw - 8, cy), typ, font=fnt_f, fill=G500)
            cy += 14
        if indexes:
            d.line((x + 4, cy + 2, x + w_ - 4, cy + 2), fill=G300)
            cy += 4
            d.text((x + 8, cy), '« индексы »', font=f(9, italic=True), fill=G500)
            cy += 14
            for idx in indexes:
                d.text((x + 8, cy), '· ' + idx, font=f(9), fill=G500)
                cy += 14
        return h_

    def fk(x1, y1, x2, y2, mult='1..N'):
        d.line((x1, y1, x2, y2), fill=G500, width=1)
        # ромб на одной стороне (агрегация)
        # просто метка
        mx, my = (x1 + x2) // 2, (y1 + y2) // 2
        fnt = f(10)
        tw = text_w(d, mult, fnt)
        d.rectangle((mx - tw // 2 - 2, my - 8, mx + tw // 2 + 2, my + 8), fill='#ffffff')
        d.text((mx - tw // 2, my - 7), mult, font=fnt, fill=BLUE)

    # Заголовок
    d.text((40, 25), 'Физическая модель данных (PostgreSQL 15) — UML Class Diagram',
           font=f(15, bold=True), fill=INK)

    # users
    cls(40, 80, 250, 'users',
        [('UUID PK', 'id', 'PK'),
         ('VARCHAR(200)', 'email UNIQUE', None),
         ('VARCHAR(200)', 'full_name', None),
         ('VARCHAR(100)', 'password', None),
         ('user_role ENUM', 'role', None),
         ('BOOLEAN', 'active', None)], accent=PURPLE)
    # clients
    cls(40, 280, 250, 'clients',
        [('UUID PK', 'id', 'PK'),
         ('VARCHAR(200)', 'full_name', None),
         ('VARCHAR(20)', 'phone UNIQUE', None),
         ('VARCHAR(200)', 'email', None),
         ('BIGINT', 'telegram_id', None)], accent=GREEN)
    # sources & categories
    cls(40, 480, 250, 'sources',
        [('UUID PK', 'id', 'PK'),
         ('VARCHAR(100)', 'name UNIQUE', None)], accent=AMBER)
    cls(40, 580, 250, 'categories',
        [('UUID PK', 'id', 'PK'),
         ('VARCHAR(200)', 'name UNIQUE', None),
         ('BOOLEAN', 'active', None)], accent=AMBER)

    # tickets (центр)
    cls(370, 200, 320, 'tickets',
        [('UUID PK', 'id', 'PK'),
         ('VARCHAR(20) UNIQUE', 'number', None),
         ('UUID FK', 'client_id ⤳ clients', 'FK'),
         ('UUID FK', 'manager_id ⤳ users', 'FK'),
         ('UUID FK', 'source_id ⤳ sources', 'FK'),
         ('UUID FK', 'category_id ⤳ categories', 'FK'),
         ('ticket_status ENUM', 'status', None),
         ('TEXT', 'description', None),
         ('BOOLEAN', 'archived', None),
         ('TIMESTAMP', 'created_at', None),
         ('TIMESTAMP', 'updated_at', None),
         ('TIMESTAMP', 'archived_at', None)],
        indexes=['idx_tickets_status', 'idx_tickets_manager',
                 'idx_tickets_created', 'idx_tickets_archived'],
        accent=BLUE)

    # ticket_history
    cls(750, 80, 280, 'ticket_history',
        [('UUID PK', 'id', 'PK'),
         ('UUID FK', 'ticket_id ⤳ ON CASCADE', 'FK'),
         ('history_event ENUM', 'event_type', None),
         ('ticket_status', 'status_from', None),
         ('ticket_status', 'status_to', None),
         ('UUID FK', 'user_id', 'FK'),
         ('TEXT', 'description', None),
         ('TIMESTAMP', 'created_at', None)],
        indexes=['idx_history_ticket'],
        accent=BLUE)

    # notifications
    cls(750, 330, 280, 'notifications',
        [('UUID PK', 'id', 'PK'),
         ('UUID FK', 'ticket_id', 'FK'),
         ('UUID FK', 'client_id', 'FK'),
         ('TEXT', 'message_text', None),
         ('delivery_status ENUM', 'delivery_status', None),
         ('INT', 'attempts', None)],
        indexes=['idx_notif_ticket', 'idx_notif_status'],
        accent=GREEN)

    # plan_targets / audit_log
    cls(750, 540, 280, 'plan_targets',
        [('UUID PK', 'id', 'PK'),
         ('INT', 'year, month UNIQUE', None),
         ('INT', 'target_count', None),
         ('UUID FK', 'set_by_user', 'FK')], accent=PURPLE)

    cls(750, 700, 280, 'audit_log',
        [('UUID PK', 'id', 'PK'),
         ('UUID FK', 'user_id', 'FK'),
         ('VARCHAR(100)', 'action', None),
         ('VARCHAR(100)', 'entity_type', None),
         ('UUID', 'entity_id', None),
         ('TIMESTAMP', 'created_at', None)], accent=G700)

    # supplier_orders, suppliers, items
    cls(1050, 80, 230, 'suppliers',
        [('UUID PK', 'id', 'PK'),
         ('VARCHAR(200) UNIQUE', 'name', None),
         ('VARCHAR(100)', 'country', None),
         ('VARCHAR(40)', 'phone', None),
         ('VARCHAR(200)', 'email', None),
         ('BOOLEAN', 'active', None)], accent=TEAL)

    cls(1050, 290, 230, 'supplier_orders',
        [('UUID PK', 'id', 'PK'),
         ('VARCHAR(20) UNIQUE', 'number', None),
         ('UUID FK', 'ticket_id?', 'FK'),
         ('UUID FK', 'supplier_id', 'FK'),
         ('UUID FK', 'manager_id', 'FK'),
         ('supplier_order_status', 'status', None),
         ('TEXT', 'notes', None)],
        indexes=['idx_so_status', 'idx_so_supplier',
                 'idx_so_ticket', 'idx_so_created'],
        accent=TEAL)

    cls(1050, 580, 230, 'supplier_order_items',
        [('UUID PK', 'id', 'PK'),
         ('UUID FK', 'supplier_order_id ⤳ ON CASCADE', 'FK'),
         ('VARCHAR(300)', 'name', None),
         ('NUMERIC(12,2)', 'quantity', None),
         ('VARCHAR(20)', 'unit', None),
         ('INT', 'position', None)], accent=TEAL)

    # Связи
    fk(290, 130, 370, 270, '1..N')
    fk(290, 320, 370, 290, '1..N')
    fk(290, 510, 370, 350, '1..N')
    fk(290, 610, 370, 380, '1..N')
    fk(690, 280, 750, 130, '1..N')
    fk(690, 330, 750, 380, '1..N')
    fk(290, 130, 750, 590, '1..N')
    fk(290, 130, 750, 730, '1..N')
    fk(690, 380, 1050, 350, '0..N')
    fk(1160, 200, 1160, 290, '1..N')
    fk(1160, 480, 1160, 580, '1..N')
    fk(290, 130, 1050, 350, '1..N')

    # Подвал
    d.text((40, H - 35),
           'Все таблицы — в схеме public. ENUM-типы: user_role, ticket_status, '
           'history_event, delivery_status, supplier_order_status. '
           'gen_random_uuid() — расширение pgcrypto.',
           font=f(10, italic=True), fill=G500)

    img.save(os.path.join(OUT, '2_2_2_physical_uml.png'), 'PNG')
    print('OK 2.2.2')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.2.3 — Диаграмма состояний UML (Ticket)
# ═════════════════════════════════════════════════════════════════════════
def fig_2_2_3():
    """Диаграмма состояний с ортогональной разводкой и без пересечений подписей."""
    W, H = 1500, 760
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)

    d.text((40, 22), 'Диаграмма состояний UML — жизненный цикл сущности Ticket',
           font=f(16, bold=True), fill=INK)
    d.text((40, 48), '11 состояний; переходы реализованы в TicketStatus.getAllowedTransitions()',
           font=f(11, italic=True), fill=G500)

    # ── Сетка позиций ───────────────────────────────────────────────────
    # 4 колонки × 3 ряда
    BW, BH = 150, 56
    states = {
        # код:                (label,                 cx, cy, color)
        'NEW':            ('Новая',                  150, 150, BLUE),
        'IN_PROGRESS':    ('Принята в работу',       380, 150, BLUE),
        'WORKING':        ('В работе',               650, 350, BLUE),
        'FROZEN':         ('Заморожена',             380, 350, AMBER),
        'ON_APPROVAL':    ('На согласовании',        920, 130, PURPLE),
        'TRANSFERRED':    ('Передана в подразделение', 920, 230, PURPLE),
        'AWAITING_REPLY': ('Ожидает внешнего ответа', 920, 350, PURPLE),
        'IN_DELIVERY':    ('В процессе поставки',    920, 470, PURPLE),
        'DONE':           ('Выполнена',              1220, 350, GREEN),
        'CANCELLED':      ('Отменена клиентом',      650, 590, RED),
        'ARCHIVED':       ('Архивирована',           1220, 590, G500),
    }

    def state(x, y, label, color, terminal=False):
        if terminal:
            # Двойная рамка (UML final) + надпись внутри
            rrect(d, (x - 90, y - 32, x + 90, y + 32),
                  fill='#ffffff', outline=color, width=2, radius=20)
            rrect(d, (x - 84, y - 26, x + 84, y + 26),
                  fill=color, outline=color, radius=16)
            fnt = f(11, bold=True)
            d.text((x - text_w(d, label, fnt) // 2, y - 7),
                   label, font=fnt, fill='#ffffff')
            return
        rrect(d, (x - BW // 2, y - BH // 2, x + BW // 2, y + BH // 2),
              fill='#ffffff', outline=color, width=2, radius=14)
        d.rectangle((x - BW // 2, y - BH // 2, x - BW // 2 + 4, y + BH // 2),
                    fill=color)
        fnt = f(11, bold=True)
        words = label.split(' ')
        lines, cur = [], ''
        for w in words:
            test = (cur + ' ' + w).strip()
            if text_w(d, test, fnt) > BW - 20:
                if cur: lines.append(cur)
                cur = w
            else: cur = test
        if cur: lines.append(cur)
        ty = y - len(lines) * 7
        for ln in lines:
            d.text((x - text_w(d, ln, fnt) // 2, ty), ln, font=fnt, fill=INK)
            ty += 14

    # Старт
    d.ellipse((40, 142, 60, 162), fill=INK, outline=INK)
    arrow(d, 60, 152, 150 - BW // 2, 150, color=INK, width=2)

    for code, (lbl, x, y, color) in states.items():
        state(x, y, lbl, color, terminal=(code == 'ARCHIVED'))

    # ── Ортогональные стрелки (Manhattan routing) ────────────────────────
    def L(*pts, color=G500, width=1, label=None, font_size=10, lbl_pos=0.5,
          lbl_offset=(0, -10)):
        """Ломаная с финальной стрелкой; подпись на любом сегменте."""
        pts = [(int(x), int(y)) for x, y in pts]
        for i in range(len(pts) - 1):
            d.line((pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1]),
                   fill=color, width=width)
        # стрелка на последнем сегменте
        x1, y1 = pts[-2]
        x2, y2 = pts[-1]
        import math
        ang = math.atan2(y2 - y1, x2 - x1)
        L_ = 7
        a1 = ang + math.radians(155)
        a2 = ang - math.radians(155)
        d.polygon([(x2, y2),
                   (int(x2 + L_ * math.cos(a1)), int(y2 + L_ * math.sin(a1))),
                   (int(x2 + L_ * math.cos(a2)), int(y2 + L_ * math.sin(a2)))],
                  fill=color)
        if label:
            # средняя точка вдоль ломаной по доле
            total_len = sum(((pts[i + 1][0] - pts[i][0]) ** 2 +
                             (pts[i + 1][1] - pts[i][1]) ** 2) ** 0.5
                            for i in range(len(pts) - 1))
            target = total_len * lbl_pos
            acc = 0
            mx, my = pts[0]
            for i in range(len(pts) - 1):
                seg = (((pts[i + 1][0] - pts[i][0]) ** 2 +
                        (pts[i + 1][1] - pts[i][1]) ** 2) ** 0.5)
                if acc + seg >= target:
                    t = (target - acc) / seg if seg else 0
                    mx = int(pts[i][0] + (pts[i + 1][0] - pts[i][0]) * t)
                    my = int(pts[i][1] + (pts[i + 1][1] - pts[i][1]) * t)
                    break
                acc += seg
            mx += lbl_offset[0]
            my += lbl_offset[1]
            fnt = f(font_size)
            tw = text_w(d, label, fnt)
            th = text_h(d, label, fnt)
            pad = 3
            d.rectangle((mx - tw // 2 - pad, my - th // 2 - pad,
                         mx + tw // 2 + pad, my + th // 2 + pad),
                        fill='#ffffff')
            d.text((mx - tw // 2, my - th // 2 - 1), label, font=fnt, fill=G700)

    def edge_pts(a, b, anchor_a='r', anchor_b='l'):
        """Точки выхода/входа для коробок."""
        ax, ay, _ = states[a][1], states[a][2], None
        bx, by, _ = states[b][1], states[b][2], None
        ha, hb = BH // 2, BH // 2
        wa, wb = BW // 2, BW // 2
        anc = {
            'l': (-wa, 0), 'r': (wa, 0), 't': (0, -ha), 'b': (0, ha),
            'tr': (wa, -ha + 8), 'tl': (-wa, -ha + 8),
            'br': (wa,  ha - 8), 'bl': (-wa,  ha - 8),
        }
        ax += anc[anchor_a][0]; ay += anc[anchor_a][1]
        bx += anc[anchor_b][0]; by += anc[anchor_b][1]
        return ax, ay, bx, by

    # Прямой главный путь NEW → IN_PROGRESS → WORKING
    ax, ay, bx, by = edge_pts('NEW', 'IN_PROGRESS')
    L((ax, ay), (bx, by), color=BLUE, width=2, label='принять', lbl_offset=(0, -12))

    ax, ay, bx, by = edge_pts('IN_PROGRESS', 'WORKING', 'b', 't')
    L((ax, ay), (ax, ay + 100), (bx, ay + 100), (bx, by),
      color=BLUE, width=2, label='начать работу', lbl_offset=(0, -12))

    # IN_PROGRESS → FROZEN
    ax, ay, bx, by = edge_pts('IN_PROGRESS', 'FROZEN', 'b', 't')
    L((ax, ay), (ax, by - 30), (bx, by - 30), (bx, by),
      color=AMBER, label='заморозить', lbl_offset=(0, -10))

    # FROZEN ↔ WORKING
    ax, ay, bx, by = edge_pts('FROZEN', 'WORKING', 'r', 'l')
    L((ax, ay - 8), (bx, by - 8), color=AMBER, label='возобновить', lbl_offset=(0, -10))
    ax, ay, bx, by = edge_pts('WORKING', 'FROZEN', 'l', 'r')
    L((ax, ay + 8), (bx, by + 8), color=AMBER, label='пауза', lbl_offset=(0, 8))

    # WORKING → 4 ветки в правый столбец
    ax, ay, bx, by = edge_pts('WORKING', 'ON_APPROVAL', 'tr', 'l')
    L((ax, ay), (ax + 80, ay), (ax + 80, by), (bx, by),
      color=PURPLE, label='на согласование', lbl_offset=(0, -10))
    ax, ay, bx, by = edge_pts('WORKING', 'TRANSFERRED', 'r', 'l')
    L((ax, ay - 6), (bx - 30, ay - 6), (bx - 30, by), (bx, by),
      color=PURPLE, label='передать', lbl_offset=(0, -10))
    ax, ay, bx, by = edge_pts('WORKING', 'AWAITING_REPLY', 'r', 'l')
    L((ax, ay + 6), (bx, ay + 6), color=PURPLE, label='внеш. ответ', lbl_offset=(0, -10))
    ax, ay, bx, by = edge_pts('WORKING', 'IN_DELIVERY', 'br', 'l')
    L((ax, ay), (ax + 80, ay), (ax + 80, by), (bx, by),
      color=PURPLE, label='в поставку', lbl_offset=(0, -10))

    # Возвраты в WORKING (тонко, серым)
    for src in ['ON_APPROVAL', 'TRANSFERRED', 'AWAITING_REPLY', 'IN_DELIVERY']:
        sx, sy = states[src][1], states[src][2]
        wx, wy = states['WORKING'][1], states['WORKING'][2]
        # из левого края src вверх/вниз обратно к WORKING нижней частью
        ax = sx - BW // 2
        bx = wx + BW // 2
        L((ax, sy + 12), (bx + 30, sy + 12), (bx + 30, wy + BH // 2 - 4),
          (bx, wy + BH // 2 - 4),
          color=G400, label='возврат', font_size=9, lbl_offset=(0, -10))

    # Все состояния → DONE
    # WORKING → DONE напрямую
    ax, ay, bx, by = edge_pts('WORKING', 'DONE', 'r', 'l')
    L((ax, ay), (bx, by), color=GREEN, width=2, label='выполнено', lbl_offset=(0, -10))
    # ON_APPROVAL → DONE
    ax, ay, bx, by = edge_pts('ON_APPROVAL', 'DONE', 'r', 'tl')
    L((ax, ay), (bx + 50, ay), (bx + 50, by), (bx, by),
      color=GREEN, label='утв.+выполнено', lbl_offset=(0, -10))
    # TRANSFERRED → DONE
    ax, ay, bx, by = edge_pts('TRANSFERRED', 'DONE', 'r', 't')
    L((ax, ay), (bx, ay), (bx, by), color=GREEN, label='выполнено', lbl_offset=(0, -10))
    # AWAITING_REPLY → IN_DELIVERY
    ax, ay, bx, by = edge_pts('AWAITING_REPLY', 'IN_DELIVERY', 'b', 't')
    L((ax, ay), (bx, by), color=PURPLE, label='в поставку', lbl_offset=(0, -10))
    # IN_DELIVERY → DONE
    ax, ay, bx, by = edge_pts('IN_DELIVERY', 'DONE', 'r', 'b')
    L((ax, ay), (bx, ay), (bx, by), color=GREEN, label='доставлено', lbl_offset=(0, -10))

    # Отмены — все в CANCELLED (коротко, красным)
    for src in ['NEW', 'IN_PROGRESS', 'WORKING', 'FROZEN', 'ON_APPROVAL']:
        sx, sy = states[src][1], states[src][2]
        cx, cy_ = states['CANCELLED'][1], states['CANCELLED'][2]
        ax = sx
        ay = sy + BH // 2
        if src == 'WORKING':
            L((ax, ay), (cx, cy_ - BH // 2), color=RED, label='отмена', lbl_offset=(0, -10))
        elif src == 'NEW' or src == 'IN_PROGRESS':
            L((ax, ay), (ax, cy_ - 30), (cx - BW // 2, cy_ - 30), (cx - BW // 2, cy_),
              color=RED, label='отмена', font_size=9, lbl_offset=(0, -10))
        else:
            L((ax, ay), (ax, cy_ - 20), (cx - 30, cy_ - 20), (cx - BW // 2, cy_),
              color=RED, label='отмена', font_size=9, lbl_offset=(0, -10))

    # DONE → ARCHIVED, CANCELLED → ARCHIVED
    ax, ay, bx, by = edge_pts('DONE', 'ARCHIVED', 'b', 't')
    L((ax, ay), (bx, by - 30), color=G500, label='архивация', lbl_offset=(0, -10))
    ax, ay, bx, by = edge_pts('CANCELLED', 'ARCHIVED', 'r', 'l')
    L((ax, ay), (bx - 38, by), color=G500, label='архивация', lbl_offset=(0, -10))

    # Легенда
    d.text((40, H - 35),
           'Цвета: синий — операционные · фиолетовый — согласование/передача · '
           'оранжевый — пауза · зелёный — успех · красный — отмена · '
           'серый — финал (архив).',
           font=f(11, italic=True), fill=G500)

    img.save(os.path.join(OUT, '2_2_3_state_ticket.png'), 'PNG')
    print('OK 2.2.3')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.2.4 — Конвейер формирования отчёта
# ═════════════════════════════════════════════════════════════════════════
def fig_2_2_4():
    W, H = 1400, 600
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)

    d.text((40, 25), 'Конвейер формирования аналитического отчёта',
           font=f(15, bold=True), fill=INK)
    d.text((40, 50), 'GET /reports/export/{type}?from&to&managerId&format',
           font=f(11, italic=True), fill=G500)

    def step(x, y, w_, h_, num, title, sub, color):
        rrect(d, (x, y, x + w_, y + h_), fill='#ffffff', outline=color, width=2, radius=10)
        d.rectangle((x, y, x + 4, y + h_), fill=color)
        fnt_n = f(28, bold=True)
        fnt_t = f(13, bold=True)
        fnt_s = f(11)
        d.text((x + 14, y + 10), num, font=fnt_n, fill=color)
        d.text((x + 60, y + 18), title, font=fnt_t, fill=INK)
        # sub-text wrap
        words = sub.split(' ')
        lines, cur = [], ''
        for w_w in words:
            test = (cur + ' ' + w_w).strip()
            if text_w(d, test, fnt_s) > w_ - 70:
                if cur: lines.append(cur)
                cur = w_w
            else:
                cur = test
        if cur: lines.append(cur)
        cy = y + 40
        for ln in lines:
            d.text((x + 60, cy), ln, font=fnt_s, fill=G700)
            cy += 14

    sw, sh = 250, 130
    yy = 130

    step(40,   yy, sw, sh, '1',
         'HTTP-запрос',
         'ReportController принимает GET-запрос с параметрами: '
         'from, to, managerId, format', BLUE)

    step(310,  yy, sw, sh, '2',
         'RBAC-эффективность',
         'Если роль = MANAGER, managerId = me. format = pdf | xlsx', PURPLE)

    step(580,  yy, sw, sh, '3',
         'JPQL-агрегация',
         'TicketRepository: GROUP BY status / source / category, '
         'COUNT, SUM (CASE)', AMBER)

    step(850,  yy, sw, sh, '4',
         'DTO ReportData',
         'ReportService собирает headers + rows + total в '
         'обобщённую структуру', GREEN)

    step(1120, yy, sw, sh, '5',
         'Рендер выходного файла',
         'XLSX — Apache POI 5.2.5; PDF — OpenPDF 1.3.43 + TTF '
         '(кириллица)', RED)

    # Стрелки между шагами
    for x in [290, 560, 830, 1100]:
        arrow(d, x, yy + sh // 2, x + 20, yy + sh // 2, color=G500, width=2)

    # Веточка: предпросмотр
    yy2 = yy + sh + 40
    step(580, yy2, sw, sh - 40, '4a',
         'HTML-предпросмотр',
         'Шаблон reports/view.html (Thymeleaf) — таблица + Chart.js', GREEN)
    step(850, yy2, sw, sh - 40, '4b',
         'Chart.js',
         'doughnut (sources, categories) или bar', GREEN)
    arrow(d, 705, yy + sh, 705, yy2, color=G500, width=2,
          label='view вместо export')
    arrow(d, 830, yy2 + (sh - 40) // 2, 850, yy2 + (sh - 40) // 2, color=G500, width=2)

    # Подвал — пять отчётов
    yy3 = yy2 + sh + 30
    rrect(d, (40, yy3, W - 40, yy3 + 80), fill=G50, outline=G300, width=1, radius=8)
    d.text((50, yy3 + 12),
           'Реализованные отчёты:',
           font=f(12, bold=True), fill=INK)
    items = ['О-01 По статусам',
             'О-02 По конверсиям',
             'О-03 По источникам',
             'О-04 По категориям',
             'О-05 По эффективности менеджеров (только директор)']
    cx = 50
    for i, it in enumerate(items):
        d.text((cx, yy3 + 36), it, font=f(11), fill=G700)
        cx += text_w(d, it, f(11)) + 30

    img.save(os.path.join(OUT, '2_2_4_report_pipeline.png'), 'PNG')
    print('OK 2.2.4')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.2.5 — Mock-скриншот дашборда
# ═════════════════════════════════════════════════════════════════════════
def fig_2_2_5_dashboard():
    W, H = 1280, 920
    img = Image.new('RGB', (W, H), '#f8fafc')
    d = ImageDraw.Draw(img)

    # Navbar
    d.rectangle((0, 0, W, 56), fill='#ffffff', outline=G200, width=1)
    d.line((0, 56, W, 56), fill=G200, width=1)
    d.text((24, 18), 'Ветлайн ВК', font=f(16, bold=True), fill=BLUE)
    nav_items = [('Заявки', False), ('Заказы поставщикам', False),
                 ('Отчёты', False), ('Дашборд', True), ('Архив', False),
                 ('Администрирование', False)]
    cx = 200
    for label, active in nav_items:
        tw = text_w(d, label, f(13, bold=active))
        if active:
            rrect(d, (cx - 8, 14, cx + tw + 8, 38), fill='#eff6ff', radius=6, outline='#eff6ff')
            d.text((cx, 18), label, font=f(13, bold=True), fill=BLUE)
        else:
            d.text((cx, 18), label, font=f(13), fill=G700)
        cx += tw + 24
    d.text((W - 160, 18), 'director@vetline.ru', font=f(12), fill=G500)

    # Контейнер
    pad = 24
    cy = 56 + pad

    # Заголовок и переключатели периода
    d.text((pad, cy), 'Дашборд', font=f(24, bold=True), fill=G900)
    d.text((pad, cy + 32), 'Оперативная сводка по отделу продаж',
           font=f(12), fill=G500)
    # Pills
    pills = [('Неделя', False), ('Месяц', True), ('Квартал', False), ('Год', False)]
    px = W - pad - 10
    for label, active in pills[::-1]:
        tw = text_w(d, label, f(13))
        bx2 = px
        bx1 = px - tw - 28
        fill = G900 if active else G100
        col_t = '#ffffff' if active else G700
        rrect(d, (bx1, cy + 4, bx2, cy + 36), fill=fill, outline=fill, radius=20)
        d.text((bx1 + 14, cy + 12), label, font=f(13, bold=active), fill=col_t)
        px = bx1 - 6
    d.text((px - 60, cy + 12), 'Период:', font=f(12, bold=True), fill=G500)

    cy += 60

    # KPI cards
    def kpi(x, w_, color, label, value, sub, sub_color=G500):
        h_ = 96
        rrect(d, (x, cy, x + w_, cy + h_), fill='#ffffff', outline=G200, width=1, radius=8)
        d.rectangle((x, cy, x + w_, cy + 3), fill=color)
        d.text((x + 16, cy + 14), label, font=f(11, bold=True), fill=G500)
        d.text((x + 16, cy + 32), value, font=f(28, bold=True), fill=G900)
        d.text((x + 16, cy + 70), sub, font=f(11), fill=sub_color)

    card_w = (W - pad * 2 - 14 * 3) // 4
    kpi(pad, card_w, BLUE, 'ВСЕГО ЗАЯВОК ЗА ПЕРИОД', '134',
        '▲ +12% к прошлому периоду', GREEN)
    kpi(pad + (card_w + 14), card_w, GREEN, 'АКТИВНЫЕ ЗАЯВКИ', '47',
        'сейчас в работе')
    kpi(pad + (card_w + 14) * 2, card_w, AMBER, 'КОНВЕРСИЯ В ВЫПОЛНЕННЫЕ', '67.9%',
        '91 из 134 заявок')
    kpi(pad + (card_w + 14) * 3, card_w, PURPLE, 'РОСТ К ПРОШЛОМУ ПЕРИОДУ', '+12.4%',
        'Прошлый период: 119 заявок', GREEN)

    cy += 96 + 14

    # Ряд 1: график + воронка
    chart_w = int((W - pad * 2 - 14) * 0.6)
    funnel_w = (W - pad * 2 - 14) - chart_w
    chart_h = 280

    # Карточка графика
    rrect(d, (pad, cy, pad + chart_w, cy + chart_h), fill='#ffffff',
          outline=G200, width=1, radius=8)
    d.rectangle((pad, cy, pad + chart_w, cy + 36), fill=G50)
    d.line((pad, cy + 36, pad + chart_w, cy + 36), fill=G200)
    d.text((pad + 16, cy + 10), 'Динамика обращений и закрытых заявок',
           font=f(13, bold=True), fill=G900)
    # Легенда
    d.ellipse((pad + 16, cy + 50, pad + 26, cy + 60), fill=BLUE, outline=BLUE)
    d.text((pad + 32, cy + 48), 'Новые заявки', font=f(11), fill=G700)
    d.ellipse((pad + 130, cy + 50, pad + 140, cy + 60), fill=GREEN, outline=GREEN)
    d.text((pad + 146, cy + 48), 'Закрытые заявки', font=f(11), fill=G700)

    # Линии
    import random
    random.seed(7)
    chart_x0, chart_y0 = pad + 40, cy + 80
    chart_x1, chart_y1 = pad + chart_w - 20, cy + chart_h - 30
    pts = 20
    new_data = [3 + random.randint(0, 6) + (i // 5) for i in range(pts)]
    new_data[10] = 9
    done_data = [max(0, n - 2 - random.randint(0, 3)) for n in new_data]
    # Сетка
    for i in range(5):
        ly = chart_y0 + i * (chart_y1 - chart_y0) // 4
        d.line((chart_x0, ly, chart_x1, ly), fill=G200, width=1)
    # Линии и заливка
    def draw_series(data, color, fill_alpha=0.1):
        max_v = max(data + new_data) + 1
        coords = []
        for i, v in enumerate(data):
            x = chart_x0 + i * (chart_x1 - chart_x0) // (pts - 1)
            y = chart_y1 - int(v * (chart_y1 - chart_y0) / max_v)
            coords.append((x, y))
        # заливка
        fill_pts = coords + [(chart_x1, chart_y1), (chart_x0, chart_y1)]
        from PIL import ImageColor
        rgb = ImageColor.getrgb(color)
        fillc = (*rgb, int(255 * fill_alpha))
        overlay = Image.new('RGBA', img.size, (0, 0, 0, 0))
        ovd = ImageDraw.Draw(overlay)
        ovd.polygon(fill_pts, fill=fillc)
        img.paste(overlay, (0, 0), overlay)
        # линия
        for i in range(len(coords) - 1):
            d.line((coords[i][0], coords[i][1], coords[i + 1][0], coords[i + 1][1]),
                   fill=color, width=2)
        for x, y in coords:
            d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=color, outline=color)

    draw_series(new_data, BLUE, 0.10)
    draw_series(done_data, GREEN, 0.08)

    # Воронка
    rrect(d, (pad + chart_w + 14, cy, pad + chart_w + 14 + funnel_w, cy + chart_h),
          fill='#ffffff', outline=G200, width=1, radius=8)
    d.rectangle((pad + chart_w + 14, cy, pad + chart_w + 14 + funnel_w, cy + 36),
                fill=G50)
    d.text((pad + chart_w + 30, cy + 10), 'Воронка по статусам',
           font=f(13, bold=True), fill=G900)
    funnel_data = [('Новые', 134, '#1e3a8a'),
                   ('Принята в работу', 118, '#2563eb'),
                   ('В работе', 96, '#3b82f6'),
                   ('На согласовании', 74, '#60a5fa'),
                   ('В процессе поставки', 51, '#93c5fd'),
                   ('Выполнена', 47, GREEN),
                   ('Отменена', 8, RED)]
    fy = cy + 56
    f_x = pad + chart_w + 28
    f_max_w = funnel_w - 36
    f_max = max(v for _, v, _ in funnel_data)
    for label, v, c in funnel_data:
        bar_w = max(60, int(f_max_w * v / f_max))
        rrect(d, (f_x, fy, f_x + bar_w, fy + 28), fill=c, outline=c, radius=6)
        d.text((f_x + 10, fy + 6), label, font=f(11, bold=True), fill='#ffffff')
        d.text((f_x + bar_w - 30, fy + 6), str(v), font=f(11, bold=True), fill='#ffffff')
        fy += 32

    cy += chart_h + 14

    # Ряд 2: План/Факт + Топ менеджеры
    pf_w = chart_w
    tm_w = funnel_w
    pf_h = 220

    rrect(d, (pad, cy, pad + pf_w, cy + pf_h), fill='#ffffff',
          outline=G200, width=1, radius=8)
    d.rectangle((pad, cy, pad + pf_w, cy + 36), fill=G50)
    d.text((pad + 16, cy + 10), 'План / Факт', font=f(13, bold=True), fill=G900)

    def pf_row(y, label, val, total, color):
        d.text((pad + 16, y), label, font=f(12), fill=G700)
        bar_x0 = pad + 200
        bar_x1 = pad + pf_w - 110
        d.rectangle((bar_x0, y + 4, bar_x1, y + 12), fill=G100, outline=G100)
        pct = min(100, int(val * 100 / max(total, 1)))
        d.rectangle((bar_x0, y + 4, bar_x0 + (bar_x1 - bar_x0) * pct // 100, y + 12),
                    fill=color, outline=color)
        d.text((bar_x1 + 12, y), f'{val} / {total}',
               font=f(12, bold=True), fill=G900)

    pf_row(cy + 70,  'Количество заявок',  134, 150, BLUE)
    pf_row(cy + 110, 'Выполненные',         91, 134, GREEN)
    pf_row(cy + 150, 'Конверсия',           67, 100, PURPLE)

    # Топ менеджеры
    rrect(d, (pad + chart_w + 14, cy, pad + chart_w + 14 + tm_w, cy + pf_h),
          fill='#ffffff', outline=G200, width=1, radius=8)
    d.rectangle((pad + chart_w + 14, cy, pad + chart_w + 14 + tm_w, cy + 36),
                fill=G50)
    d.text((pad + chart_w + 30, cy + 10), 'Топ менеджеры периода',
           font=f(13, bold=True), fill=G900)
    th_y = cy + 50
    d.text((pad + chart_w + 30, th_y), 'Менеджер', font=f(11, bold=True), fill=G500)
    d.text((pad + chart_w + 14 + tm_w - 130, th_y), 'Заявок',
           font=f(11, bold=True), fill=G500)
    d.text((pad + chart_w + 14 + tm_w - 70, th_y), 'Закрыто',
           font=f(11, bold=True), fill=G500)
    rows = [('Петрова М.А.', 42, 18),
            ('Сидоров К.В.', 38, 15),
            ('Козлова Е.Н.', 31, 12),
            ('Новиков П.С.', 23, 6)]
    ry = th_y + 24
    for nm, t, dn in rows:
        d.text((pad + chart_w + 30, ry), nm, font=f(12), fill=G700)
        d.text((pad + chart_w + 14 + tm_w - 130, ry), str(t), font=f(12), fill=G700)
        d.text((pad + chart_w + 14 + tm_w - 70, ry), str(dn),
               font=f(12, bold=True), fill=GREEN)
        d.line((pad + chart_w + 30, ry + 22, pad + chart_w + 14 + tm_w - 30, ry + 22),
               fill=G100)
        ry += 36

    img.save(os.path.join(OUT, '2_2_5_dashboard.png'), 'PNG')
    print('OK 2.2.5')

# ═════════════════════════════════════════════════════════════════════════
#  Рисунок 2.2.6 — Mock-скриншот предпросмотра отчёта
# ═════════════════════════════════════════════════════════════════════════
def fig_2_2_6_report():
    W, H = 1280, 760
    img = Image.new('RGB', (W, H), '#f8fafc')
    d = ImageDraw.Draw(img)

    # Navbar
    d.rectangle((0, 0, W, 56), fill='#ffffff', outline=G200, width=1)
    d.line((0, 56, W, 56), fill=G200, width=1)
    d.text((24, 18), 'Ветлайн ВК', font=f(16, bold=True), fill=BLUE)
    nav_items = [('Заявки', False), ('Заказы поставщикам', False),
                 ('Отчёты', True), ('Дашборд', False), ('Архив', False),
                 ('Администрирование', False)]
    cx = 200
    for label, active in nav_items:
        tw = text_w(d, label, f(13, bold=active))
        if active:
            rrect(d, (cx - 8, 14, cx + tw + 8, 38), fill='#eff6ff', radius=6, outline='#eff6ff')
            d.text((cx, 18), label, font=f(13, bold=True), fill=BLUE)
        else:
            d.text((cx, 18), label, font=f(13), fill=G700)
        cx += tw + 24

    pad = 24
    cy = 56 + pad

    # Header
    d.text((pad, cy), 'О-03 По источникам обращений: 01.04.2026 — 26.04.2026',
           font=f(20, bold=True), fill=G900)
    # Buttons
    bx = W - pad
    for label, primary in [('↓ pdf', 2), ('↓ xlsx', 1), ('← К списку отчётов', 0)]:
        tw = text_w(d, label, f(12))
        bw = tw + 24
        bx -= bw + 8
        if primary == 1:
            rrect(d, (bx, cy + 5, bx + bw, cy + 33), fill=BLUE, outline=BLUE, radius=8)
            d.text((bx + 12, cy + 11), label, font=f(12, bold=True), fill='#ffffff')
        elif primary == 2:
            rrect(d, (bx, cy + 5, bx + bw, cy + 33), fill=G100, outline=G200, radius=8)
            d.text((bx + 12, cy + 11), label, font=f(12, bold=True), fill=G700)
        else:
            rrect(d, (bx, cy + 5, bx + bw, cy + 33), fill='#ffffff', outline=G200, radius=8)
            d.text((bx + 12, cy + 11), label, font=f(12), fill=G700)

    cy += 60

    # Две карточки: таблица + диаграмма
    table_w = int((W - pad * 2 - 16) * 0.58)
    chart_w = (W - pad * 2 - 16) - table_w
    card_h = 380

    # Таблица
    rrect(d, (pad, cy, pad + table_w, cy + card_h), fill='#ffffff',
          outline=G200, radius=8, width=1)
    d.rectangle((pad, cy, pad + table_w, cy + 36), fill=G50)
    d.text((pad + 16, cy + 10), 'Табличное представление',
           font=f(13, bold=True), fill=G900)
    # Заголовки
    headers = ['Источник', 'Количество', '%']
    col_w = [int(table_w * 0.55), int(table_w * 0.25), int(table_w * 0.20)]
    hy = cy + 50
    cx = pad
    for i, h in enumerate(headers):
        d.text((cx + 16, hy), h, font=f(11, bold=True), fill=G500)
        cx += col_w[i]
    d.line((pad + 8, hy + 22, pad + table_w - 8, hy + 22), fill=G300, width=2)

    rows = [('Сайт',           48, '35.8%'),
            ('Телефон',        32, '23.9%'),
            ('Мессенджер',     28, '20.9%'),
            ('E-mail',         16, '11.9%'),
            ('Личный визит',    7,  '5.2%'),
            ('Другое',          3,  '2.2%')]
    ry = hy + 36
    for label, cnt, pct in rows:
        cx = pad
        d.text((cx + 16, ry), label, font=f(12), fill=G700)
        cx += col_w[0]
        d.text((cx + 16, ry), str(cnt), font=f(12), fill=G700)
        cx += col_w[1]
        d.text((cx + 16, ry), pct, font=f(12), fill=G700)
        d.line((pad + 8, ry + 24, pad + table_w - 8, ry + 24), fill=G100)
        ry += 32
    # Итого
    d.line((pad + 8, ry + 6, pad + table_w - 8, ry + 6), fill=G300, width=2)
    ry += 14
    d.text((pad + 16, ry), 'ИТОГО', font=f(12, bold=True), fill=G900)
    d.text((pad + col_w[0] + 16, ry), '134', font=f(12, bold=True), fill=G900)

    # Диаграмма (doughnut)
    cx0 = pad + table_w + 16
    rrect(d, (cx0, cy, cx0 + chart_w, cy + card_h), fill='#ffffff',
          outline=G200, radius=8, width=1)
    d.rectangle((cx0, cy, cx0 + chart_w, cy + 36), fill=G50)
    d.text((cx0 + 16, cy + 10), 'Визуализация',
           font=f(13, bold=True), fill=G900)

    # Pie/doughnut
    chart_cx = cx0 + 130
    chart_cy = cy + 200
    radius_o = 100
    radius_i = 55

    palette = [BLUE, AMBER, GREEN, PURPLE, '#ec4899', G500]
    angles = []
    total = sum(v for _, v, _ in rows)
    a0 = -90
    for i, (lbl, v, _) in enumerate(rows):
        a1 = a0 + v / total * 360
        d.pieslice((chart_cx - radius_o, chart_cy - radius_o,
                    chart_cx + radius_o, chart_cy + radius_o),
                   start=a0, end=a1, fill=palette[i], outline='#ffffff', width=2)
        angles.append((a0, a1, palette[i]))
        a0 = a1
    # внутренний круг
    d.ellipse((chart_cx - radius_i, chart_cy - radius_i,
               chart_cx + radius_i, chart_cy + radius_i), fill='#ffffff')
    # центральная подпись
    d.text((chart_cx - 18, chart_cy - 10), '134', font=f(20, bold=True), fill=G900)
    d.text((chart_cx - 18, chart_cy + 14), 'итого', font=f(11), fill=G500)

    # Легенда справа от диаграммы
    lx = chart_cx + radius_o + 30
    ly = cy + 100
    for i, (lbl, v, pct) in enumerate(rows):
        d.rectangle((lx, ly + 4, lx + 14, ly + 18), fill=palette[i])
        d.text((lx + 22, ly + 2), f'{lbl} — {pct}', font=f(11), fill=G700)
        ly += 28

    img.save(os.path.join(OUT, '2_2_6_report_view.png'), 'PNG')
    print('OK 2.2.6')

# ─── Запуск всех ────────────────────────────────────────────────────────
fig_2_1_1()
fig_2_1_2()
fig_2_1_3()
fig_2_2_1()
fig_2_2_2()
fig_2_2_3()
fig_2_2_4()
fig_2_2_5_dashboard()
fig_2_2_6_report()

print()
print('Все 9 рисунков сохранены в', OUT)
for f_ in sorted(os.listdir(OUT)):
    p = os.path.join(OUT, f_)
    print(f'  {f_:35s}  {os.path.getsize(p):>8} bytes')
