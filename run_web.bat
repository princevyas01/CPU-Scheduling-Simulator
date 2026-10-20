@echo off
cd /d "%~dp0frontend"
echo ===================================================
echo Starting CPU Scheduling Studio — React Web Frontend
echo ===================================================
if not exist node_modules (
    echo Installing npm dependencies...
    npm install
)
echo Launching React application at http://localhost:3000...
start http://localhost:3000
npm run dev
