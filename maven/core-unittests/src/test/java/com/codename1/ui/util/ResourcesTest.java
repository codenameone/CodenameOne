package com.codename1.ui.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResourcesTest {

    @AfterEach
    public void resetPassword() {
        Resources.setPassword(null);
    }

    @Test
    public void testOpenFileParsesHeaderDataUiAndL10N() throws Exception {
        byte[] resourceBytes = createStandardResourceStream();
        Resources resources = new Resources(new ByteArrayInputStream(resourceBytes), 160);

        assertEquals(2, resources.getMajorVersion());
        assertEquals(5, resources.getMinorVersion());
        assertArrayEquals(new String[]{"metaOne", "metaTwo"}, resources.getMetaData());

        assertEquals(Resources.MAGIC_DATA, resources.getResourceType("binaryData"));
        assertEquals(Resources.MAGIC_UI, resources.getResourceType("uiDescriptor"));
        assertEquals(Resources.MAGIC_L10N, resources.getResourceType("translations"));

        assertArrayEquals(new String[]{"binaryData"}, resources.getDataResourceNames());
        assertArrayEquals(new String[]{"uiDescriptor"}, resources.getUIResourceNames());
        assertArrayEquals(new String[]{"translations"}, resources.getL10NResourceNames());

        List<String> resourceNames = Arrays.asList(resources.getResourceNames());
        assertTrue(resourceNames.contains("binaryData"));
        assertTrue(resourceNames.contains("uiDescriptor"));
        assertTrue(resourceNames.contains("translations"));

        assertInputStreamContentEquals(new byte[]{10, 20, 30}, resources.getData("binaryData"));
        assertInputStreamContentEquals(new byte[]{1, 2, 3, 4}, resources.getUi("uiDescriptor"));

        Hashtable<String, String> en = resources.getL10N("translations", "en");
        assertEquals("Hello", en.get("greeting"));
        assertEquals("Bye", en.get("farewell"));

        Hashtable<String, String> es = resources.getL10N("translations", "es");
        assertEquals("Hola", es.get("greeting"));
        assertEquals("Adios", es.get("farewell"));

        Enumeration localeEnum = resources.listL10NLocales("translations");
        Collection<String> locales = new ArrayList<String>();
        while (localeEnum.hasMoreElements()) {
            locales.add((String) localeEnum.nextElement());
        }
        assertTrue(locales.contains("en"));
        assertTrue(locales.contains("es"));

        Collection<String> localeSet = resources.l10NLocaleSet("translations");
        assertTrue(localeSet.contains("en"));
        assertTrue(localeSet.contains("es"));
    }

    @Test
    public void testPasswordProtectedResourceRequiresPassword() throws Exception {
        byte[] resourceBytes = createPasswordProtectedResourceStream("abcd", "protectedData", new byte[]{9, 8});
        assertThrows(IllegalStateException.class, () -> new Resources(new ByteArrayInputStream(resourceBytes), -1));
    }

    @Test
    public void testPasswordProtectedResourceDecodesEntries() throws Exception {
        byte[] resourceBytes = createPasswordProtectedResourceStream("abcd", "protectedData", new byte[]{9, 8});
        Resources.setPassword("abcd");
        Resources resources = new Resources(new ByteArrayInputStream(resourceBytes), -1);

        assertArrayEquals(new String[]{"protectedData"}, resources.getDataResourceNames());
        assertEquals(Resources.MAGIC_DATA, resources.getResourceType("protectedData"));
        assertInputStreamContentEquals(new byte[]{9, 8}, resources.getData("protectedData"));
    }

    @Test
    public void testInvalidResourceCountThrows() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(-1);
        dos.flush();

        byte[] resourceBytes = baos.toByteArray();
        assertThrows(IOException.class, () -> new Resources(new ByteArrayInputStream(resourceBytes), -1));
    }

    private static byte[] createStandardResourceStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(4);

        dos.writeByte(Resources.MAGIC_HEADER);
        dos.writeUTF("headerEntry");
        dos.writeShort(0);
        dos.writeShort(2);
        dos.writeShort(5);
        dos.writeShort(2);
        dos.writeUTF("metaOne");
        dos.writeUTF("metaTwo");

        dos.writeByte(Resources.MAGIC_DATA);
        dos.writeUTF("binaryData");
        dos.writeInt(3);
        dos.write(new byte[]{10, 20, 30});

        dos.writeByte(Resources.MAGIC_UI);
        dos.writeUTF("uiDescriptor");
        dos.writeInt(4);
        dos.write(new byte[]{1, 2, 3, 4});

        dos.writeByte(Resources.MAGIC_L10N);
        dos.writeUTF("translations");
        dos.writeShort(2);
        dos.writeShort(2);
        dos.writeUTF("greeting");
        dos.writeUTF("farewell");
        dos.writeUTF("en");
        dos.writeUTF("Hello");
        dos.writeUTF("Bye");
        dos.writeUTF("es");
        dos.writeUTF("Hola");
        dos.writeUTF("Adios");

        dos.flush();
        return baos.toByteArray();
    }

    private static byte[] createPasswordProtectedResourceStream(String password, String id, byte[] payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(2);

        dos.writeByte(Resources.MAGIC_PASSWORD);
        byte[] key = password.getBytes(StandardCharsets.UTF_8);
        char[] encodedPassword = new char[]{(char) ('l' ^ key[0]), (char) ('w' ^ key[1])};
        dos.writeUTF(new String(encodedPassword));

        KeyEncoder encoder = new KeyEncoder(key, 2);
        dos.writeByte(encoder.encodeByte(Resources.MAGIC_DATA & 0xff));
        dos.writeUTF(encoder.encodeString(id));
        dos.writeInt(payload.length);
        dos.write(payload);

        dos.flush();
        return baos.toByteArray();
    }

    private static void assertInputStreamContentEquals(byte[] expected, InputStream stream) throws IOException {
        assertNotNull(stream);
        try (InputStream in = stream) {
            assertArrayEquals(expected, readAll(in));
        }
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[32];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private static final class KeyEncoder {
        private final byte[] key;
        private int offset;

        private KeyEncoder(byte[] key, int initialOffset) {
            this.key = key;
            this.offset = initialOffset % key.length;
        }

        private byte encodeByte(int value) {
            int encoded = value ^ (key[offset] & 0xff);
            advance();
            return (byte) encoded;
        }

        private String encodeString(String value) {
            char[] chars = value.toCharArray();
            char[] encoded = new char[chars.length];
            for (int i = 0; i < chars.length; i++) {
                encoded[i] = (char) (chars[i] ^ (key[offset] & 0xff));
                advance();
            }
            return new String(encoded);
        }

        private void advance() {
            offset++;
            if (offset == key.length) {
                offset = 0;
            }
        }
    }
}
