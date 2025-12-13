package com.codename1.io;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.util.Callback;
import org.junit.jupiter.api.Assertions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class WebServiceProxyCallTest extends UITestBase {

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
}
