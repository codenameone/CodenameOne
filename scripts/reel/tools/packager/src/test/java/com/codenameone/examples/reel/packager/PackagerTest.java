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
package com.codenameone.examples.reel.packager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PackagerTest {
    private static final String KID = "00112233445566778899aabbccddeeff";
    private static final String KEY = "0f1e2d3c4b5a69788796a5b4c3d2e1f0";
    private static final String IV = "a0b1c2d3e4f5061728394a5b6c7d8e9f";
    private static Path out;
    private static PackageJob.Info info;

    @BeforeAll
    static void build() throws Exception {
        Assumptions.assumeTrue(Ffmpeg.available(), "ffmpeg not installed");
        Path work = Files.createTempDirectory(Ffmpeg.tempRoot(), "pkgtest");
        Path clip = work.resolve("clip.mp4");
        Ffmpeg.run(List.of("-f", "lavfi", "-i", "testsrc2=s=854x480:r=25:d=4", "-f", "lavfi", "-i",
                "sine=d=4", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-shortest", clip.toString()));
        out = work.resolve("out");
        PackageJob j = new PackageJob();
        j.input = clip;
        j.out = out;
        j.keyId = KID;
        j.key = KEY;
        j.iv = IV;
        j.segment = 2;
        info = j.run();
    }

    @Test
    void playlistsSayKeyAndNeverUpscale() throws Exception {
        String master = Files.readString(out.resolve("master.m3u8"));
        assertTrue(master.contains("v480.m3u8") && master.contains("v360.m3u8"));
        assertFalse(master.contains("v720.m3u8"));
        assertEquals(List.of("480", "360"), info.renditions());
        for (String h : info.renditions()) {
            String pl = Files.readString(out.resolve("v" + h + ".m3u8"));
            assertTrue(pl.contains("URI=\"key\""), pl);
            assertTrue(pl.contains("IV=0x" + IV), pl);
            assertTrue(pl.contains("v" + h + "_00001.ts"));
        }
        assertFalse(Files.exists(out.resolve("key")));
    }

    @Test
    void segmentsAreEncryptedAndDecrypt() throws Exception {
        byte[] seg = Files.readAllBytes(out.resolve("v480_00001.ts"));
        boolean plain = true;
        for (int o = 0; o < Math.min(seg.length, 188 * 5); o += 188) {
            plain &= seg[o] == 0x47;
        }
        assertFalse(plain, "segment looks like plain MPEG-TS");
        Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Hex.decode(KEY), "AES"), new IvParameterSpec(Hex.decode(IV)));
        byte[] dec = c.doFinal(seg, 0, seg.length - seg.length % 16);
        assertEquals(0x47, dec[0]);
        assertEquals(0x47, dec[188]);
    }

    @Test
    void cencHasPsshAndTenc() throws Exception {
        byte[] d = Files.readAllBytes(out.resolve("cenc.mp4"));
        int[] pssh = Mp4.find(d, 0, d.length, "moov/pssh");
        assertNotNull(pssh);
        assertArrayEquals(Mp4.COMMON_SYSTEM_ID, Arrays.copyOfRange(d, pssh[0] + 12, pssh[0] + 28));
        assertArrayEquals(Hex.decode(KID), Arrays.copyOfRange(d, pssh[0] + 32, pssh[0] + 48));
        assertNotNull(Mp4.find(d, 0, d.length, "moov/trak/mdia/minf/stbl/stsd/encv/sinf/schi/tenc"));
        assertNotNull(Mp4.find(d, 0, d.length, "moof"));
    }

    @Test
    void hashesInfoAndImages() throws Exception {
        List<String> lines = Files.readAllLines(out.resolve("hashes.txt"));
        assertFalse(lines.isEmpty());
        for (String l : lines) {
            assertTrue(l.matches("[0-9a-f]{16}"), l);
        }
        Map<String, Object> m = Json.parseObject(Files.readString(out.resolve("info.json")));
        assertEquals(854L, m.get("width"));
        assertEquals(480L, m.get("height"));
        assertEquals(List.of("AES_128", "CLEARKEY"), m.get("schemes"));
        assertEquals(lines, m.get("frameHashes"));
        assertTrue(((Number) m.get("durationSeconds")).doubleValue() > 3);
        assertTrue(Files.size(out.resolve("poster.jpg")) > 500);
        assertTrue(Files.size(out.resolve("sprite.jpg")) > 500);
        try (Stream<Path> s = Files.list(out)) {
            assertTrue(s.noneMatch(Files::isDirectory));
        }
    }

    @Test
    void jsonRoundTrip() {
        Object o = Json.parse("{\"a\":[1,2.5,\"x\\n\"],\"b\":{\"c\":true,\"d\":null}}");
        assertEquals("{\"a\":[1,2.5,\"x\\n\"],\"b\":{\"c\":true,\"d\":null}}", Json.write(o));
    }
}
