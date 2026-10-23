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

import java.util.HashMap;
import java.util.Map;

/// Captures what the application plays, for the recording's audio track.
///
/// Every place the simulator hands PCM to the sound card calls [#write] with the
/// same bytes: the sound pool's mixer (`JavaSESoundPool`, short effects and game
/// sounds) and the ffmpeg media player (`FFMPEGMedia`, music and longer clips).
/// Both play 44.1 kHz, 16-bit little-endian stereo, the only format accepted here.
///
/// Each source is placed on the recording's timeline by a cursor of its own: it
/// starts at the moment the source first writes and advances by what it wrote,
/// so a line that buffers ahead stays contiguous. A source that pauses longer
/// than its cursor lead (the line drained) is moved back to the wall clock.
public final class AppAudioTap {
    private static final int SAMPLE_RATE = 44100;
    private static AppAudioTap active;

    private final long startMs;
    private float[] mix = new float[SAMPLE_RATE * 10];
    private int length;
    private final Map<Object, Long> cursors = new HashMap<Object, Long>();

    AppAudioTap(long startMs) {
        this.startMs = startMs;
    }

    static synchronized void setActive(AppAudioTap tap) {
        active = tap;
    }

    private static synchronized AppAudioTap current() {
        return active;
    }

    /// Hands played audio to the recording, when one is capturing app audio.
    /// `pcm` is 44.1 kHz, 16-bit little-endian stereo; `gain` is the volume the
    /// source plays it at (1 for full). Cheap and does nothing when not recording.
    public static void write(Object source, byte[] pcm, int off, int len, float gain) {
        AppAudioTap t = current();
        if (t != null) {
            t.add(source, pcm, off, len, gain, System.currentTimeMillis());
        }
    }

    synchronized void add(Object source, byte[] pcm, int off, int len, float gain, long nowMs) {
        int frames = len / 4;
        if (frames <= 0) {
            return;
        }
        long now = Math.max(0, (nowMs - startMs) * SAMPLE_RATE / 1000L);
        Long c = cursors.get(source);
        long at = c == null || c.longValue() < now ? now : c.longValue();
        cursors.put(source, Long.valueOf(at + frames));
        int end = (int) Math.min(Integer.MAX_VALUE - 1, at + frames);
        if (end > mix.length) {
            float[] grown = new float[Math.max(end, mix.length * 2)];
            System.arraycopy(mix, 0, grown, 0, length);
            mix = grown;
        }
        for (int i = 0; i < frames && at + i < end; i++) {
            int p = off + i * 4;
            int l = (short) ((pcm[p] & 0xff) | (pcm[p + 1] << 8));
            int r = (short) ((pcm[p + 2] & 0xff) | (pcm[p + 3] << 8));
            mix[(int) at + i] += (l + r) * 0.5f * gain;
        }
        length = Math.max(length, end);
    }

    /// Mixes the captured audio into a mono track, clipping at full scale.
    synchronized void mixInto(short[] pcm) {
        int n = Math.min(pcm.length, length);
        for (int i = 0; i < n; i++) {
            float v = pcm[i] + mix[i];
            pcm[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(v)));
        }
    }
}
