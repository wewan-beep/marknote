@echo off
chcp 65001 >nul
cd /d %~dp0marknote-web
if not exist node_modules call npm install
echo start frontend http://localhost:3000
call npm run dev
pause
