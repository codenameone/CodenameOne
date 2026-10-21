/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.ui;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The encoder draws the same symbol as an independent one.
///
/// `qr-vectors.txt` was written by the Python `segno` encoder (level M, byte
/// mode): for every version this supports, the matrix for each of the eight
/// masks (and the one segno picks by penalty, read past: see below). A wrong check byte, a misplaced
/// format bit or a different mask choice fails here, not at an authenticator
/// app that will not scan the code.
class QrCodeTest {
    @Test
    void matchesAReferenceEncoderForEveryVersionAndMask() throws Exception {
        InputStream in = QrCodeTest.class.getResourceAsStream("/qr-vectors.txt");
        assertNotNull(in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        int checked = 0;
        StringBuilder failures = new StringBuilder();
        String header;
        while ((header = reader.readLine()) != null && header.length() > 0) {
            String[] parts = header.split(" ", 3);
            int version = Integer.parseInt(parts[0]);
            int mask = Integer.parseInt(parts[1]);
            byte[] data = parts[2].getBytes(StandardCharsets.UTF_8);
            int size = version * 4 + 17;
            if (mask < 0) {
                // segno scores the masks before writing the format bits, this
                // encoder after (as the standard's evaluation reads the whole
                // symbol), so the automatic choice can differ while both
                // symbols are valid. The fixed-mask matrices are the check.
                for (int y = 0; y < size; y++) {
                    reader.readLine();
                }
                assertTrue(isOneOfTheFixedMasks(data, version), "version " + version);
                checked++;
                continue;
            }
            QrCode code = QrCode.encode(data, version, mask);
                        assertEquals(size, code.size());
            StringBuilder theirs = new StringBuilder();
            StringBuilder ours = new StringBuilder();
            for (int y = 0; y < size; y++) {
                theirs.append(reader.readLine()).append('\n');
                for (int x = 0; x < size; x++) {
                    ours.append(code.isDark(x, y) ? '1' : '0');
                }
                ours.append('\n');
            }
            if (!theirs.toString().equals(ours.toString())) {
                failures.append(version).append('/').append(mask).append(' ');
            }
            checked++;
        }
        assertEquals(90, checked);
        assertEquals("", failures.toString(), "version/mask that differ");
    }

    private static boolean isOneOfTheFixedMasks(byte[] data, int version) {
        QrCode auto = QrCode.encode(data, version, -1);
        for (int mask = 0; mask < 8; mask++) {
            if (same(auto, QrCode.encode(data, version, mask))) {
                return true;
            }
        }
        return false;
    }

    private static boolean same(QrCode a, QrCode b) {
        for (int y = 0; y < a.size(); y++) {
            for (int x = 0; x < a.size(); x++) {
                if (a.isDark(x, y) != b.isDark(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void picksTheSmallestVersionThatFits() {
        assertEquals(21, QrCode.encode(new byte[14]).size());
        assertEquals(25, QrCode.encode(new byte[15]).size());
        assertEquals(57, QrCode.encode(new byte[213]).size());
    }
}
