@echo off
echo Running Interactive Redis Client...
echo.
echo Usage: run-interactive-client.bat [host] [port]
echo Default: localhost 6379
echo.

if "%1"=="" (
    set HOST=localhost
) else (
    set HOST=%1
)

if "%2"=="" (
    set PORT=6379
) else (
    set PORT=%2
)

echo Connecting to %HOST%:%PORT%...
mvn exec:java -Dexec.mainClass="com.redisclone.client.InteractiveRedisClient" -Dexec.args="%HOST% %PORT%"
