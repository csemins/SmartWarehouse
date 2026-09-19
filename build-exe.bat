@echo off
setlocal
cd /d "%~dp0"

echo [1/3] Building fat jar and SmartWarehouse.exe ...
call mvnw.cmd -q -Pwindows-exe package -DskipTests
if errorlevel 1 goto :fail

set "JLINK=jlink"
if defined JAVA_HOME set "JLINK=%JAVA_HOME%\bin\jlink"

echo [2/3] Creating bundled Java runtime with jlink ...
if exist dist\SmartWarehouse rmdir /s /q dist\SmartWarehouse
mkdir dist\SmartWarehouse
"%JLINK%" --add-modules java.base,java.desktop,java.sql,java.net.http,java.logging,java.xml,java.naming,java.management,java.scripting,jdk.unsupported,jdk.crypto.ec,jdk.charsets,jdk.localedata,jdk.zipfs,jdk.jsobject,jdk.xml.dom ^
  --strip-debug --no-header-files --no-man-pages --output dist\SmartWarehouse\runtime
if errorlevel 1 goto :fail

echo [3/3] Copying the executable ...
copy /y target\windows\SmartWarehouse\SmartWarehouse.exe dist\SmartWarehouse\ >nul

echo.
echo Done:  dist\SmartWarehouse\SmartWarehouse.exe
echo (keep the runtime folder next to the exe; zip the whole SmartWarehouse folder to share it)
goto :eof

:fail
echo BUILD FAILED
exit /b 1
