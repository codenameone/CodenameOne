package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BufferedOutputStreamTest {
    private TestCodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation(true);
        Util.setImplementation(implementation);
    }

    @Test
    void constructWithDefaultBufferSize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        assertNotNull(bos);
    }

    @Test
    void constructWithCustomBufferSize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 4096);

        assertNotNull(bos);
    }

    @Test
    void constructWithName() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, "testStream");

        assertEquals("testStream", bos.getName());
    }

    @Test
    void constructWithInvalidBufferSize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () -> new BufferedOutputStream(baos, 0));
        assertThrows(IllegalArgumentException.class, () -> new BufferedOutputStream(baos, -1));
    }

    @Test
    void writeSingleByte() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        bos.write(65);
        bos.flush();

        assertEquals(65, baos.toByteArray()[0]);
    }

    @Test
    void writeMultipleBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        bos.write(1);
        bos.write(2);
        bos.write(3);
        bos.flush();

        assertArrayEquals(new byte[]{1, 2, 3}, baos.toByteArray());
    }

    @Test
    void writeByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        byte[] data = {1, 2, 3, 4, 5};

        bos.write(data);
        bos.flush();

        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    void writeByteArrayWithOffsetAndLength() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        byte[] data = {1, 2, 3, 4, 5};

        bos.write(data, 1, 3);
        bos.flush();

        assertArrayEquals(new byte[]{2, 3, 4}, baos.toByteArray());
    }

    @Test
    void flushBuffer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 10);

        bos.write(1);
        bos.write(2);
        assertEquals(0, baos.toByteArray().length);

        bos.flush();
        assertEquals(2, baos.toByteArray().length);
    }

    @Test
    void automaticFlushOnFullBuffer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 4);

        bos.write(1);
        bos.write(2);
        bos.write(3);
        bos.write(4);

        // Buffer is full but not yet flushed
        assertEquals(0, baos.toByteArray().length);

        // Writing 5th byte triggers flush of the first 4 bytes
        bos.write(5);

        assertEquals(4, baos.toByteArray().length);

        bos.flush();

        assertEquals(5, baos.toByteArray().length);
    }

    @Test
    void writeLargeArrayBypassesBuffer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 10);
        byte[] largeData = new byte[100];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) i;
        }

        bos.write(largeData, 0, largeData.length);

        assertArrayEquals(largeData, baos.toByteArray());
    }

    @Test
    void close() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        bos.write(1);
        bos.close();

        assertEquals(1, baos.toByteArray().length);
    }

    @Test
    void closeMultipleTimes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        bos.close();
        bos.close();
    }

    @Test
    void flushAfterClose() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        bos.write(1);
        bos.close();
        bos.flush();
    }

    @Test
    void getTotalBytesWritten() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        bos.write(1);
        bos.write(2);
        bos.write(3);

        assertEquals(3, bos.getTotalBytesWritten());
    }

    @Test
    void getTotalBytesWrittenWithArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        byte[] data = {1, 2, 3, 4, 5};

        bos.write(data);

        assertEquals(5, bos.getTotalBytesWritten());
    }

    @Test
    void getLastActivityTime() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        long before = System.currentTimeMillis();
        bos.write(1);
        long after = System.currentTimeMillis();
        long lastActivity = bos.getLastActivityTime();

        assertTrue(lastActivity >= before && lastActivity <= after);
    }

    @Test
    void setAndGetConnection() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        Object connection = new Object();

        bos.setConnection(connection);

        assertSame(connection, bos.getConnection());
    }

    @Test
    void setAndGetProgressListener() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        AtomicInteger updateCount = new AtomicInteger();

        bos.setProgressListener((stream, bytes) -> updateCount.incrementAndGet());

        bos.write(1);
        bos.write(2);

        assertTrue(updateCount.get() >= 2);
    }

    @Test
    void progressListenerTracksBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        AtomicInteger totalBytes = new AtomicInteger();

        bos.setProgressListener((stream, bytes) -> totalBytes.set(bytes));

        bos.write(new byte[]{1, 2, 3, 4, 5});

        assertEquals(5, totalBytes.get());
    }

    @Test
    void defaultBufferSize() {
        int original = BufferedOutputStream.getDefaultBufferSize();
        BufferedOutputStream.setDefaultBufferSize(16384);

        assertEquals(16384, BufferedOutputStream.getDefaultBufferSize());

        BufferedOutputStream.setDefaultBufferSize(original);
    }

    @Test
    void writeLargeData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 1024);
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        bos.write(largeData);
        bos.flush();

        assertArrayEquals(largeData, baos.toByteArray());
    }

    @Test
    void multipleFlushes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 10);

        bos.write(1);
        bos.flush();
        assertEquals(1, baos.toByteArray().length);

        bos.write(2);
        bos.flush();
        assertEquals(2, baos.toByteArray().length);

        bos.write(3);
        bos.flush();
        assertEquals(3, baos.toByteArray().length);
    }

    @Test
    void writeArrayExceedingBuffer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 5);
        byte[] data1 = {1, 2};
        byte[] data2 = {3, 4, 5, 6};

        bos.write(data1, 0, data1.length);
        bos.write(data2, 0, data2.length);
        bos.flush();

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6}, baos.toByteArray());
    }

    @Test
    void flushBufferMethod() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 10);

        bos.write(1);
        bos.write(2);
        bos.flushBuffer();

        assertArrayEquals(new byte[]{1, 2}, baos.toByteArray());
    }

    @Test
    void closeFlushesBuffer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos, 10);

        bos.write(new byte[]{1, 2, 3});
        bos.close();

        assertArrayEquals(new byte[]{1, 2, 3}, baos.toByteArray());
    }
}
