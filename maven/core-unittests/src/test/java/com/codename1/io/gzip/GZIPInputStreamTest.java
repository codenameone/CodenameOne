package com.codename1.io.gzip;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GZIPInputStreamTest {
    @Test
    void readsCompressedContentAndExposesHeaderMetadata() throws IOException, GZIPException {
        String text = "Codename One gzip test payload";
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(compressed);
        out.setName("payload.txt");
        out.setComment("unit-test");
        out.setOS(3);
        out.setModifiedTime(123456789L);
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.finish();
        long expectedCrc = out.getCRC();
        out.close();

        byte[] payload = compressed.toByteArray();
        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(payload));
        in.readHeader();

        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        byte[] buffer = new byte[64];
        int read;
        while ((read = in.read(buffer)) != -1) {
            uncompressed.write(buffer, 0, read);
        }

        assertEquals(text, new String(uncompressed.toByteArray(), StandardCharsets.UTF_8));
        assertEquals("payload.txt", in.getName());
        assertEquals("unit-test", in.getComment());
        assertEquals(3, in.getOS());
        assertEquals(0L, in.getModifiedtime(), "Codename One GZIPInputStream normalizes mtime to zero");
        assertEquals(expectedCrc, in.getCRC());
    }

    @Test
    void crcUnavailableBeforeStreamIsConsumed() throws IOException {
        String text = "partial";
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(compressed);
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.finish();
        out.close();

        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed.toByteArray()));

        assertThrows(GZIPException.class, in::getCRC);
    }

    @Test
    void readHeaderFailsOnInsufficientInput() throws IOException {
        byte[] truncated = new byte[]{0x1f, (byte) 0x8b, 0x08};
        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(truncated));

        assertThrows(IOException.class, in::readHeader);
    }
}
