@echo off
setlocal
cd /d "%~dp0"

set "JAVAFX_LIB=%~dp0javafx-sdk-27.0.1\lib"
if not exist "%JAVAFX_LIB%\javafx.web.jar" (
    echo JavaFX SDK not found: "%JAVAFX_LIB%"
    pause
    exit /b 1
)

if not exist "out" mkdir "out"

javac -encoding UTF-8 --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.web,javafx.swing -d "out" *.java
if errorlevel 1 (
    echo VanGo compilation failed.
    pause
    exit /b 1
)

java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.web,javafx.swing -cp "out" VanGoKiosk
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" pause
exit /b %EXIT_CODE%
