@echo off
cd /d "%~dp0CPUSchedulingSimulator"
echo Starting CPU Scheduling Studio...
javac -encoding UTF-8 Main.java
start javaw Main
