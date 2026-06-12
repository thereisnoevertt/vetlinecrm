@echo off
chcp 65001 >nul
echo.
echo ====================================================
echo   Vetline CRM — локальный запуск (без Docker)
echo ====================================================
echo.

REM Проверяем JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo [ОШИБКА] Переменная JAVA_HOME не задана!
    echo Установите JDK 17+ и пропишите JAVA_HOME.
    pause
    exit /b 1
)

REM Проверяем версию Java
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr /i "17\|21\|22\|23" >nul
if errorlevel 1 (
    echo [ПРЕДУПРЕЖДЕНИЕ] Рекомендуется Java 17+. Текущая версия:
    "%JAVA_HOME%\bin\java" -version
)

echo [OK] Java найдена: %JAVA_HOME%
echo.
echo Запуск с профилем 'local'...
echo (Ctrl+C для остановки)
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=local

pause
