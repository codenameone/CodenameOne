package com.codename1.io.tar;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TarStreamTest extends UITestBase {

    @FormTest
    void testTarEntryWriteAndReadRoundTrip() throws Exception {
        byte[] payload = "hello tar".getBytes("UTF-8");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TarOutputStream tarOut = new TarOutputStream(out);

        TarEntry entry = new TarEntry(new byte[TarConstants.HEADER_BLOCK]);
        entry.setName("sample.txt");
        entry.setSize(payload.length);
        entry.getHeader().linkFlag = TarHeader.LF_NORMAL;

        tarOut.putNextEntry(entry);
        tarOut.write(payload);
        tarOut.close();

        TarInputStream tarIn = new TarInputStream(new ByteArrayInputStream(out.toByteArray()));
        TarEntry read = tarIn.getNextEntry();
        assertNotNull(read);
        assertEquals("sample.txt", read.getName().trim());

        byte[] buffer = new byte[(int) read.getSize()];
        int len = tarIn.read(buffer, 0, buffer.length);
        assertEquals(buffer.length, len);
        assertEquals("hello tar", new String(buffer, "UTF-8"));
        tarIn.close();
    }

    @FormTest
    void testTarHeaderSerializationWithOctalHelpers() {
        TarEntry folder = new TarEntry(new byte[TarConstants.HEADER_BLOCK]);
        folder.setName("folder/");
        folder.setSize(0);
        folder.getHeader().linkFlag = TarHeader.LF_DIR;
        assertTrue(folder.isDirectory());

        byte[] header = new byte[TarConstants.HEADER_BLOCK];
        folder.writeEntryHeader(header);

        TarEntry parsed = new TarEntry(header);
        assertEquals(folder.getName(), parsed.getName());
        assertEquals(folder.getHeader().linkFlag, parsed.getHeader().linkFlag);

        byte[] octal = new byte[8];
        Octal.getOctalBytes(493, octal, 0, octal.length);
        long parsedValue = Octal.parseOctal(octal, 0, octal.length);
        assertEquals(493, parsedValue);
    }
}
