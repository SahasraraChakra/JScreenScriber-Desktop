@echo off
cd /d "%~dp0"
java -version
java --enable-native-access=ALL-UNNAMED -jar dist\jscreenscriber.jar
pause
