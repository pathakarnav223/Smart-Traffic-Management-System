@echo off
REM Smart Traffic Management System - build and run (Windows). Needs JDK 17 or newer.
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac --release 17 -d out @sources.txt
if errorlevel 1 (
  echo Compilation failed.
  del sources.txt
  pause
  exit /b 1
)
del sources.txt
REM lib\*.jar (mysql-connector-j) is added to the classpath if present
java -cp "out;lib\*" traffic.Main
