@echo off
setlocal enabledelayedexpansion

set "output_file=ssh_config.json"

echo Enter SSH connection details:
set /p "hostname=Hostname: "
set /p "username=Username: "
set /p "port=Port (default 22): "
set /p "password=Password: "

if "%port%"=="" set "port=22"

(
echo {
echo   "host": "%hostname%",
echo   "port": %port%,
echo   "user": "%username%",
echo   "pass": "%password%"
echo }
) > %output_file%

echo SSH configuration has been saved to %output_file%
echo.
echo Content of %output_file%:
type %output_file%

pause