@echo off
setlocal enabledelayedexpansion

echo Building frontend...

REM Navigate to frontend directory
cd src\main\resources\frontend

REM Detect package manager
where pnpm >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set PKG_MGR=pnpm
    goto :found_pkg_mgr
)

where npm >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set PKG_MGR=npm
    goto :found_pkg_mgr
)

echo Error: Neither pnpm nor npm found. Please install Node.js and a package manager.
exit /b 1

:found_pkg_mgr
echo Using package manager: %PKG_MGR%

REM Install dependencies if node_modules doesn't exist
if not exist "node_modules" (
    echo Installing frontend dependencies...
    call %PKG_MGR% install
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to install dependencies
        exit /b 1
    )
)

REM Build the frontend
echo Running frontend build...
call %PKG_MGR% run build
if %ERRORLEVEL% NEQ 0 (
    echo Build failed
    exit /b 1
)

REM Create target static directory if it doesn't exist
if not exist "..\static" mkdir ..\static

REM Remove old static files
echo Cleaning old static files...
if exist "..\static\*" del /q ..\static\* >nul 2>nul
for /d %%p in (..\static\*) do rmdir "%%p" /s /q

REM Copy built files to Spring Boot static resources
echo Copying built files to Spring Boot static resources...
xcopy /E /I /Y dist\* ..\static\ >nul
if %ERRORLEVEL% NEQ 0 (
    echo Failed to copy files
    exit /b 1
)

echo.
echo Frontend build complete! Files copied to src\main\resources\static\

cd ..\..\..\..
