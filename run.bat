@echo off
setlocal
cd /d "%~dp0"

echo ======================================================================
echo  CPU Scheduling Studio - BTech Mini Project
echo ======================================================================

if not exist "target\classes" mkdir "target\classes"

echo [1/2] Compiling CPU Scheduling Studio sources...
javac -encoding UTF-8 -d target\classes src\main\java\cpuscheduler\model\*.java src\main\java\cpuscheduler\scheduler\*.java src\main\java\cpuscheduler\scheduler\algorithms\*.java src\main\java\cpuscheduler\simulation\*.java src\main\java\cpuscheduler\experiment\*.java src\main\java\cpuscheduler\export\*.java src\main\java\cpuscheduler\ui\*.java src\main\java\cpuscheduler\Main.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Launching CPU Scheduling Studio GUI...
start javaw -cp target\classes cpuscheduler.Main

endlocal
