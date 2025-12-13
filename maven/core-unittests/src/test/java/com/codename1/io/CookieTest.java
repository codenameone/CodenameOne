package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CookieTest {
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
    void setAndGetName() {
        Cookie cookie = new Cookie();
        cookie.setName("sessionId");

        assertEquals("sessionId", cookie.getName());
    }

    @Test
    void setAndGetValue() {
        Cookie cookie = new Cookie();
        cookie.setValue("abc123");

        assertEquals("abc123", cookie.getValue());
    }

    @Test
    void setAndGetDomain() {
        Cookie cookie = new Cookie();
        cookie.setDomain("example.com");

        assertEquals("example.com", cookie.getDomain());
    }

    @Test
    void setAndGetPath() {
        Cookie cookie = new Cookie();

        assertEquals("/", cookie.getPath());

        cookie.setPath("/api");
        assertEquals("/api", cookie.getPath());
    }

    @Test
    void setAndGetSecure() {
        Cookie cookie = new Cookie();

        assertFalse(cookie.isSecure());

        cookie.setSecure(true);
        assertTrue(cookie.isSecure());
    }

    @Test
    void setAndGetHttpOnly() {
        Cookie cookie = new Cookie();

        assertFalse(cookie.isHttpOnly());

        cookie.setHttpOnly(true);
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    void setAndGetExpires() {
        Cookie cookie = new Cookie();
        long expiryTime = System.currentTimeMillis() + 3600000;

        cookie.setExpires(expiryTime);
        assertEquals(expiryTime, cookie.getExpires());
    }

    @Test
    void getVersion() {
        Cookie cookie = new Cookie();

        assertEquals(1, cookie.getVersion());
    }

    @Test
    void getObjectId() {
        Cookie cookie = new Cookie();

        assertEquals("Cookie", cookie.getObjectId());
    }

    @Test
    void externalizeAndInternalize() throws IOException {
        Cookie original = new Cookie();
        original.setName("testCookie");
        original.setValue("testValue");
        original.setDomain("example.com");
        original.setExpires(123456789L);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        original.externalize(dos);
        dos.close();

        Cookie restored = new Cookie();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(1, dis);
        dis.close();

        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getValue(), restored.getValue());
        assertEquals(original.getDomain(), restored.getDomain());
        assertEquals(original.getExpires(), restored.getExpires());
    }

    @Test
    void externalizeWithNullValue() throws IOException {
        Cookie original = new Cookie();
        original.setName("testCookie");
        original.setValue(null);
        original.setDomain("example.com");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        original.externalize(dos);
        dos.close();

        Cookie restored = new Cookie();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(1, dis);
        dis.close();

        assertEquals(original.getName(), restored.getName());
        assertNull(restored.getValue());
        assertEquals(original.getDomain(), restored.getDomain());
    }

    @Test
    void externalizeWithNullDomain() throws IOException {
        Cookie original = new Cookie();
        original.setName("testCookie");
        original.setValue("value");
        original.setDomain(null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        original.externalize(dos);
        dos.close();

        Cookie restored = new Cookie();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(1, dis);
        dis.close();

        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getValue(), restored.getValue());
        assertNull(restored.getDomain());
    }

    @Test
    void testToString() {
        Cookie cookie = new Cookie();
        cookie.setName("session");
        cookie.setValue("abc123");
        cookie.setDomain("example.com");
        cookie.setExpires(123456789L);
        cookie.setSecure(true);
        cookie.setPath("/api");

        String result = cookie.toString();

        assertTrue(result.contains("session"));
        assertTrue(result.contains("abc123"));
        assertTrue(result.contains("example.com"));
        assertTrue(result.contains("123456789"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("/api"));
    }

    @Test
    void autoStoredDefault() {
        assertTrue(Cookie.isAutoStored());
    }

    @Test
    void setAutoStored() {
        Cookie.setAutoStored(false);
        assertFalse(Cookie.isAutoStored());

        Cookie.setAutoStored(true);
        assertTrue(Cookie.isAutoStored());
    }

    @Test
    void clearCookiesFromStorage() {
        Cookie.setAutoStored(true);

        Storage.getInstance().writeObject(Cookie.STORAGE_NAME, "test data");
        assertTrue(Storage.getInstance().exists(Cookie.STORAGE_NAME));

        Cookie.clearCookiesFromStorage();

        assertFalse(Storage.getInstance().exists(Cookie.STORAGE_NAME));
    }

    @Test
    void clearCookiesWhenNoneExist() {
        Storage.getInstance().deleteStorageFile(Cookie.STORAGE_NAME);

        assertDoesNotThrow(() -> Cookie.clearCookiesFromStorage());
    }

    @Test
    void verifyStorageName() {
        assertEquals("Cookies", Cookie.STORAGE_NAME);
    }
}
