@echo off
echo Starting Redis Clone Server...
echo.
echo Usage: run-server.bat [port]
echo Default port: 6379
echo.
echo Press Ctrl+C to stop the server
echo.

if "%1"=="" (
    set PORT=6379
) else (
    set PORT=%1
)

echo Starting server on port %PORT%...
mvn exec:java -Dexec.mainClass="com.redisclone.server.RedisServer" -Dexec.args="--port %PORT%"
