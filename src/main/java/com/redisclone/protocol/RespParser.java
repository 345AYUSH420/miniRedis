package com.redisclone.protocol;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RespParser {
    
    public static class RespValue {
        private final Type type;
        private final Object value;
        
        public enum Type {
            SIMPLE_STRING, ERROR, INTEGER, BULK_STRING, ARRAY
        }
        
        public RespValue(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
        
        public Type getType() {
            return type;
        }
        
        public Object getValue() {
            return value;
        }
        
        @SuppressWarnings("unchecked")
        public List<RespValue> getArray() {
            if (type != Type.ARRAY) {
                throw new ClassCastException("Value is not an array");
            }
            return (List<RespValue>) value;
        }
        
        public String getString() {
            if (type == Type.SIMPLE_STRING || type == Type.BULK_STRING) {
                return (String) value;
            }
            throw new ClassCastException("Value is not a string");
        }
        
        public long getInteger() {
            if (type != Type.INTEGER) {
                throw new ClassCastException("Value is not an integer");
            }
            return (Long) value;
        }
        
        public String getError() {
            if (type != Type.ERROR) {
                throw new ClassCastException("Value is not an error");
            }
            return (String) value;
        }
        
        @Override
        public String toString() {
            return "RespValue{" + "type=" + type + ", value=" + value + '}';
        }
    }
    
    private final BufferedReader reader;
    
    public RespParser(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }
    
    public RespValue parse() throws IOException {
        int firstByte = reader.read();
        if (firstByte == -1) {
            throw new EOFException("Unexpected end of stream");
        }
        
        char type = (char) firstByte;
        
        switch (type) {
            case '+':
                return parseSimpleString();
            case '-':
                return parseError();
            case ':':
                return parseInteger();
            case '$':
                return parseBulkString();
            case '*':
                return parseArray();
            default:
                throw new IOException("Unknown RESP type: " + type);
        }
    }
    
    private RespValue parseSimpleString() throws IOException {
        String value = reader.readLine();
        if (value == null) {
            throw new EOFException("Unexpected end of stream");
        }
        return new RespValue(RespValue.Type.SIMPLE_STRING, value);
    }
    
    private RespValue parseError() throws IOException {
        String value = reader.readLine();
        if (value == null) {
            throw new EOFException("Unexpected end of stream");
        }
        return new RespValue(RespValue.Type.ERROR, value);
    }
    
    private RespValue parseInteger() throws IOException {
        String value = reader.readLine();
        if (value == null) {
            throw new EOFException("Unexpected end of stream");
        }
        try {
            return new RespValue(RespValue.Type.INTEGER, Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid integer: " + value);
        }
    }
    
    private RespValue parseBulkString() throws IOException {
        String lengthStr = reader.readLine();
        if (lengthStr == null) {
            throw new EOFException("Unexpected end of stream");
        }
        
        int length = Integer.parseInt(lengthStr);
        if (length == -1) {
            return new RespValue(RespValue.Type.BULK_STRING, null);
        }
        
        char[] buffer = new char[length];
        int bytesRead = reader.read(buffer, 0, length);
        if (bytesRead != length) {
            throw new EOFException("Unexpected end of stream");
        }
        reader.readLine();
        
        return new RespValue(RespValue.Type.BULK_STRING, new String(buffer));
    }
    
    private RespValue parseArray() throws IOException {
        String lengthStr = reader.readLine();
        if (lengthStr == null) {
            throw new EOFException("Unexpected end of stream");
        }
        
        int length = Integer.parseInt(lengthStr);
        if (length == -1) {
            return new RespValue(RespValue.Type.ARRAY, null);
        }
        
        List<RespValue> array = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            array.add(parse());
        }
        
        return new RespValue(RespValue.Type.ARRAY, array);
    }
    
    public void close() throws IOException {
        reader.close();
    }
}
