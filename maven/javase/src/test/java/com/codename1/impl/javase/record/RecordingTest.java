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
package com.codename1.impl.javase.record;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordingTest {

    @Test
    void optionsApplyMcpArguments() {
        RecordingOptions o = new RecordingOptions();
        Map<String, Object> a = new HashMap<String, Object>();
        a.put("format", "WebM");
        a.put("fps", Integer.valueOf(24));
        a.put("frame", "skin");
        a.put("touches", "dot");
        a.put("touchColor", "#ff0000");
        a.put("tapSound", Boolean.TRUE);
        a.put("audio", "microphone");
        o.apply(a);
        assertEquals("webm", o.getFormat());
        assertEquals(24, o.getFps());
        assertEquals(RecordingOptions.Frame.SKIN, o.getFrame());
        assertEquals(TouchOverlay.Style.DOT, o.getTouches());
        assertEquals(0xffff0000, o.getTouchColor());
        assertTrue(o.hasAudioTrack());
    }

    @Test
    void jsonParserStringBooleansAreAccepted() {
        // CN1's JSONParser yields "true" (a String) unless told to use Booleans; a
        // tapSound sent over MCP arrived that way and was silently ignored.
        RecordingOptions o = new RecordingOptions();
        Map<String, Object> a = new HashMap<String, Object>();
        a.put("tapSound", "true");
        a.put("touchTrail", "false");
        o.apply(a);
        assertTrue(o.isTapSound());
        assertFalse(o.isTouchTrail());
        assertTrue(o.hasAudioTrack());
    }

    @Test
    void pathExtensionIsTheFormatWhenNoneIsGiven() {
        RecordingOptions o = new RecordingOptions();
        Map<String, Object> a = new HashMap<String, Object>();
        a.put("path", "/tmp/demo.GIF");
        o.apply(a);
        assertEquals("gif", o.getFormat());
        a.put("format", "mp4");
        o.apply(a);
        assertEquals("mp4", o.getFormat());
    }

    @Test
    void gifNeverHasAudio() {
        RecordingOptions o = new RecordingOptions();
        o.setTapSound(true);
        o.setFormat("gif");
        assertFalse(o.hasAudioTrack());
    }

    @Test
    void badValuesAreRejectedByName() {
        RecordingOptions o = new RecordingOptions();
        assertThrows(IllegalArgumentException.class, () -> o.setFormat("avi"));
        assertThrows(IllegalArgumentException.class, () -> o.setFps(0));
        Map<String, Object> a = new HashMap<String, Object>();
        a.put("touches", "sparkles");
        assertThrows(IllegalArgumentException.class, () -> o.apply(a));
    }

    @Test
    void outputDefaultsToHomeWithFormatExtension() {
        RecordingOptions o = new RecordingOptions();
        o.setFormat("mov");
        assertTrue(o.resolveOutput().getName().endsWith(".mov"));
        o.setPath("file:///tmp/x.mp4");
        assertEquals("/tmp/x.mp4", o.resolveOutput().getPath());
    }

    @Test
    void tapClicksLandAtTheirTimestamps() {
        short[] pcm = SimulatorRecorder.buildAudio(1000, null, Arrays.asList(Long.valueOf(500)));
        assertEquals(44100, pcm.length);
        int firstLoud = -1;
        for (int i = 0; i < pcm.length; i++) {
            if (Math.abs(pcm[i]) > 1000) {
                firstLoud = i;
                break;
            }
        }
        // 500 ms in, give or take the click's own rise.
        assertTrue(firstLoud >= 22050 && firstLoud < 22050 + 100, "first loud sample " + firstLoud);
        short[] silent = SimulatorRecorder.buildAudio(200, null, Collections.<Long>emptyList());
        for (short s : silent) {
            assertEquals(0, s);
        }
    }

    @Test
    void microphoneSamplesAreDecodedLittleEndian() {
        byte[] mic = {0x34, 0x12, (byte) 0xff, (byte) 0xff};
        short[] pcm = SimulatorRecorder.buildAudio(100, mic, Collections.<Long>emptyList());
        assertEquals(0x1234, pcm[0]);
        assertEquals(-1, pcm[1]);
    }

    @Test
    void touchOverlayDrawsUnderTheTouchAndFades() throws Exception {
        TouchOverlay t = new TouchOverlay();
        t.setStyle(TouchOverlay.Style.DOT);
        t.setColor(0xffff0000);
        t.pressed(50, 50);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        t.paint(g, 1.0, 0);
        g.dispose();
        assertEquals(0xffff0000, img.getRGB(50, 50));
        assertEquals(0, img.getRGB(5, 5));
        t.released(50, 50);
        assertTrue(t.hasVisibleTouches());
        Thread.sleep(500);
        assertFalse(t.hasVisibleTouches());
    }

    @Test
    void touchOverlayScalesToBufferPixels() {
        TouchOverlay t = new TouchOverlay();
        t.setStyle(TouchOverlay.Style.DOT);
        t.setColor(0xff00ff00);
        t.setRadius(4);
        t.pressed(10, 10);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        t.paint(g, 2.0, 0);
        g.dispose();
        assertEquals(0xff00ff00, img.getRGB(20, 20));
        assertEquals(0, img.getRGB(10, 10));
    }

    @Test
    void captionDrawsInTheLowerPart() {
        BufferedImage img = new BufferedImage(400, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        SimulatorRecorder.paintCaption(g, "A new feature that wraps across more than one line of text", 400, 800);
        g.dispose();
        assertEquals(0, img.getRGB(200, 100));
        boolean drawnLow = false;
        for (int y = 600; y < 800; y++) {
            if (img.getRGB(200, y) != 0) {
                drawnLow = true;
                break;
            }
        }
        assertTrue(drawnLow);
    }

    @Test
    void autoStopAtTheLimitKeepsTheResult(@org.junit.jupiter.api.io.TempDir java.io.File dir) throws Exception {
        SimulatorRecorder rec = new SimulatorRecorder(new SimulatorRecorder.FrameSource() {
            public BufferedImage captureFrame(RecordingOptions.Frame frame) {
                return new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
            }
        });
        RecordingOptions o = new RecordingOptions();
        o.setPath(new java.io.File(dir, "auto.mp4").getAbsolutePath());
        o.setFps(10);
        o.setMaxSeconds(1);
        rec.start(o);
        long deadline = System.currentTimeMillis() + 20000;
        while ((rec.isRecording() || (rec.getLastResult() == null && rec.getLastError() == null))
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertFalse(rec.isRecording());
        assertEquals(null, rec.getLastError());
        assertTrue(rec.getLastResult().file.exists());
        assertThrows(IllegalStateException.class, rec::stop);
        // A new recording may start once the previous one is finished.
        o.setPath(new java.io.File(dir, "again.mp4").getAbsolutePath());
        rec.start(o);
        assertTrue(rec.isRecording());
        assertTrue(rec.stop().file.exists());
    }

    @Test
    void appAudioIsTheDefaultAndMixedOnTheTimeline() {
        assertEquals(RecordingOptions.Audio.APP, new RecordingOptions().getAudio());
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("audio", "app_and_microphone");
        RecordingOptions o = new RecordingOptions();
        o.apply(args);
        assertEquals(RecordingOptions.Audio.APP_AND_MICROPHONE, o.getAudio());

        AppAudioTap tap = new AppAudioTap(1000);
        // 100 stereo frames of a constant 1000 at t=1500ms (sample 22050)...
        byte[] pcm = new byte[400];
        for (int i = 0; i < 200; i++) {
            pcm[i * 2] = (byte) (1000 & 0xff);
            pcm[i * 2 + 1] = (byte) (1000 >> 8);
        }
        Object source = new Object();
        tap.add(source, pcm, 0, pcm.length, 1f, 1500);
        // ...and a second write right after lands contiguously, not at the clock.
        tap.add(source, pcm, 0, pcm.length, 0.5f, 1500);
        short[] out = new short[44100];
        tap.mixInto(out);
        assertEquals(0, out[22049]);
        assertEquals(1000, out[22050]);
        assertEquals(1000, out[22149]);
        assertEquals(500, out[22150]);
        assertEquals(0, out[22250]);
    }
}
