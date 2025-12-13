package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AccessTokenTest {
    private TestCodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation(true);
        Util.setImplementation(implementation);
    }

    @Test
    void constructWithTokenAndExpires() {
        AccessToken token = new AccessToken("mytoken123", "3600");

        assertEquals("mytoken123", token.getToken());
        assertEquals("3600", token.getExpires());
        assertNotNull(token.getExpiryDate());
    }

    @Test
    void constructWithRefreshToken() {
        AccessToken token = new AccessToken("access", "7200", "refresh");

        assertEquals("access", token.getToken());
        assertEquals("7200", token.getExpires());
        assertEquals("refresh", token.getRefreshToken());
    }

    @Test
    void constructWithIdentityToken() {
        AccessToken token = new AccessToken("access", "1800", "refresh", "identity");

        assertEquals("access", token.getToken());
        assertEquals("1800", token.getExpires());
        assertEquals("refresh", token.getRefreshToken());
        assertEquals("identity", token.getIdentityToken());
    }

    @Test
    void defaultConstructor() {
        AccessToken token = new AccessToken();

        assertNull(token.getToken());
        assertNull(token.getExpires());
        assertNull(token.getRefreshToken());
        assertNull(token.getIdentityToken());
    }

    @Test
    void createWithExpiryDate() {
        Date expiryDate = new Date(System.currentTimeMillis() + 3600000);
        AccessToken token = AccessToken.createWithExpiryDate("token", expiryDate);

        assertEquals("token", token.getToken());
        assertEquals(expiryDate, token.getExpiryDate());
    }

    @Test
    void setAndGetRefreshToken() {
        AccessToken token = new AccessToken("access", "3600");
        token.setRefreshToken("newRefresh");

        assertEquals("newRefresh", token.getRefreshToken());
    }

    @Test
    void setAndGetIdentityToken() {
        AccessToken token = new AccessToken("access", "3600");
        token.setIdentityToken("idToken");

        assertEquals("idToken", token.getIdentityToken());
    }

    @Test
    void setAndGetExpiryDate() {
        AccessToken token = new AccessToken("access", null);
        Date date = new Date(System.currentTimeMillis() + 7200000);

        token.setExpiryDate(date);
        assertEquals(date, token.getExpiryDate());
    }

    @Test
    void isExpiredWithFutureDate() {
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        AccessToken token = AccessToken.createWithExpiryDate("token", futureDate);

        assertFalse(token.isExpired());
    }

    @Test
    void isExpiredWithPastDate() {
        Date pastDate = new Date(System.currentTimeMillis() - 3600000);
        AccessToken token = AccessToken.createWithExpiryDate("token", pastDate);

        assertTrue(token.isExpired());
    }

    @Test
    void isExpiredWithNullDate() {
        AccessToken token = new AccessToken("token", null);

        assertFalse(token.isExpired());
    }

    @Test
    void parseExpiresAsSeconds() {
        AccessToken token = new AccessToken("token", "3600");
        Date expiryDate = token.getExpiryDate();

        assertNotNull(expiryDate);
        assertTrue(expiryDate.getTime() > System.currentTimeMillis());
    }

    @Test
    void parseExpiresWithDecimal() {
        AccessToken token = new AccessToken("token", "3600.5");
        Date expiryDate = token.getExpiryDate();

        assertNotNull(expiryDate);
        long expectedTime = System.currentTimeMillis() + 3600L * 1000;
        assertTrue(Math.abs(expiryDate.getTime() - expectedTime) < 1000);
    }

    @Test
    void parseExpiresWithZero() {
        AccessToken token = new AccessToken("token", "0");

        assertNull(token.getExpiryDate());
    }

    @Test
    void parseExpiresWithInvalidString() {
        AccessToken token = new AccessToken("token", "invalid");

        assertNull(token.getExpiryDate());
    }

    @Test
    void getVersion() {
        AccessToken token = new AccessToken("token", "3600");

        assertEquals(4, token.getVersion());
    }

    @Test
    void getObjectId() {
        AccessToken token = new AccessToken("token", "3600");

        assertEquals("AccessToken", token.getObjectId());
    }

    @Test
    void externalizeAndInternalize() throws IOException {
        AccessToken original = new AccessToken("access123", "3600", "refresh456", "identity789");
        original.setExpiryDate(new Date(System.currentTimeMillis() + 7200000));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        original.externalize(dos);
        dos.close();

        AccessToken restored = new AccessToken();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(4, dis);
        dis.close();

        assertEquals(original.getToken(), restored.getToken());
        assertEquals(original.getExpires(), restored.getExpires());
        assertEquals(original.getRefreshToken(), restored.getRefreshToken());
        assertEquals(original.getIdentityToken(), restored.getIdentityToken());
        assertEquals(original.getExpiryDate(), restored.getExpiryDate());
    }

    @Test
    void externalizeWithNullValues() throws IOException {
        AccessToken original = new AccessToken();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        original.externalize(dos);
        dos.close();

        AccessToken restored = new AccessToken();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(4, dis);
        dis.close();

        assertNull(restored.getToken());
        assertNull(restored.getExpires());
        assertNull(restored.getRefreshToken());
        assertNull(restored.getIdentityToken());
    }

    @Test
    void internalizeVersion1() throws IOException {
        AccessToken original = new AccessToken("token", "3600");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        Util.writeUTF("token", dos);
        Util.writeUTF("3600", dos);
        dos.close();

        AccessToken restored = new AccessToken();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(1, dis);
        dis.close();

        assertEquals("token", restored.getToken());
        assertEquals("3600", restored.getExpires());
        assertNull(restored.getRefreshToken());
        assertNull(restored.getIdentityToken());
    }

    @Test
    void internalizeVersion2() throws IOException {
        AccessToken original = new AccessToken("token", "3600");
        Date expiryDate = new Date(System.currentTimeMillis() + 3600000);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        Util.writeUTF("token", dos);
        Util.writeUTF("3600", dos);
        Util.writeObject(expiryDate, dos);
        dos.close();

        AccessToken restored = new AccessToken();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(2, dis);
        dis.close();

        assertEquals("token", restored.getToken());
        assertEquals(expiryDate, restored.getExpiryDate());
        assertNull(restored.getRefreshToken());
    }

    @Test
    void internalizeVersion3() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        Util.writeUTF("token", dos);
        Util.writeUTF("3600", dos);
        Util.writeObject(new Date(), dos);
        Util.writeUTF("refresh", dos);
        dos.close();

        AccessToken restored = new AccessToken();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        restored.internalize(3, dis);
        dis.close();

        assertEquals("token", restored.getToken());
        assertEquals("refresh", restored.getRefreshToken());
        assertNull(restored.getIdentityToken());
    }

    @Test
    void equalsWithSameTokenAndExpiry() {
        Date expiryDate = new Date(System.currentTimeMillis() + 3600000);
        AccessToken token1 = AccessToken.createWithExpiryDate("token", expiryDate);
        AccessToken token2 = AccessToken.createWithExpiryDate("token", expiryDate);

        assertTrue(token1.equals(token2));
        assertEquals(token1.hashCode(), token2.hashCode());
    }

    @Test
    void equalsWithDifferentTokens() {
        Date expiryDate = new Date(System.currentTimeMillis() + 3600000);
        AccessToken token1 = AccessToken.createWithExpiryDate("token1", expiryDate);
        AccessToken token2 = AccessToken.createWithExpiryDate("token2", expiryDate);

        assertFalse(token1.equals(token2));
    }

    @Test
    void equalsWithDifferentExpiry() {
        AccessToken token1 = AccessToken.createWithExpiryDate("token", new Date(1000));
        AccessToken token2 = AccessToken.createWithExpiryDate("token", new Date(2000));

        assertFalse(token1.equals(token2));
    }

    @Test
    void equalsWithNull() {
        AccessToken token = new AccessToken("token", "3600");

        assertFalse(token.equals(null));
    }

    @Test
    void equalsWithDifferentClass() {
        AccessToken token = new AccessToken("token", "3600");

        assertFalse(token.equals("token"));
    }

    @Test
    void testToString() {
        Date expiryDate = new Date(System.currentTimeMillis() + 3600000);
        AccessToken token = AccessToken.createWithExpiryDate("mytoken", expiryDate);

        String result = token.toString();
        assertTrue(result.contains("mytoken"));
        assertTrue(result.contains("AccessToken"));
    }

    @Test
    void hashCodeConsistency() {
        AccessToken token = new AccessToken("token", "3600");
        int hash1 = token.hashCode();
        int hash2 = token.hashCode();

        assertEquals(hash1, hash2);
    }
}
