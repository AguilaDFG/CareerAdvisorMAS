@echo off
setlocal enabledelayedexpansion
title CareerAdvisor-MAS

:: ================================================================
::  CareerAdvisor-MAS  —  Lanzador Windows
::  Doble clic para compilar (si es necesario) y ejecutar.
:: ================================================================

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "SRC=%ROOT%\src"
set "BIN=%ROOT%\bin"
set "LIB=%ROOT%\lib\jade.jar"
set "MAIN=es.upm.careeradvisor.MainLauncher"

:: ── 1. Comprobar Java ─────────────────────────────────────────
where java  >nul 2>&1 || goto :NO_JAVA
where javac >nul 2>&1 || goto :NO_JAVAC

:: ── 2. Comprobar jade.jar ─────────────────────────────────────
if not exist "%LIB%" goto :NO_JADE

:: ── 3. Compilar si bin no existe o está vacío ─────────────────
set "NEEDS_COMPILE=0"
if not exist "%BIN%" set "NEEDS_COMPILE=1"
if "%NEEDS_COMPILE%"=="0" (
    dir /b /s "%BIN%\*.class" >nul 2>&1 || set "NEEDS_COMPILE=1"
)

if "%NEEDS_COMPILE%"=="1" (
    echo.
    echo  Compilando el proyecto...
    if not exist "%BIN%" mkdir "%BIN%"
    dir /b /s "%SRC%\*.java" > "%TEMP%\sources_career.txt"
    javac -encoding UTF-8 -cp "%LIB%" -d "%BIN%" @"%TEMP%\sources_career.txt"
    if errorlevel 1 (
        echo.
        echo  [ERROR] La compilacion ha fallado. Revisa los mensajes anteriores.
        pause
        exit /b 1
    )
    echo  Compilacion correcta.
)

:: ── 4. Ejecutar ───────────────────────────────────────────────
echo.
echo  Iniciando CareerAdvisor-MAS...
echo.
java -cp "%BIN%;%LIB%" %MAIN%
if errorlevel 1 ( echo. && echo  [ERROR] La aplicacion termino con error. && pause )
endlocal
exit /b 0

:NO_JAVA
echo.
echo  [ERROR] No se encontro 'java'. Instala el JDK desde https://adoptium.net/
pause & exit /b 1

:NO_JAVAC
echo.
echo  [ERROR] No se encontro 'javac'. Asegurate de instalar el JDK (no solo el JRE).
pause & exit /b 1

:NO_JADE
echo.
echo  [ERROR] No se encontro lib\jade.jar
echo  Descarga JADE desde https://jade.tilab.com/ y copialo en la carpeta lib\
pause & exit /b 1
