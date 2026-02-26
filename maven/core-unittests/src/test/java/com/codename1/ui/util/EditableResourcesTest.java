package com.codename1.ui.util;

import com.codename1.ui.EncodedImage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codename1.junit.UITestBase;

public class EditableResourcesTest extends UITestBase {


    @Test
    public void testSettersRejectUnsupportedTypes() {
        EditableResources resources = new EditableResources();

        assertThrows(UnsupportedOperationException.class, () -> resources.setUi("form", new byte[]{1}));
        assertThrows(UnsupportedOperationException.class, () -> resources.setTimeline("timeline", null));
        assertThrows(UnsupportedOperationException.class, () -> resources.setIndexedImage("indexed", null));
        assertThrows(UnsupportedOperationException.class, () -> resources.setSVG("svg", null));
    }

    @Test
    public void testOpenRejectsGuiBuilderEntry() throws Exception {
        byte[] content = createSingleEntryResource(Resources.MAGIC_UI, "form", new byte[]{1, 2, 3});
        assertThrows(UnsupportedOperationException.class, () -> EditableResources.open(new ByteArrayInputStream(content)));
    }

    @Test
    public void testOpenRejectsTimelineEntry() throws Exception {
        byte[] content = createSingleEntryResource(Resources.MAGIC_TIMELINE, "timeline", new byte[]{0});
        assertThrows(UnsupportedOperationException.class, () -> EditableResources.open(new ByteArrayInputStream(content)));
    }

    @Test
    public void testOpenRejectsIndexedImageSubtype() throws Exception {
        byte[] content = createImageEntryResource("img", (byte) 0xF3, new byte[0]);
        assertThrows(UnsupportedOperationException.class, () -> EditableResources.open(new ByteArrayInputStream(content)));
    }

    @Test
    public void testOpenRejectsSvgSubtype() throws Exception {
        byte[] content = createImageEntryResource("img", (byte) 0xF5, new byte[0]);
        assertThrows(UnsupportedOperationException.class, () -> EditableResources.open(new ByteArrayInputStream(content)));
    }


    @Test
    public void testSetThemeStoresThemeResource() {
        EditableResources resources = new EditableResources();
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("bgColor", "ff0000");

        resources.setTheme("mainTheme", theme);

        assertEquals("ff0000", resources.getTheme("mainTheme").get("bgColor"));
    }

    @Test
    public void testEditableSaveRoundTripsSupportedTypesWithResourcesReader() throws Exception {
        EditableResources editable = new EditableResources();

        editable.setImage("img", EncodedImage.create(SINGLE_PIXEL_PNG));
        editable.setData("blob", new byte[]{4, 5, 6});

        Hashtable<String, Hashtable<String, String>> l10n = new Hashtable<String, Hashtable<String, String>>();
        Hashtable<String, String> en = new Hashtable<String, String>();
        en.put("hello", "Hello");
        Hashtable<String, String> fr = new Hashtable<String, String>();
        fr.put("hello", "Bonjour");
        l10n.put("en", en);
        l10n.put("fr", fr);
        editable.setL10N("messages", l10n);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        editable.save(out);

        Resources loaded = new Resources(new ByteArrayInputStream(out.toByteArray()), -1);

        assertNotNull(loaded.getImage("img"));
        assertArrayEquals(new byte[]{4, 5, 6}, readAll(loaded.getData("blob")));
        assertEquals("Hello", loaded.getL10N("messages", "en").get("hello"));
        assertEquals("Bonjour", loaded.getL10N("messages", "fr").get("hello"));
    }

    private static byte[] createSingleEntryResource(byte magic, String id, byte[] payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(2);
        dos.writeByte(Resources.MAGIC_HEADER);
        dos.writeUTF("header");
        dos.writeShort(0);
        dos.writeShort(1);
        dos.writeShort(12);
        dos.writeShort(0);

        dos.writeByte(magic);
        dos.writeUTF(id);
        dos.writeInt(payload.length);
        dos.write(payload);
        dos.flush();
        return baos.toByteArray();
    }

    private static byte[] createImageEntryResource(String id, byte imageSubtype, byte[] payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(2);
        dos.writeByte(Resources.MAGIC_HEADER);
        dos.writeUTF("header");
        dos.writeShort(0);
        dos.writeShort(1);
        dos.writeShort(12);
        dos.writeShort(0);

        dos.writeByte(Resources.MAGIC_IMAGE);
        dos.writeUTF(id);
        dos.writeByte(imageSubtype);
        dos.write(payload);
        dos.flush();
        return baos.toByteArray();
    }

    private static byte[] readAll(java.io.InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[32];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private static final byte[] SINGLE_PIXEL_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };
}
