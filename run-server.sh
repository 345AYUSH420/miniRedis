#!/bin/bash

echo "Starting Redis Clone Server..."
echo ""
echo "Usage: $0 [port]"
echo "Default port: 6379"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

if [ -z "$1" ]; then
    PORT=6379
else
    PORT=$1
fi

echo "Starting server on port $PORT..."
mvn exec:java -Dexec.mainClass="com.redisclone.server.RedisServer" -Dexec.args="--port $PORT"
