/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.media;

import com.codename1.io.Util;
import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Pure cross platform fallback implementation of `SoundPoolPeer`, built entirely
/// on top of `MediaManager`.
///
/// Each loaded sound keeps a small ring of `Media` instances that are
/// `Media#prepare()`d up front, so the decode/buffer cost is paid at load time. A
/// play looks for an idle instance in the ring, rewinds it with `Media#setTime(int)`
/// and starts it. This works on every platform with no native code, but inherits
/// the latency of the platform's general purpose media player and cannot change
/// pitch or pan -- `rate` and `pan` are ignored. Ports that provide a real low
/// latency `SoundPoolPeer` are used in preference to this (see
/// `com.codename1.gaming.SoundPool#isNativeAccelerated()`).
class MediaSoundPoolPeer implements SoundPoolPeer {
    private static final int RING_DEFAULT = 4;

    private final int maxStreams;
    private final List sounds = new ArrayList();
    private final Map voices = new HashMap();
    private int activeVoices;
    private int nextVoiceId = 1;

    MediaSoundPoolPeer(int maxStreams) {
        this.maxStreams = maxStreams < 1 ? 1 : maxStreams;
    }

    private static final class Slot {
        Media media;
        boolean busy;
        int voiceId;
        int loopsRemaining;
    }

    private static final class Sound {
        String uri;
        byte[] data;
        String mime;
        Slot[] ring;
    }

    /// Restarts a looping voice from the EDT (scheduled from the off-EDT media
    /// completion callback).
    private static final class RestartVoice implements Runnable {
        private final Slot slot;

        RestartVoice(Slot slot) {
            this.slot = slot;
        }

        public void run() {
            try {
                slot.media.setTime(0);
            } catch (Throwable t) {
            }
            slot.media.play();
        }
    }

    private Media newMedia(Sound s, final Slot slot) throws IOException {
        Runnable onComplete = new Runnable() {
            public void run() {
                onVoiceComplete(slot);
            }
        };
        if (s.uri != null) {
            return MediaManager.createMedia(s.uri, false, onComplete);
        }
        return MediaManager.createMedia(new ByteArrayInputStream(s.data), s.mime, onComplete);
    }

    private Sound buildSound(Sound s) throws IOException {
        int ringSize = Math.min(maxStreams, RING_DEFAULT);
        if (ringSize < 1) {
            ringSize = 1;
        }
        s.ring = new Slot[ringSize];
        for (int i = 0; i < ringSize; i++) {
            Slot slot = new Slot();
            slot.media = newMedia(s, slot);
            try {
                slot.media.prepare();
            } catch (Throwable t) {
                // prepare is best effort; play will still work
            }
            s.ring[i] = slot;
        }
        synchronized (this) {
            sounds.add(s);
        }
        return s;
    }

    public Object loadSound(InputStream data, String mimeType) throws IOException {
        byte[] bytes = Util.readInputStream(data);
        Util.cleanup(data);
        Sound s = new Sound();
        s.data = bytes;
        s.mime = mimeType;
        return buildSound(s);
    }

    public Object loadSound(String uri) throws IOException {
        Sound s = new Sound();
        s.uri = uri;
        return buildSound(s);
    }

    public synchronized int play(Object soundHandle, float volume, float pan, float rate, int loop) {
        if (activeVoices >= maxStreams) {
            return -1;
        }
        Sound s = (Sound) soundHandle;
        Slot free = null;
        for (int i = 0; i < s.ring.length; i++) {
            if (!s.ring[i].busy) {
                free = s.ring[i];
                break;
            }
        }
        if (free == null) {
            return -1;
        }
        int vid = nextVoiceId++;
        free.busy = true;
        free.voiceId = vid;
        free.loopsRemaining = loop;
        voices.put(Integer.valueOf(vid), free);
        activeVoices++;
        applyVolume(free.media, volume);
        try {
            free.media.setTime(0);
        } catch (Throwable t) {
            // some media may not support seeking; ignore
        }
        free.media.play();
        return vid;
    }

