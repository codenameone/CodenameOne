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
}
