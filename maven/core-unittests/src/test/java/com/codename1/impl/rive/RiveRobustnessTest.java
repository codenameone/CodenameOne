/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.rive;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Inputs a hostile or broken file can feed the runtime: none may hang,
/// throw, or build without bound.
class RiveRobustnessTest {

    private static LinearAnimationInstance pingPong(float speed) {
        LinearAnimation a = new LinearAnimation();
        a.setFps(60);
        a.setDuration(60);
        a.setSpeed(speed);
        a.setLoopValue(LinearAnimation.LOOP_PING_PONG);
        return new LinearAnimationInstance(a, new Artboard());
    }

    @Test
    @Timeout(5)
    void pingPongSurvivesHugeAndInfiniteAdvances() {
        LinearAnimationInstance i = pingPong(1f);
        i.advance(Float.MAX_VALUE);
        assertTrue(i.getTime() >= 0 && i.getTime() <= 1f, "time " + i.getTime());
        i.advance(Float.POSITIVE_INFINITY);
        assertTrue(i.getTime() >= 0 && i.getTime() <= 1f, "time " + i.getTime());
        LinearAnimationInstance fast = pingPong(1e30f);
        fast.advance(1f / 60);
        assertTrue(fast.getTime() >= 0 && fast.getTime() <= 1f, "time " + fast.getTime());
        LinearAnimationInstance big = pingPong(1f);
        big.advance(12345.25f);
        assertTrue(big.getTime() >= 0 && big.getTime() <= 1f, "time " + big.getTime());
        // 12345.25 s is 6172 round trips plus 1.25 s: forward to the end,
        // then 0.25 s back
        assertEquals(0.75f, big.getTime(), 0.01f);
    }

    @Test
    void roundingIsHalfAwayFromZeroLikeStdRound() {
        assertEquals(-3.0, DataConverterOperation.roundHalfAway(-2.5), 0);
        assertEquals(3.0, DataConverterOperation.roundHalfAway(2.5), 0);
        assertEquals(-2.0, DataConverterOperation.roundHalfAway(-2.4), 0);
        assertEquals(0.0, DataConverterOperation.roundHalfAway(0.4), 0);
    }

    @Test
    @Timeout(10)
    void artboardsThatContainThemselvesStop() {
        final int[] built = new int[1];
        final RiveFileContext[] ctx = new RiveFileContext[1];
        ctx[0] = new RiveFileContext(new RiveFileContext.ArtboardFactory() {
            @Override
            public Artboard instance(int index) {
                built[0]++;
                // 0 nests 1, 1 nests 0 and itself
                if (index == 0) {
                    ctx[0].instanceArtboard(1);
                } else {
                    ctx[0].instanceArtboard(0);
                    ctx[0].instanceArtboard(1);
                }
                return new Artboard();
            }
        });
        assertNotNull(ctx[0].instanceArtboard(0));
        assertEquals(2, built[0], "each artboard of the cycle is built once");
    }

    @Test
    @Timeout(10)
    void exponentialFanOutIsCapped() {
        final int[] built = new int[1];
        final RiveFileContext[] ctx = new RiveFileContext[1];
        ctx[0] = new RiveFileContext(new RiveFileContext.ArtboardFactory() {
            @Override
            public Artboard instance(int index) {
                built[0]++;
                // each of 40 levels nests the next one four times: 4^40 unbounded
                if (index < 40) {
                    for (int k = 0; k < 4; k++) {
                        ctx[0].instanceArtboard(index + 1);
                    }
                }
                return new Artboard();
            }
        });
        assertNotNull(ctx[0].instanceArtboard(0));
        assertTrue(built[0] <= RiveFileContext.MAX_INSTANCES, "built " + built[0]);
        // a second top-level instancing gets a fresh budget
        built[0] = 0;
        assertNotNull(ctx[0].instanceArtboard(0));
        assertTrue(built[0] > 1);
    }

    private static int table(byte[] d, String tag) {
        int n = ((d[4] & 0xff) << 8) | (d[5] & 0xff);
        for (int i = 0; i < n; i++) {
            int rec = 12 + i * 16;
            if (new String(d, rec, 4, java.nio.charset.StandardCharsets.US_ASCII).equals(tag)) {
                return rec;
            }
        }
        return -1;
    }

    private static int u32(byte[] d, int p) {
        return ((d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
    }

    @Test
    void aMalformedFontDrawsNothingInsteadOfThrowing() throws Exception {
        byte[] good = Files.readAllBytes(new File("../../CodenameOne/src/material-design-font.ttf").toPath());
        RiveFont ok = RiveFont.decode(good);
        assertNotNull(ok);
        int glyph = -1;
        for (int g = 1; g < 200 && glyph < 0; g++) {
            if (ok.glyphPath(g).pointCount > 0) {
                glyph = g;
            }
        }
        assertTrue(glyph > 0, "the font has an outlined glyph");
        int headRec = table(good, "head");
        int locaRec = table(good, "loca");
        assertTrue(headRec > 0 && locaRec > 0);
        boolean longLoca = (((good[u32(good, headRec + 8) + 50] & 0xff) << 8) | (good[u32(good, headRec + 8) + 51] & 0xff)) != 0;
        int loca = u32(good, locaRec + 8);
        // Only that glyph's loca entry is broken, so the font still decodes:
        // its start gets the high bit (negative as an int) or runs far past
        // the glyf table.
        byte[] negative = Arrays.copyOf(good, good.length);
        byte[] pastEnd = Arrays.copyOf(good, good.length);
        if (longLoca) {
            int p = loca + glyph * 4;
            negative[p] = (byte) 0x80;
            pastEnd[p + 4] = (byte) 0x7f;
        } else {
            int p = loca + glyph * 2;
            pastEnd[p + 2] = (byte) 0xff;
            pastEnd[p + 3] = (byte) 0xff;
            negative = pastEnd;
        }
        for (byte[] bad : new byte[][] {negative, pastEnd}) {
            RiveFont f = RiveFont.decode(bad);
            assertNotNull(f, "a single broken loca entry still decodes");
            RivePath path = f.glyphPath(glyph);
            assertEquals(0, path.pointCount, "the broken glyph draws nothing");
            assertTrue(path == f.glyphPath(glyph), "and is cached");
        }
        assertNull(RiveFont.decode(new byte[4]));
    }
}