    private void onVoiceComplete(final Slot slot) {
        // Media completion callbacks arrive off the EDT; restart looping playback
        // on the EDT and keep bookkeeping synchronized.
        synchronized (this) {
            if (!slot.busy) {
                return;
            }
            if (slot.loopsRemaining != 0) {
                if (slot.loopsRemaining > 0) {
                    slot.loopsRemaining--;
                }
                Display.getInstance().callSerially(new RestartVoice(slot));
                return;
            }
            voices.remove(Integer.valueOf(slot.voiceId));
            slot.busy = false;
            activeVoices--;
        }
    }

    private static void applyVolume(Media m, float volume) {
        int pct = Math.round(volume * 100);
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        try {
            m.setVolume(pct);
        } catch (Throwable t) {
        }
    }

    public synchronized void setVolume(int voiceId, float volume) {
        Slot slot = (Slot) voices.get(Integer.valueOf(voiceId));
        if (slot != null) {
            applyVolume(slot.media, volume);
        }
    }

    public void setRate(int voiceId, float rate) {
        // not supported by the generic Media player
    }

    public void setPan(int voiceId, float pan) {
        // not supported by the generic Media player
    }

    public synchronized void pauseVoice(int voiceId) {
        Slot slot = (Slot) voices.get(Integer.valueOf(voiceId));
        if (slot != null) {
            slot.media.pause();
        }
    }

    public synchronized void resumeVoice(int voiceId) {
        Slot slot = (Slot) voices.get(Integer.valueOf(voiceId));
        if (slot != null) {
            slot.media.play();
        }
    }

    public synchronized void stopVoice(int voiceId) {
        Slot slot = (Slot) voices.remove(Integer.valueOf(voiceId));
        if (slot != null && slot.busy) {
            stopSlot(slot);
            activeVoices--;
        }
    }

    private static void stopSlot(Slot slot) {
        slot.loopsRemaining = 0;
        try {
            slot.media.pause();
            slot.media.setTime(0);
        } catch (Throwable t) {
        }
        slot.busy = false;
    }

    public synchronized void stopAll() {
        for (int i = 0; i < sounds.size(); i++) {
            Sound s = (Sound) sounds.get(i);
            for (int j = 0; j < s.ring.length; j++) {
                if (s.ring[j].busy) {
                    stopSlot(s.ring[j]);
                }
            }
        }
        voices.clear();
        activeVoices = 0;
    }

    public synchronized void autoPause() {
        for (int i = 0; i < sounds.size(); i++) {
            Sound s = (Sound) sounds.get(i);
            for (int j = 0; j < s.ring.length; j++) {
                if (s.ring[j].busy) {
                    s.ring[j].media.pause();
                }
            }
        }
    }

    public synchronized void autoResume() {
        for (int i = 0; i < sounds.size(); i++) {
            Sound s = (Sound) sounds.get(i);
            for (int j = 0; j < s.ring.length; j++) {
                if (s.ring[j].busy) {
                    s.ring[j].media.play();
                }
            }
        }
    }

    public synchronized void unloadSound(Object soundHandle) {
        Sound s = (Sound) soundHandle;
        for (int j = 0; j < s.ring.length; j++) {
            Slot slot = s.ring[j];
            if (slot.busy) {
                voices.remove(Integer.valueOf(slot.voiceId));
                activeVoices--;
                slot.busy = false;
            }
            try {
                slot.media.cleanup();
            } catch (Throwable t) {
            }
        }
        sounds.remove(s);
    }

    public synchronized void release() {
        for (int i = 0; i < sounds.size(); i++) {
            Sound s = (Sound) sounds.get(i);
            for (int j = 0; j < s.ring.length; j++) {
                try {
                    s.ring[j].media.cleanup();
                } catch (Throwable t) {
                }
            }
        }
        sounds.clear();
        voices.clear();
        activeVoices = 0;
    }
}
