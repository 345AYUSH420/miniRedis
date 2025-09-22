# Redis Clone in Java

A Redis-compatible key-value store implementation in Java, supporting the Redis Serialization Protocol (RESP) and core data structures.

## Features

### Core Data Structures
- **Strings**: Basic key-value storage with expiration support
- **Hashes**: Field-value mappings within a key
- **Lists**: Ordered collections with push/pop operations
- **Sets**: Unordered collections of unique elements
- **Sorted Sets**: Ordered collections with score-based ranking

### Redis Protocol Support
- **RESP2 Protocol**: Full Redis Serialization Protocol implementation
- **Command Parsing**: Supports all major Redis commands
- **Network Server**: TCP socket-based server with concurrent client handling

### Key Features
- **Thread-Safe**: Concurrent access using thread-safe data structures
- **Expiration**: TTL support with automatic cleanup
- **Pattern Matching**: Key pattern matching with wildcards
- **Error Handling**: Comprehensive error responses following Redis conventions
- **Persistence**: Automatic data persistence with RDB-like format
- **Concurrent Clients**: Multiple client connections with thread pool

## Supported Commands

### String Commands
- `SET key value [EX seconds]` - Set a string value with optional expiration
- `GET key` - Get a string value
- `EXISTS key [key ...]` - Check if keys exist
- `DEL key [key ...]` - Delete keys
- `KEYS pattern` - Find keys matching pattern
- `EXPIRE key seconds` - Set expiration on key
- `TTL key` - Get time to live for key

### Hash Commands
- `HSET key field value` - Set hash field
- `HGET key field` - Get hash field
- `HGETALL key` - Get all hash fields and values
- `HDEL key field [field ...]` - Delete hash fields

### List Commands
- `LPUSH key element [element ...]` - Push elements to left of list
- `RPUSH key element [element ...]` - Push elements to right of list
- `LPOP key` - Pop element from left of list
- `RPOP key` - Pop element from right of list
- `LRANGE key start stop` - Get range of elements from list

### Set Commands
- `SADD key member [member ...]` - Add members to set
- `SMEMBERS key` - Get all members of set
- `SISMEMBER key member` - Check if member exists in set
- `SREM key member [member ...]` - Remove members from set

### Sorted Set Commands
- `ZADD key score member` - Add member with score to sorted set
- `ZSCORE key member` - Get score of member
- `ZRANGE key start stop` - Get range of members by rank

### Server Commands
- `PING [message]` - Test connection
- `INFO [section]` - Get server information
- `FLUSHALL` - Remove all keys

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Run Server
```bash
# Windows
run-server.bat

# Unix/Linux/Mac
./run-server.sh

# Or directly with Maven
mvn exec:java -Dexec.mainClass="com.redisclone.server.RedisServer"
```

### Run with Custom Port
```bash
# Windows
run-server.bat 6380

# Unix/Linux/Mac
./run-server.sh 6380

# Or directly with Maven
mvn exec:java -Dexec.mainClass="com.redisclone.server.RedisServer" -Dexec.args="--port 6380"
```

### Run Example Client
```bash
# Windows
run-client.bat

# Unix/Linux/Mac
./run-client.sh

# Or directly with Maven
mvn exec:java -Dexec.mainClass="com.redisclone.client.RedisClientExample"
```

## Usage Examples

### Using Redis CLI
```bash
# Connect to the server
redis-cli -p 6379

# String operations
SET mykey "Hello World"
GET mykey
SET mykey "Hello" EX 10

# Hash operations
HSET user:1 name "John"
HSET user:1 age "30"
HGETALL user:1

# List operations
LPUSH mylist "world"
LPUSH mylist "hello"
LRANGE mylist 0 -1

# Set operations
SADD myset "member1"
SADD myset "member2"
SMEMBERS myset

# Sorted set operations
ZADD leaderboard 100 "player1"
ZADD leaderboard 200 "player2"
ZRANGE leaderboard 0 -1
```

### Using Java Client
```java
import java.net.Socket;
import java.io.*;

public class RedisClient {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 6379);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Send SET command
            out.println("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n");
            String response = in.readLine();
            System.out.println("SET response: " + response);
            
            // Send GET command
            out.println("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
            response = in.readLine();
            System.out.println("GET response: " + response);
            if (response.startsWith("$")) {
                String value = in.readLine();
                System.out.println("Value: " + value);
            }
        }
    }
}
```

## Architecture

### Core Components

1. **RedisStore**: Thread-safe in-memory data store with support for all Redis data types
2. **RespParser**: RESP protocol parser for incoming commands
3. **RespWriter**: RESP protocol writer for outgoing responses
4. **CommandProcessor**: Command interpreter and executor
5. **RedisServer**: TCP server with concurrent client handling

### Thread Safety
- Uses `ConcurrentHashMap` for main storage
- Uses `CopyOnWriteArrayList` for lists
- Uses `CopyOnWriteArraySet` for sets
- Uses `ConcurrentSkipListSet` for sorted sets
- Automatic cleanup of expired keys runs in background thread

### Memory Management
- Automatic cleanup of expired keys every 10 seconds
- Efficient memory usage with appropriate data structures
- No memory leaks with proper connection handling

## Limitations

- **No Replication**: Single-node implementation
- **No Clustering**: No distributed features
- **Limited Pattern Matching**: Basic wildcard support only
- **No Transactions**: No MULTI/EXEC support
- **No Pub/Sub**: No publish/subscribe functionality
- **Basic Persistence**: Simple RDB-like format, no AOF

## Future Enhancements

- [x] Persistence layer (RDB-like format)
- [ ] Transaction support (MULTI/EXEC)
- [ ] Pub/Sub functionality
- [ ] Advanced pattern matching
- [ ] Memory optimization
- [ ] Configuration file support
- [ ] Metrics and monitoring
- [ ] Replication support
- [ ] AOF (Append Only File) persistence
- [ ] Memory usage optimization

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Inspired by Redis (https://redis.io/)
- Built with Java 11 and Maven
- Uses JUnit 5 for testing
