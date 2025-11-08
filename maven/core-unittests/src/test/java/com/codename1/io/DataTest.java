package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DataTest {
    private TestCodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation(true);
        Util.setImplementation(implementation);
        Storage.setStorageInstance(null);
    }

    @AfterEach
    void tearDown() {
        Storage.setStorageInstance(null);
        Util.setImplementation(null);
    }

    @Test
    void stringDataWithDefaultCharset() throws IOException {
        Data.StringData data = new Data.StringData("Hello");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);

        assertEquals("Hello", new String(baos.toByteArray(), "UTF-8"));
        assertEquals(5, data.getSize());
    }

    @Test
    void stringDataWithCustomCharset() throws IOException {
        Data.StringData data = new Data.StringData("Hello", "UTF-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);

        assertEquals("Hello", new String(baos.toByteArray(), "UTF-8"));
    }

    @Test
    void stringDataWithInvalidCharset() {
        assertThrows(RuntimeException.class, () -> new Data.StringData("Hello", "INVALID-CHARSET"));
    }

    @Test
    void stringDataSize() throws IOException {
        Data.StringData data = new Data.StringData("Test");

        assertEquals(4, data.getSize());
    }

    @Test
    void stringDataWithUnicodeCharacters() throws IOException {
        Data.StringData data = new Data.StringData("Hello \u4e2d\u6587");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);

        assertEquals("Hello \u4e2d\u6587", new String(baos.toByteArray(), "UTF-8"));
    }

    @Test
    void byteDataAppendTo() throws IOException {
        byte[] bytes = {1, 2, 3, 4, 5};
        Data.ByteData data = new Data.ByteData(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);

        assertArrayEquals(bytes, baos.toByteArray());
    }

    @Test
    void byteDataSize() throws IOException {
        byte[] bytes = {1, 2, 3, 4, 5};
        Data.ByteData data = new Data.ByteData(bytes);

        assertEquals(5, data.getSize());
    }

    @Test
    void byteDataWithEmptyArray() throws IOException {
        byte[] bytes = {};
        Data.ByteData data = new Data.ByteData(bytes);

        assertEquals(0, data.getSize());
    }

    @Test
    void byteDataMultipleAppends() throws IOException {
        byte[] bytes = {1, 2, 3};
        Data.ByteData data = new Data.ByteData(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);
        data.appendTo(baos);

        byte[] result = baos.toByteArray();
        assertEquals(6, result.length);
        assertEquals(1, result[0]);
        assertEquals(1, result[3]);
    }

    @Test
    void storageDataAppendTo() throws IOException {
        String key = "testData";
        byte[] content = {10, 20, 30, 40, 50};
        Storage.getInstance().writeObject(key, content);

        Data.StorageData data = new Data.StorageData(key);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);

        assertArrayEquals(content, baos.toByteArray());
    }

    @Test
    void storageDataSize() throws IOException {
        String key = "testSize";
        byte[] content = new byte[100];
        Storage.getInstance().writeObject(key, content);

        Data.StorageData data = new Data.StorageData(key);

        assertTrue(data.getSize() > 0);
    }

    @Test
    void fileDataAppendTo() throws IOException {
        String path = "/test/file.txt";
        byte[] content = {5, 10, 15, 20};

        OutputStream os = FileSystemStorage.getInstance().openOutputStream(path);
        os.write(content);
        os.close();

        File file = new File(path);
        Data.FileData data = new Data.FileData(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);

        assertArrayEquals(content, baos.toByteArray());
    }

    @Test
    void fileDataSize() throws IOException {
        String path = "/test/sizefile.txt";
        byte[] content = new byte[50];

        OutputStream os = FileSystemStorage.getInstance().openOutputStream(path);
        os.write(content);
        os.close();

        File file = new File(path);
        Data.FileData data = new Data.FileData(file);

        assertEquals(50, data.getSize());
    }

    @Test
    void stringDataEmptyString() throws IOException {
        Data.StringData data = new Data.StringData("");

        assertEquals(0, data.getSize());
    }

    @Test
    void stringDataLargeString() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("test");
        }
        String largeString = sb.toString();

        Data.StringData data = new Data.StringData(largeString);

        assertEquals(4000, data.getSize());
    }

    @Test
    void byteDataWithLargeArray() throws IOException {
        byte[] largeBytes = new byte[10000];
        for (int i = 0; i < largeBytes.length; i++) {
            largeBytes[i] = (byte) (i % 256);
        }

        Data.ByteData data = new Data.ByteData(largeBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        data.appendTo(baos);

        assertArrayEquals(largeBytes, baos.toByteArray());
        assertEquals(10000, data.getSize());
    }

    @Test
    void stringDataWithSpecialCharacters() throws IOException {
        String special = "Hello\nWorld\t\r\n";
        Data.StringData data = new Data.StringData(special);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        data.appendTo(baos);

        assertEquals(special, new String(baos.toByteArray(), "UTF-8"));
    }

    @Test
    void multipleDataImplementationsInSequence() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Data.StringData stringData = new Data.StringData("Hello");
        stringData.appendTo(baos);

        Data.ByteData byteData = new Data.ByteData(new byte[]{1, 2, 3});
        byteData.appendTo(baos);

        byte[] result = baos.toByteArray();
        assertTrue(result.length > 0);
    }
}
