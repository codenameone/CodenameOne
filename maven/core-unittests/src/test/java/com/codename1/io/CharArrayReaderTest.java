package com.codename1.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CharArrayReaderTest {

    @Test
    void constructWithBuffer() {
        char[] buf = {'h', 'e', 'l', 'l', 'o'};
        CharArrayReader reader = new CharArrayReader(buf);

        assertNotNull(reader);
    }

    @Test
    void constructWithOffsetAndLength() {
        char[] buf = {'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'};
        CharArrayReader reader = new CharArrayReader(buf, 6, 4);

        assertNotNull(reader);
    }

    @Test
    void constructWithInvalidOffset() {
        char[] buf = {'h', 'e', 'l', 'l', 'o'};

        assertThrows(IllegalArgumentException.class, () -> new CharArrayReader(buf, -1, 3));
    }

    @Test
    void constructWithOffsetGreaterThanLength() {
        char[] buf = {'h', 'e', 'l', 'l', 'o'};

        assertThrows(IllegalArgumentException.class, () -> new CharArrayReader(buf, 10, 3));
    }

    @Test
    void constructWithNegativeLength() {
        char[] buf = {'h', 'e', 'l', 'l', 'o'};

        assertThrows(IllegalArgumentException.class, () -> new CharArrayReader(buf, 0, -1));
    }

    @Test
    void readSingleCharacter() throws IOException {
        char[] buf = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(buf);

        assertEquals('a', reader.read());
        assertEquals('b', reader.read());
        assertEquals('c', reader.read());
        assertEquals(-1, reader.read());
    }

    @Test
    void readIntoBuffer() throws IOException {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[5];

        int count = reader.read(dest, 0, 5);

        assertEquals(5, count);
        assertArrayEquals(source, dest);
    }

    @Test
    void readPartialBuffer() throws IOException {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[10];

        int count = reader.read(dest, 2, 3);

        assertEquals(3, count);
        assertEquals('h', dest[2]);
        assertEquals('e', dest[3]);
        assertEquals('l', dest[4]);
    }

    @Test
    void readMoreThanAvailable() throws IOException {
        char[] source = {'a', 'b'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[5];

        int count = reader.read(dest, 0, 5);

        assertEquals(2, count);
        assertEquals('a', dest[0]);
        assertEquals('b', dest[1]);
    }

    @Test
    void readAfterEnd() throws IOException {
        char[] source = {'a'};
        CharArrayReader reader = new CharArrayReader(source);
        reader.read();

        assertEquals(-1, reader.read());
    }

    @Test
    void readWithInvalidOffset() {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[3];

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> reader.read(dest, -1, 2));
    }

    @Test
    void readWithOffsetOutOfBounds() {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[3];

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> reader.read(dest, 5, 2));
    }

    @Test
    void readWithNegativeLength() {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[3];

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> reader.read(dest, 0, -1));
    }

    @Test
    void readWithLengthExceedingBuffer() {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[3];

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> reader.read(dest, 2, 5));
    }

    @Test
    void markAndReset() throws IOException {
        char[] source = {'a', 'b', 'c', 'd', 'e'};
        CharArrayReader reader = new CharArrayReader(source);

        assertEquals('a', reader.read());
        reader.mark(100);
        assertEquals('b', reader.read());
        assertEquals('c', reader.read());
        reader.reset();
        assertEquals('b', reader.read());
    }

    @Test
    void markSupported() {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        assertTrue(reader.markSupported());
    }

    @Test
    void resetWithoutMark() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        reader.read();
        reader.read();
        reader.reset();

        assertEquals('a', reader.read());
    }

    @Test
    void markAfterClose() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        reader.close();

        assertThrows(IOException.class, () -> reader.mark(10));
    }

    @Test
    void resetAfterClose() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        reader.close();

        assertThrows(IOException.class, () -> reader.reset());
    }

    @Test
    void skipCharacters() throws IOException {
        char[] source = {'a', 'b', 'c', 'd', 'e'};
        CharArrayReader reader = new CharArrayReader(source);

        long skipped = reader.skip(2);

        assertEquals(2, skipped);
        assertEquals('c', reader.read());
    }

    @Test
    void skipMoreThanAvailable() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        long skipped = reader.skip(10);

        assertEquals(3, skipped);
        assertEquals(-1, reader.read());
    }

    @Test
    void skipNegativeAmount() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        long skipped = reader.skip(-5);

        assertEquals(0, skipped);
        assertEquals('a', reader.read());
    }

    @Test
    void skipZero() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        long skipped = reader.skip(0);

        assertEquals(0, skipped);
        assertEquals('a', reader.read());
    }

    @Test
    void skipAfterClose() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        reader.close();

        assertThrows(IOException.class, () -> reader.skip(1));
    }

    @Test
    void ready() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        assertTrue(reader.ready());
        reader.read();
        assertTrue(reader.ready());
        reader.read();
        reader.read();
        assertFalse(reader.ready());
    }

    @Test
    void readyAfterClose() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        reader.close();

        assertThrows(IOException.class, () -> reader.ready());
    }

    @Test
    void close() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        reader.close();

        assertThrows(IOException.class, () -> reader.read());
    }

    @Test
    void closeMultipleTimes() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);

        reader.close();
        reader.close();

        assertThrows(IOException.class, () -> reader.read());
    }

    @Test
    void readAfterClose() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        reader.close();

        assertThrows(IOException.class, () -> reader.read());
    }

    @Test
    void readBufferAfterClose() throws IOException {
        char[] source = {'a', 'b', 'c'};
        CharArrayReader reader = new CharArrayReader(source);
        char[] dest = new char[3];
        reader.close();

        assertThrows(IOException.class, () -> reader.read(dest, 0, 3));
    }

    @Test
    void constructWithOffsetAtEnd() {
        char[] buf = {'h', 'e', 'l', 'l', 'o'};
        CharArrayReader reader = new CharArrayReader(buf, 5, 0);

        assertNotNull(reader);
    }

    @Test
    void readWithOffsetAndLength() throws IOException {
        char[] source = "hello world".toCharArray();
        CharArrayReader reader = new CharArrayReader(source, 6, 5);

        char[] dest = new char[5];
        int count = reader.read(dest, 0, 5);

        assertEquals(5, count);
        assertEquals("world", new String(dest));
    }

    @Test
    void markWithOffsetAndLength() throws IOException {
        char[] source = "hello world".toCharArray();
        CharArrayReader reader = new CharArrayReader(source, 6, 5);

        assertEquals('w', reader.read());
        reader.mark(10);
        assertEquals('o', reader.read());
        reader.reset();
        assertEquals('o', reader.read());
    }

    @Test
    void readBeyondLength() throws IOException {
        char[] source = "hello world".toCharArray();
        CharArrayReader reader = new CharArrayReader(source, 0, 5);

        char[] dest = new char[10];
        int count = reader.read(dest, 0, 10);

        assertEquals(5, count);
        assertEquals("hello", new String(dest, 0, count));
    }
}
