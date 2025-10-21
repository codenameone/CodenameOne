package com.codename1.push;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.TestImplementationProvider;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PushTest {
    private String originalPreferencesLocation;
    private CodenameOneImplementation originalDisplayImpl;
    private CodenameOneImplementation originalUtilImpl;
    private Object originalStorageInstance;

    @BeforeEach
    void setup() throws Exception {
        Field locationField = Preferences.class.getDeclaredField("preferencesLocation");
        locationField.setAccessible(true);
        originalPreferencesLocation = (String) locationField.get(null);
        Preferences.setPreferencesLocation("PushTest-" + System.nanoTime());
        Field storageField = Display.class.getDeclaredField("localProperties");
        storageField.setAccessible(true);
        storageField.set(Display.getInstance(), null);

        Field implField = Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        originalDisplayImpl = (CodenameOneImplementation) implField.get(null);

        Field utilImplField = Util.class.getDeclaredField("implInstance");
        utilImplField.setAccessible(true);
        originalUtilImpl = (CodenameOneImplementation) utilImplField.get(null);

        Field storageInstanceField = Storage.class.getDeclaredField("INSTANCE");
        storageInstanceField.setAccessible(true);
        originalStorageInstance = storageInstanceField.get(null);

        CodenameOneImplementation implementation = TestImplementationProvider.installImplementation(true);
        implField.set(null, implementation);
    }

    @AfterEach
    void tearDown() {
        if (originalPreferencesLocation != null) {
            Preferences.setPreferencesLocation(originalPreferencesLocation);
        }
        try {
            Field implField = Display.class.getDeclaredField("impl");
            implField.setAccessible(true);
            implField.set(null, originalDisplayImpl);
            Field storageInstanceField = Storage.class.getDeclaredField("INSTANCE");
            storageInstanceField.setAccessible(true);
            storageInstanceField.set(null, originalStorageInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Util.setImplementation(originalUtilImpl);
    }

    @Test
    void gcmAuthStoresKeyAndSupportsChaining() throws Exception {
        Push push = new Push("t", "body", "device");
        Push result = push.gcmAuth("secret");
        assertSame(push, result);
        assertEquals("secret", getPrivateField(push, "googleAuthKey"));
    }

    @Test
    void apnsAuthStoresCredentialsAndProductionFlag() throws Exception {
        Push push = new Push("t", "body", "device");
        Push returned = push.apnsAuth("https://cert", "pass", true);
        assertSame(push, returned);
        assertEquals("https://cert", getPrivateField(push, "iosCertificateURL"));
        assertEquals("pass", getPrivateField(push, "iosCertificatePassword"));
        assertEquals(Boolean.TRUE, getPrivateField(push, "production"));
    }

    @Test
    void wnsAuthStoresCredentials() throws Exception {
        Push push = new Push("t", "body", "device");
        push.wnsAuth("sid", "client");
        assertEquals("sid", getPrivateField(push, "wnsSID"));
        assertEquals("client", getPrivateField(push, "wnsClientSecret"));
    }

    @Test
    void pushTypeUpdatesInternalState() throws Exception {
        Push push = new Push("t", "body");
        push.pushType(7);
        assertEquals(7, getPrivateField(push, "pushType"));
    }

    @Test
    void createPushMessagePopulatesArgumentsCorrectly() throws Exception {
        Push.PushConnection connection = invokeCreatePushMessage(
                "token", "Body", true, "auth", "https://cert", "pass",
                "bbUrl", "bbApp", "bbPass", "bbPort", "sid", "secret", 5,
                new String[]{"d1", "d2"}
        );

        assertEquals("https://push.codenameone.com/push/push", connection.getUrl());
        assertTrue(connection.isPost());
        assertTrue(connection.isFailSilently());

        LinkedHashMap arguments = getArguments(connection);
        assertEquals("token", arguments.get("token"));
        assertArrayEquals(new String[]{"d1", "d2"}, (String[]) arguments.get("device"));
        assertEquals("5", arguments.get("type"));
        assertEquals("auth", arguments.get("auth"));
        assertEquals("pass", arguments.get("certPassword"));
        assertEquals("https://cert", arguments.get("cert"));
        assertEquals("Body", arguments.get("body"));
        assertEquals("bbUrl", arguments.get("burl"));
        assertEquals("bbApp", arguments.get("bbAppId"));
        assertEquals("bbPass", arguments.get("bbPass"));
        assertEquals("bbPort", arguments.get("bbPort"));
        assertEquals("sid", arguments.get("sid"));
        assertEquals("secret", arguments.get("client_secret"));
        assertEquals("true", arguments.get("production"));
    }

    @Test
    void createPushMessageHandlesTestEnvironment() throws Exception {
        Push.PushConnection connection = invokeCreatePushMessage(
                "token", "Body", false, "auth", "https://cert", "pass",
                "", "", "", "", "", "", 1,
                new String[]{"device"}
        );

        LinkedHashMap arguments = getArguments(connection);
        assertEquals("false", arguments.get("production"));
    }

    @Test
    void pushConnectionSuccessfulWhenNoError() throws Exception {
        Push.PushConnection connection = new Push.PushConnection();
        readResponse(connection, "{\"result\":\"ok\"}");
        assertTrue(connection.successful);
    }

    @Test
    void pushConnectionFailsWhenErrorPresent() throws Exception {
        Push.PushConnection connection = new Push.PushConnection();
        readResponse(connection, "{\"error\":\"denied\"}");
        assertFalse(connection.successful);
    }

    @Test
    void pushConnectionHandlesNetworkFailures() {
        Push.PushConnection connection = new Push.PushConnection();
        connection.handleErrorResponseCode(500, "server");
        assertFalse(connection.successful);
        connection.handleException(new RuntimeException("boom"));
        assertFalse(connection.successful);
    }

    @Test
    void getDeviceKeyReturnsStringValueWhenStored() {
        Preferences.set("push_id", 123L);
        assertEquals("123", Push.getDeviceKey());
    }

    @Test
    void getDeviceKeyReturnsNullWhenMissing() {
        Preferences.delete("push_id");
        assertNull(Push.getDeviceKey());
    }

    @Test
    void getPushKeyUsesStoredValueOrPrefix() throws Exception {
        Preferences.set("push_key", "cn1-my-app-123");
        assertEquals("cn1-my-app-123", Push.getPushKey());

        Preferences.set("push_key", "plain");
        Field field = Display.class.getDeclaredField("localProperties");
        field.setAccessible(true);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("cn1_push_prefix", "prefix");
        field.set(Display.getInstance(), map);

        assertEquals("cn1-prefix-plain", Push.getPushKey());
    }

    @Test
    void getPushKeyReturnsNullWhenUnset() {
        Preferences.delete("push_key");
        assertNull(Push.getPushKey());
    }

    private Object getPrivateField(Object target, String name) throws Exception {
        Field f = Push.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private Push.PushConnection invokeCreatePushMessage(String token, String body, boolean production,
                                                         String googleAuthKey, String iosUrl, String iosPassword,
                                                         String bbUrl, String bbApp, String bbPass, String bbPort,
                                                         String sid, String secret, int type, String[] deviceKeys)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = Push.class.getDeclaredMethod("createPushMessage", String.class, String.class, boolean.class,
                String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, int.class, String[].class);
        m.setAccessible(true);
        return (Push.PushConnection) m.invoke(null, token, body, production, googleAuthKey, iosUrl, iosPassword,
                bbUrl, bbApp, bbPass, bbPort, sid, secret, type, deviceKeys);
    }

    private LinkedHashMap getArguments(ConnectionRequest request) throws Exception {
        Field f = ConnectionRequest.class.getDeclaredField("requestArguments");
        f.setAccessible(true);
        return (LinkedHashMap) f.get(request);
    }

    private void readResponse(Push.PushConnection connection, String json) throws Exception {
        InputStream stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        Method readResponse = Push.PushConnection.class.getDeclaredMethod("readResponse", InputStream.class);
        readResponse.setAccessible(true);
        readResponse.invoke(connection, stream);
    }
}
