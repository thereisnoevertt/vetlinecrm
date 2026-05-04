// Vetline CRM — main.js

document.addEventListener('DOMContentLoaded', () => {
    // Автоскрытие flash-уведомлений через 5 секунд
    document.querySelectorAll('.alert').forEach(el => {
        setTimeout(() => {
            el.style.transition = 'opacity .5s';
            el.style.opacity = '0';
            setTimeout(() => el.remove(), 500);
        }, 5000);
    });

    // Кликабельные строки таблицы (через data-href)
    document.querySelectorAll('[data-href]').forEach(el => {
        el.style.cursor = 'pointer';
        el.addEventListener('click', () => {
            window.location = el.getAttribute('data-href');
        });
    });
});
