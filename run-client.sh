#!/bin/bash

echo "Running Redis Clone Client Example..."
echo ""
echo "Usage: $0 [host] [port]"
echo "Default: localhost 6379"
echo ""

if [ -z "$1" ]; then
    HOST=localhost
else
    HOST=$1
fi

if [ -z "$2" ]; then
    PORT=6379
else
    PORT=$2
fi

echo "Connecting to $HOST:$PORT..."
mvn exec:java -Dexec.mainClass="com.redisclone.client.RedisClientExample" -Dexec.args="$HOST $PORT"
