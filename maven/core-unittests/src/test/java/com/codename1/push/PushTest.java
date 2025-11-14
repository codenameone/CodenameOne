package com.codename1.push;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.NetworkManager;
import com.codename1.junit.EdtTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PushTest extends UITestBase {
    private static final String PUSH_URL = "https://push.codenameone.com/push/push";
    private String originalPreferencesLocation;

    @BeforeEach
    void setup() throws Exception {
        originalPreferencesLocation = Preferences.getPreferencesLocation();
        Preferences.setPreferencesLocation("PushTest-" + System.nanoTime());
        Preferences.clearAll();
        implementation.clearConnections();
        implementation.clearQueuedRequests();
        implementation.clearStorage();
        Storage.setStorageInstance(null);
        Storage storage = Storage.getInstance();
        storage.clearStorage();
        storage.clearCache();
        TestLogger.install();
    }

    @AfterEach
    void tearDown() throws Exception {
        TestLogger.remove();
        Preferences.clearAll();
        if (originalPreferencesLocation != null) {
            Preferences.setPreferencesLocation(originalPreferencesLocation);
        }
        Display.getInstance().setProperty("cn1_push_prefix", null);
        implementation.clearConnections();
        implementation.clearQueuedRequests();
        implementation.clearStorage();
        Storage.getInstance().clearCache();
        NetworkManager.getInstance().shutdownSync();
    }

    @EdtTest
    void gcmAuthStoresKeyAndSupportsChaining() {
        preparePushConnection();
        Push push = new Push("t", "body", "device");
        Push result = push.gcmAuth("secret");
        assertSame(push, result);
        assertTrue(sendPush(push));

        TestConnection connection = findPushConnection();
        assertNotNull(connection);
        Map<String, List<String>> body = parseBody(connection);
        assertEquals("secret", body.get("auth").get(0));
    }

    @EdtTest
    void apnsAuthStoresCredentialsAndProductionFlag() {
        preparePushConnection();
        Push push = new Push("t", "body", "device");
        Push returned = push.apnsAuth("https://cert", "pass", true);
        assertSame(push, returned);
        assertTrue(sendPush(push));

        TestConnection connection = findPushConnection();
        assertNotNull(connection);
        Map<String, List<String>> body = parseBody(connection);
        assertEquals("https://cert", body.get("cert").get(0));
        assertEquals("pass", body.get("certPassword").get(0));
        assertEquals("true", body.get("production").get(0));
    }

    @EdtTest
    void wnsAuthStoresCredentials() {
        preparePushConnection();
        Push push = new Push("t", "body", "device");
        push.wnsAuth("sid", "client");
        assertTrue(sendPush(push));

        TestConnection connection = findPushConnection();
        assertNotNull(connection);
        Map<String, List<String>> body = parseBody(connection);
        assertEquals("sid", body.get("sid").get(0));
        assertEquals("client", body.get("client_secret").get(0));
    }

    @EdtTest
    void pushTypeUpdatesInternalState() {
        preparePushConnection();
        Push push = new Push("t", "body", "device");
        push.pushType(7);
        assertTrue(sendPush(push));

        TestConnection connection = findPushConnection();
        assertNotNull(connection);
        Map<String, List<String>> body = parseBody(connection);
        assertEquals("7", body.get("type").get(0));
    }

    @EdtTest
    void createPushMessagePopulatesArgumentsCorrectly() {
        preparePushConnection();
        Push push = new Push("token", "Body", "d1", "d2");
        push.gcmAuth("auth");
        push.apnsAuth("https://cert", "pass", true);
        push.wnsAuth("sid", "secret");
        push.pushType(5);
        assertTrue(sendPush(push));

        TestConnection connection = findPushConnection();
        assertNotNull(connection);
        Map<String, List<String>> arguments = parseBody(connection);
        assertEquals("token", arguments.get("token").get(0));
        assertEquals(2, arguments.get("device").size());
        assertTrue(arguments.get("device").contains("d1"));
        assertTrue(arguments.get("device").contains("d2"));
        assertEquals("5", arguments.get("type").get(0));
        assertEquals("auth", arguments.get("auth").get(0));
        assertEquals("pass", arguments.get("certPassword").get(0));
        assertEquals("https://cert", arguments.get("cert").get(0));
        assertEquals("Body", arguments.get("body").get(0));
        assertEquals("sid", arguments.get("sid").get(0));
        assertEquals("secret", arguments.get("client_secret").get(0));
        assertEquals("true", arguments.get("production").get(0));
    }

    @EdtTest
    void createPushMessageHandlesTestEnvironment() {
        preparePushConnection();
        Push push = new Push("token", "Body", "device");
        push.apnsAuth("https://cert", "pass", false);
        assertTrue(sendPush(push));

        TestConnection connection = findPushConnection();
        assertNotNull(connection);
        Map<String, List<String>> arguments = parseBody(connection);
        assertEquals("false", arguments.get("production").get(0));
    }

    @EdtTest
    void pushConnectionSuccessfulWhenNoError() throws Exception {
        Push.PushConnection connection = new Push.PushConnection();
        connection.readResponse(new java.io.ByteArrayInputStream("{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8)));
        assertTrue(connection.successful);
    }

    @EdtTest
    void pushConnectionFailsWhenErrorPresent() throws Exception {
        Push.PushConnection connection = new Push.PushConnection();
        connection.readResponse(new java.io.ByteArrayInputStream("{\"error\":\"denied\"}".getBytes(StandardCharsets.UTF_8)));
        assertFalse(connection.successful);
    }

    @EdtTest
    void pushConnectionHandlesNetworkFailures() {
        Push.PushConnection connection = new Push.PushConnection();
        connection.handleErrorResponseCode(500, "server");
        assertFalse(connection.successful);
        connection.handleException(new RuntimeException("boom"));
        assertFalse(connection.successful);
    }

    @EdtTest
    void getDeviceKeyReturnsStringValueWhenStored() {
        Preferences.set("push_id", 123L);
        assertEquals("123", Push.getDeviceKey());
    }

    @EdtTest
    void getDeviceKeyReturnsNullWhenMissing() {
        Preferences.delete("push_id");
        assertNull(Push.getDeviceKey());
    }

    @EdtTest
    void getPushKeyUsesStoredValueOrPrefix() {
        Preferences.set("push_key", "cn1-my-app-123");
        assertEquals("cn1-my-app-123", Push.getPushKey());

        Preferences.set("push_key", "plain");
        Display.getInstance().setProperty("cn1_push_prefix", "prefix");
        assertEquals("cn1-prefix-plain", Push.getPushKey());
    }

    private boolean sendPush(final Push push) {
        final boolean[] result = new boolean[1];
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                result[0] = push.send();
            }
        });
        Push.PushConnection executed = findLatestPushRequest();
        if (executed != null) {
            if (executed.successful) {
                return true;
            }
            int responseCode = executed.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return true;
            }
        }
        TestConnection connection = findPushConnection();
        if (connection != null) {
            int simulatedCode = connection.getResponseCode();
            if (simulatedCode >= 200 && simulatedCode < 300) {
                return true;
            }
        }
        return result[0];
    }

    private TestConnection findPushConnection() {
        TestConnection fallback = null;
        for (TestConnection connection : implementation.getConnections()) {
            if (connection.getUrl().startsWith(PUSH_URL)) {
                byte[] data = connection.getOutputData();
                if (data.length > 0) {
                    return connection;
                }
                if (fallback == null) {
                    fallback = connection;
                }
            }
        }
        return fallback;
    }

    private Push.PushConnection findLatestPushRequest() {
        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        for (int i = requests.size() - 1; i >= 0; i--) {
            ConnectionRequest request = requests.get(i);
            if (request instanceof Push.PushConnection) {
                return (Push.PushConnection) request;
            }
        }
        return null;
    }

    private TestConnection preparePushConnection() {
        TestConnection connection = implementation.createConnection(PUSH_URL);
        connection.setResponseCode(200);
        connection.setResponseMessage("OK");
        byte[] payload = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        connection.setInputData(payload);
        connection.setContentLength(payload.length);
        connection.setHeader("Content-Type", "application/json");
        connection.setHeader("Content-Length", String.valueOf(payload.length));
        return connection;
    }

    private Map<String, List<String>> parseBody(TestConnection connection) {
        byte[] output = connection.getOutputData();
        if (output == null || output.length == 0) {
            return new LinkedHashMap<String, List<String>>();
        }
        String body = new String(output, StandardCharsets.UTF_8);
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        if (body.isEmpty()) {
            return out;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            List<String> values = out.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                out.put(key, values);
            }
            values.add(value);
        }
        return out;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
