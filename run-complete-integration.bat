@echo off
echo Running Complete Integration Demo...
echo.

cd /d "%~dp0"

REM Build the classpath
set "CLASSPATH=target\classes"
for %%i in (target\dependency\*.jar) do call :append_classpath "%%i"
goto :run

:append_classpath
set "CLASSPATH=%CLASSPATH%;%~1"
goto :eof

:run
java -cp "%CLASSPATH%" com.example.kafka.CompleteIntegrationDemo
