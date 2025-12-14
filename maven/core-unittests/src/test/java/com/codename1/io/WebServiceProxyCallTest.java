package com.codename1.io;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WebServiceProxyCallTest extends UITestBase {

    @org.junit.jupiter.api.BeforeEach
    public void resetNetworkManager() {
        // Ensure NetworkManager is in a clean state
        try {
            java.lang.reflect.Field runningField = NetworkManager.class.getDeclaredField("running");
            runningField.setAccessible(true);
            runningField.setBoolean(NetworkManager.getInstance(), false);

            java.lang.reflect.Field threadsField = NetworkManager.class.getDeclaredField("networkThreads");
            threadsField.setAccessible(true);
            threadsField.set(NetworkManager.getInstance(), null);

            java.lang.reflect.Field pendingField = NetworkManager.class.getDeclaredField("pending");
            pendingField.setAccessible(true);
            ((java.util.Vector)pendingField.get(NetworkManager.getInstance())).clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void cleanup() {
        if (implementation != null) {
            implementation.clearNetworkMocks();
        }
    }

    @FormTest
    public void testWSConnectionVoid() throws Exception {
        WebServiceProxyCall.WSDefinition def = new WebServiceProxyCall.WSDefinition();
        def.url = "http://example.com/void";
        def.name = "voidMethod";
        def.returnType = WebServiceProxyCall.TYPE_VOID;
        def.arguments = new int[]{};

        WebServiceProxyCall.WSConnection conn = new WebServiceProxyCall.WSConnection(def, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Void returns nothing

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        conn.readResponse(bais);
    }

    @FormTest
    public void testWSConnectionInt() throws Exception {
        WebServiceProxyCall.WSDefinition def = new WebServiceProxyCall.WSDefinition();
        def.url = "http://example.com/int";
        def.name = "intMethod";
        def.returnType = WebServiceProxyCall.TYPE_INT;
        def.arguments = new int[]{WebServiceProxyCall.TYPE_INT};

        WebServiceProxyCall.WSConnection conn = new WebServiceProxyCall.WSConnection(def, null, 123);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(456);
        dos.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        conn.readResponse(bais);

        Assertions.assertEquals(456, conn.returnValue);

        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        conn.buildRequestBody(requestBody);

        ByteArrayInputStream requestInput = new ByteArrayInputStream(requestBody.toByteArray());
        java.io.DataInputStream dis = new java.io.DataInputStream(requestInput);
        Assertions.assertEquals("intMethod", dis.readUTF());
        Assertions.assertEquals(123, dis.readInt());
    }

    @FormTest
    public void testWSConnectionVariousTypes() throws Exception {
        WebServiceProxyCall.WSDefinition def = new WebServiceProxyCall.WSDefinition();
        def.url = "http://example.com/all";
        def.name = "allMethod";
        def.returnType = WebServiceProxyCall.TYPE_STRING;
        def.arguments = new int[]{
            WebServiceProxyCall.TYPE_BOOLEAN,
            WebServiceProxyCall.TYPE_STRING,
            WebServiceProxyCall.TYPE_STRING_ARRAY
        };

        WebServiceProxyCall.WSConnection conn = new WebServiceProxyCall.WSConnection(def, null, true, "hello", new String[]{"a", "b"});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeBoolean(true); // String presence
        dos.writeUTF("result");
        dos.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        conn.readResponse(bais);

        Assertions.assertEquals("result", conn.returnValue);

        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        conn.buildRequestBody(requestBody);

        ByteArrayInputStream requestInput = new ByteArrayInputStream(requestBody.toByteArray());
        java.io.DataInputStream dis = new java.io.DataInputStream(requestInput);
        Assertions.assertEquals("allMethod", dis.readUTF());
        Assertions.assertTrue(dis.readBoolean()); // boolean arg
        Assertions.assertTrue(dis.readBoolean()); // string arg present
        Assertions.assertEquals("hello", dis.readUTF());
        Assertions.assertEquals(2, dis.readInt()); // string array length
        Assertions.assertEquals("a", dis.readUTF());
        Assertions.assertEquals("b", dis.readUTF());
    }

    @FormTest
    public void testWSConnectionArrays() throws Exception {
         WebServiceProxyCall.WSDefinition def = new WebServiceProxyCall.WSDefinition();
        def.url = "http://example.com/arrays";
        def.name = "arraysMethod";
        def.returnType = WebServiceProxyCall.TYPE_INT_ARRAY;
        def.arguments = new int[]{};

        WebServiceProxyCall.WSConnection conn = new WebServiceProxyCall.WSConnection(def, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(2); // Array length
        dos.writeInt(10);
        dos.writeInt(20);
        dos.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        conn.readResponse(bais);

        int[] result = (int[]) conn.returnValue;
        Assertions.assertArrayEquals(new int[]{10, 20}, result);
    }

    @FormTest
    public void testWSConnectionOtherArrays() throws Exception {
        // Test byte array
        WebServiceProxyCall.WSDefinition def = new WebServiceProxyCall.WSDefinition();
        def.url = "http://example.com/bytearray";
        def.name = "byteMethod";
        def.returnType = WebServiceProxyCall.TYPE_BYTE_ARRAY;
        def.arguments = new int[]{WebServiceProxyCall.TYPE_BYTE_ARRAY};

        WebServiceProxyCall.WSConnection conn = new WebServiceProxyCall.WSConnection(def, null, new byte[]{1, 2});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(2);
        dos.writeByte(3);
        dos.writeByte(4);
        dos.flush();

        conn.readResponse(new ByteArrayInputStream(baos.toByteArray()));
        byte[] res = (byte[]) conn.returnValue;
        Assertions.assertArrayEquals(new byte[]{3, 4}, res);

        // Check request body
        ByteArrayOutputStream req = new ByteArrayOutputStream();
        conn.buildRequestBody(req);
        DataOutputStream dis = new DataOutputStream(new ByteArrayOutputStream());
        // We need to parse req
        java.io.DataInputStream in = new java.io.DataInputStream(new ByteArrayInputStream(req.toByteArray()));
        Assertions.assertEquals("byteMethod", in.readUTF());
        Assertions.assertEquals(2, in.readInt());
        Assertions.assertEquals(1, in.readByte());
        Assertions.assertEquals(2, in.readByte());
    }

    @FormTest
    public void testWSConnectionMissingTypes() throws Exception {
        WebServiceProxyCall.WSDefinition def = new WebServiceProxyCall.WSDefinition();
        def.url = "http://example.com/missing";
        def.name = "missingMethod";
        def.returnType = WebServiceProxyCall.TYPE_LONG; // Just pick one for return
        def.arguments = new int[]{
            WebServiceProxyCall.TYPE_BYTE,
            WebServiceProxyCall.TYPE_CHAR,
            WebServiceProxyCall.TYPE_SHORT,
            WebServiceProxyCall.TYPE_LONG,
            WebServiceProxyCall.TYPE_DOUBLE,
            WebServiceProxyCall.TYPE_FLOAT,
            WebServiceProxyCall.TYPE_BYTE_OBJECT,
            WebServiceProxyCall.TYPE_CHARACTER_OBJECT,
            WebServiceProxyCall.TYPE_SHORT_OBJECT,
            WebServiceProxyCall.TYPE_LONG_OBJECT,
            WebServiceProxyCall.TYPE_DOUBLE_OBJECT,
            WebServiceProxyCall.TYPE_FLOAT_OBJECT
        };

        WebServiceProxyCall.WSConnection conn = new WebServiceProxyCall.WSConnection(def, null,
            (byte)1, 'a', (short)2, 3L, 4.0, 5.0f,
            (Byte)(byte)6, (Character)'b', (Short)(short)7, 8L, 9.0, 10.0f
        );

        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        conn.buildRequestBody(requestBody);

        ByteArrayInputStream requestInput = new ByteArrayInputStream(requestBody.toByteArray());
        java.io.DataInputStream dis = new java.io.DataInputStream(requestInput);
        Assertions.assertEquals("missingMethod", dis.readUTF());

        // Byte
        Assertions.assertEquals((byte)1, dis.readByte());
        // Char
        Assertions.assertEquals('a', dis.readChar());
        // Short
        Assertions.assertEquals((short)2, dis.readShort());
        // Long
        Assertions.assertEquals(3L, dis.readLong());
        // Double
        Assertions.assertEquals(4.0, dis.readDouble(), 0.001);
        // Float
        Assertions.assertEquals(5.0f, dis.readFloat(), 0.001);

        // Byte Object
        Assertions.assertTrue(dis.readBoolean());
        Assertions.assertEquals((byte)6, dis.readByte());

        // Character Object
        Assertions.assertTrue(dis.readBoolean());
        Assertions.assertEquals('b', dis.readChar());

        // Short Object
        Assertions.assertTrue(dis.readBoolean());
        Assertions.assertEquals((short)7, dis.readShort());

        // Long Object
        Assertions.assertTrue(dis.readBoolean());
        Assertions.assertEquals(8L, dis.readLong());

        // Double Object
        Assertions.assertTrue(dis.readBoolean());
        Assertions.assertEquals(9.0, dis.readDouble(), 0.001);

        // Float Object
        Assertions.assertTrue(dis.readBoolean());
        Assertions.assertEquals(10.0f, dis.readFloat(), 0.001);
    }

    @FormTest
    public void testInvokeWebserviceSync() throws Exception {
        WebServiceProxyCall.WSDefinition def = WebServiceProxyCall.defineWebService(
                "http://example.com/apiSync", "myMethod", WebServiceProxyCall.TYPE_INT, WebServiceProxyCall.TYPE_INT);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(42);
        dos.flush();

        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(
                "http://example.com/apiSync", 200, "OK", baos.toByteArray());

        // invokeWebserviceSync uses NetworkManager.addToQueueAndWait
        // TestCodenameOneImplementation handles this if using UITestBase setup?
        // UITestBase sets up implementation.
        // We need to ensure NetworkManager uses the correct instance.
        // But TestCodenameOneImplementation.connect() handles the mock response.

        Object result = WebServiceProxyCall.invokeWebserviceSync(def, 10);
        Assertions.assertEquals(42, result);
    }

    @FormTest
    public void testInvokeWebserviceASync() throws Exception {
        WebServiceProxyCall.WSDefinition def = WebServiceProxyCall.defineWebService(
                "http://example.com/apiAsync", "myAsyncMethod", WebServiceProxyCall.TYPE_STRING, WebServiceProxyCall.TYPE_BOOLEAN);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeBoolean(true); // present
        dos.writeUTF("Success");
        dos.flush();

        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(
                "http://example.com/apiAsync", 200, "OK", baos.toByteArray());

        final AtomicReference<Object> resultRef = new AtomicReference<>();
        final AtomicBoolean successCalled = new AtomicBoolean(false);
        final AtomicBoolean failureCalled = new AtomicBoolean(false);

        WebServiceProxyCall.invokeWebserviceASync(def,
            res -> {
                resultRef.set(res);
                successCalled.set(true);
            },
            (req, err, code, msg) -> {
                failureCalled.set(true);
            },
            true
        );

        // Wait for async
        com.codename1.ui.Display.getInstance().invokeAndBlock(() -> {
            long start = System.currentTimeMillis();
            while (!successCalled.get() && !failureCalled.get() && System.currentTimeMillis() - start < 2000) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Assertions.assertTrue(successCalled.get(), "Success callback should be called");
        Assertions.assertEquals("Success", resultRef.get());
    }
}
