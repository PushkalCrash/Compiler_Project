@echo off
echo ══════════════════════════════════════════════
echo   CompilerViz — Building...
echo ══════════════════════════════════════════════
if not exist out mkdir out
javac -d out src\compiler\*.java
if %errorlevel% neq 0 (
    echo.
    echo   BUILD FAILED!
    pause
    exit /b 1
)
echo.
echo   BUILD SUCCESSFUL!
echo   Run with: run.bat
echo ══════════════════════════════════════════════
