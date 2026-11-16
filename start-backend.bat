@echo off
chcp 65001 >nul
cd /d %~dp0marknote-server
echo start backend...
mvn spring-boot:run
pause
