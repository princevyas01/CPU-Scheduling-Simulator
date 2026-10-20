@echo off
setlocal
cd /d "%~dp0"

echo ======================================================================
echo  CPU Scheduling Studio: Complete Build, Test, and Packaging
echo ======================================================================

if not exist "target\classes" mkdir "target\classes"

echo [1/4] Compiling Java source files...
javac -encoding UTF-8 -d target\classes src\main\java\cpuscheduler\model\*.java src\main\java\cpuscheduler\scheduler\*.java src\main\java\cpuscheduler\scheduler\algorithms\*.java src\main\java\cpuscheduler\simulation\*.java src\main\java\cpuscheduler\experiment\*.java src\main\java\cpuscheduler\export\*.java src\main\java\cpuscheduler\ui\*.java src\main\java\cpuscheduler\Main.java src\test\java\cpuscheduler\*.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/4] Running automated test suite...
java -cp target\classes cpuscheduler.TestRunner
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Tests failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [3/4] Packaging executable JAR...
jar --create --file target\cpu-scheduling-studio.jar --main-class cpuscheduler.Main -C target\classes .
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] JAR packaging failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [4/4] Build complete! Executable created at: target\cpu-scheduling-studio.jar
echo Run application using: java -jar target\cpu-scheduling-studio.jar
echo Or double-click: run.bat

endlocal
