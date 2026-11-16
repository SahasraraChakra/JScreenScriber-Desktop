@echo off
rem One-step build without Maven: downloads FlatLaf, compiles for Java 17+
rem Produces dist\jscreenscriber.jar

setlocal
cd /d "%~dp0"

set FL=3.5.4

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist

if not exist lib mkdir lib
mkdir build
mkdir dist

if not exist lib\flatlaf-%FL%.jar (
    curl -L -o lib\flatlaf-%FL%.jar https://repo1.maven.org/maven2/com/formdev/flatlaf/%FL%/flatlaf-%FL%.jar
)

rem Generate Java source list
(for /r src\main\java %%f in (*.java) do @echo "%%f") > sources_raw.txt

rem Convert Windows backslashes to forward slashes
powershell -NoProfile -Command "(Get-Content sources_raw.txt) | ForEach-Object { $_.Replace('\','/') } | Set-Content -Encoding ascii sources.txt"

rem Compile Java sources
javac --release 17 -encoding UTF-8 -cp lib\flatlaf-%FL%.jar -d build @sources.txt

if errorlevel 1 (
    echo COMPILE FAILED
    exit /b 1
)

rem Copy resources
xcopy /s /y /q src\main\resources\* build\ >nul

rem Extract FlatLaf into application JAR
pushd build
jar xf ..\lib\flatlaf-%FL%.jar
del /q module-info.class META-INF\MANIFEST.MF META-INF\*.SF META-INF\*.RSA META-INF\*.DSA 2>nul
popd

rem Create manifest
(echo Main-Class: com.jscreenscriber.Main& echo Enable-Native-Access: ALL-UNNAMED) > manifest.txt

rem Build executable JAR
jar cfm dist\jscreenscriber.jar manifest.txt -C build .

echo.
echo ==========================================
echo BUILD SUCCESSFUL
echo ==========================================
echo Built: dist\jscreenscriber.jar
echo.
echo Run: java -jar dist\jscreenscriber.jar

endlocal
