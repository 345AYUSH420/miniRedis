package com.redisclone.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.List;

public class RespParserTest {
    
    @Test
    void testParseSimpleString() throws IOException {
        String input = "+OK\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.SIMPLE_STRING, result.getType());
        assertEquals("OK", result.getString());
    }
    
    @Test
    void testParseError() throws IOException {
        String input = "-ERR unknown command\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.ERROR, result.getType());
        assertEquals("ERR unknown command", result.getError());
    }
    
    @Test
    void testParseInteger() throws IOException {
        String input = ":42\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.INTEGER, result.getType());
        assertEquals(42L, result.getInteger());
    }
    
    @Test
    void testParseBulkString() throws IOException {
        String input = "$5\r\nhello\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.BULK_STRING, result.getType());
        assertEquals("hello", result.getString());
    }
    
    @Test
    void testParseNullBulkString() throws IOException {
        String input = "$-1\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.BULK_STRING, result.getType());
        assertNull(result.getValue());
    }
    
    @Test
    void testParseArray() throws IOException {
        String input = "*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.ARRAY, result.getType());
        
        List<RespParser.RespValue> array = result.getArray();
        assertEquals(3, array.size());
        assertEquals("foo", array.get(0).getString());
        assertEquals("bar", array.get(1).getString());
        assertEquals("baz", array.get(2).getString());
    }
    
    @Test
    void testParseNullArray() throws IOException {
        String input = "*-1\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.ARRAY, result.getType());
        assertNull(result.getValue());
    }
    
    @Test
    void testParseComplexArray() throws IOException {
        String input = "*2\r\n$3\r\nfoo\r\n:42\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.ARRAY, result.getType());
        
        List<RespParser.RespValue> array = result.getArray();
        assertEquals(2, array.size());
        assertEquals("foo", array.get(0).getString());
        assertEquals(42L, array.get(1).getInteger());
    }
    
    @Test
    void testParseEmptyBulkString() throws IOException {
        String input = "$0\r\n\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.BULK_STRING, result.getType());
        assertEquals("", result.getString());
    }
    
    @Test
    void testParseEmptyArray() throws IOException {
        String input = "*0\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        RespParser.RespValue result = parser.parse();
        assertEquals(RespParser.RespValue.Type.ARRAY, result.getType());
        
        List<RespParser.RespValue> array = result.getArray();
        assertEquals(0, array.size());
    }
    
    @Test
    void testParseInvalidInteger() throws IOException {
        String input = ":not_a_number\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        assertThrows(IOException.class, () -> parser.parse());
    }
    
    @Test
    void testParseUnexpectedEOF() throws IOException {
        String input = "+OK";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        assertThrows(EOFException.class, () -> parser.parse());
    }
    
    @Test
    void testParseUnknownType() throws IOException {
        String input = "?unknown\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes()));
        
        assertThrows(IOException.class, () -> parser.parse());
    }
}
