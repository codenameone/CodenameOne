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
import com.codename1.media.audio.AudioBufferSourceNode;
import com.codename1.media.audio.AudioContext;
import com.codename1.media.audio.AudioData;
import com.codename1.media.audio.GainNode;
import com.codename1.media.audio.StereoPannerNode;
import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/// The `com.codename1.gaming.SoundPool` peer for a platform with a real-time
/// audio graph and no native pool: each voice is a buffer source through a
/// gain and a stereo panner, so volume, pan and rate all work and a looped
/// sound has no gap.
///
/// The graph decodes WAV. A sound in any other format is loaded into a
/// [MediaSoundPoolPeer] instead and played as that peer plays it, so one pool
/// takes every format; such a voice gets the volume and ignores pan and rate,
/// as before.
///
/// The context opens with the first sound loaded, and every call is expected
/// on the EDT, which is also where voices report their end.
class GraphSoundPoolPeer implements CompletionAwareSoundPoolPeer {
    private final int maxStreams;
    private final MediaSoundPoolPeer media;
    private AudioContext context;
    private boolean refused;
    private VoiceCompletionListener listener;
    private int nextVoiceId = 1;
    private final ArrayList voices = new ArrayList();

    /// A sound the graph plays, or a handle of the fallback peer.
    private static final class Sound {
        AudioData data;
        Object fallback;
    }

    private static final class Voice {
        int id;
        /// The fallback peer's id for this voice, or 0 on the graph.
        int fallbackId;
        Sound sound;
        AudioBufferSourceNode source;
        GainNode gain;
        StereoPannerNode panner;
        float rate;
        boolean loop;
        int repeats;
        boolean paused;
        double offset;
        double startedAt;
    }

    GraphSoundPoolPeer(int maxStreams) {
        this.maxStreams = maxStreams;
        media = new MediaSoundPoolPeer(maxStreams);
        media.setVoiceCompletionListener(new VoiceCompletionListener() {
            @Override
            public void onVoiceComplete(int fallbackId) {
                Voice v = byFallback(fallbackId);
                if (v != null) {
                    finished(v);
                }
            }
        });
    }

    private AudioContext context() {
        if (context == null && !refused) {
            context = AudioContext.create();
            refused = context == null;
        }
        return context;
    }

    private Object load(byte[] bytes, String mimeType) throws IOException {
        AudioContext c = context();
        AudioData data = c == null ? null : c.decodeAudioData(bytes);
        Sound s = new Sound();
        if (data != null) {
            s.data = data;
        } else {
            s.fallback = media.loadSound(new ByteArrayInputStream(bytes), mimeType);
        }
        return s;
    }

    @Override
    public Object loadSound(InputStream data, String mimeType) throws IOException {
        byte[] bytes = Util.readInputStream(data);
        Util.cleanup(data);
        return load(bytes, mimeType);
    }

    @Override
    public Object loadSound(String uri) throws IOException {
        if (uri != null && uri.startsWith("jar://")) {
            InputStream in = Display.getInstance().getResourceAsStream(getClass(), uri.substring(6)); // NOPMD CloseResource - loadSound closes it
            if (in != null) {
                return loadSound(in, null);
            }
        }
        Sound s = new Sound();
        s.fallback = media.loadSound(uri);
        return s;
    }

    private Voice voice(int id) {
        for (Object o : voices) {
            Voice v = (Voice) o;
            if (v.id == id) {
                return v;
            }
        }
        return null;
    }

    private Voice byFallback(int fallbackId) {
        for (Object o : voices) {
            Voice v = (Voice) o;
            if (v.fallbackId == fallbackId && v.fallbackId != 0) {
                return v;
            }
        }
        return null;
    }

    @Override
    public int play(Object sound, float volume, float pan, float rate, int loop) {
        if (!(sound instanceof Sound) || voices.size() >= maxStreams) {
            return -1;
        }
        Sound s = (Sound) sound;
        Voice v = new Voice();
        v.sound = s;
        if (s.data == null) {
            int f = media.play(s.fallback, volume, pan, rate, loop);
            if (f < 0) {
                return -1;
            }
            v.fallbackId = f;
        } else {
            v.rate = rate;
            v.loop = loop < 0;
            v.repeats = loop > 0 ? loop : 0;
            v.gain = context.createGain();
            v.gain.getGain().setValue(volume);
            v.panner = context.createStereoPanner();
            v.panner.getPan().setValue(pan);
            v.gain.connect(v.panner).connect(context.getDestination());
        }
        v.id = nextVoiceId++;
        voices.add(v);
        if (v.fallbackId == 0) {
            start(v);
        }
        return v.id;
    }

    private void start(final Voice v) {
        final AudioBufferSourceNode src = context.createBufferSource();
        src.setBuffer(v.sound.data);
        src.setLoop(v.loop);
        src.getPlaybackRate().setValue(v.rate);
        src.connect(v.gain);
        src.setOnEnded(new Runnable() {
            @Override
            public void run() {
                if (v.source != src) { // NOPMD CompareObjectsWithEquals - a replaced source
                    return;
                }
                v.source = null;
                src.disconnect();
                if (v.repeats > 0) {
                    v.repeats--;
                    v.offset = 0;
                    start(v);
                } else {
                    finished(v);
                }
            }
        });
        v.source = src;
        v.startedAt = context.getCurrentTime();
        src.start(0, v.offset);
    }

    private void halt(Voice v) {
        AudioBufferSourceNode s = v.source;
        if (s != null) {
            double t = v.offset + (context.getCurrentTime() - v.startedAt) * v.rate;
            double length = v.sound.data.getDuration();
            v.offset = v.loop && length > 0 ? t % length : t;
            v.source = null;
            s.stop();
            s.disconnect();
        }
    }

    private void remove(Voice v) {
        voices.remove(v);
        if (v.fallbackId == 0) {
            halt(v);
            v.gain.disconnect();
            v.panner.disconnect();
        }
    }

    private void finished(Voice v) {
        remove(v);
        VoiceCompletionListener l = listener;
        if (l != null) {
            l.onVoiceComplete(v.id);
        }
    }

    @Override
    public void setVolume(int voiceId, float volume) {
        Voice v = voice(voiceId);
        if (v == null) {
            return;
        }
        if (v.fallbackId != 0) {
            media.setVolume(v.fallbackId, volume);
        } else {
            v.gain.getGain().setValue(volume);
        }
    }

    @Override
    public void setRate(int voiceId, float rate) {
        Voice v = voice(voiceId);
        if (v == null) {
            return;
        }
        if (v.fallbackId != 0) {
            media.setRate(v.fallbackId, rate);
            return;
        }
        if (v.source != null) {
            v.offset += (context.getCurrentTime() - v.startedAt) * v.rate;
            v.startedAt = context.getCurrentTime();
            v.source.getPlaybackRate().setValue(rate);
        }
        v.rate = rate;
    }

    @Override
    public void setPan(int voiceId, float pan) {
        Voice v = voice(voiceId);
        if (v == null) {
            return;
        }
        if (v.fallbackId != 0) {
            media.setPan(v.fallbackId, pan);
        } else {
            v.panner.getPan().setValue(pan);
        }
    }

    @Override
    public void pauseVoice(int voiceId) {
        Voice v = voice(voiceId);
        if (v == null) {
            return;
        }
        if (v.fallbackId != 0) {
            media.pauseVoice(v.fallbackId);
        } else if (!v.paused) {
            v.paused = true;
            halt(v);
        }
    }

    @Override
    public void resumeVoice(int voiceId) {
        Voice v = voice(voiceId);
        if (v == null) {
            return;
        }
        if (v.fallbackId != 0) {
            media.resumeVoice(v.fallbackId);
        } else if (v.paused) {
            v.paused = false;
            start(v);
        }
    }

    @Override
    public void stopVoice(int voiceId) {
        Voice v = voice(voiceId);
        if (v == null) {
            return;
        }
        if (v.fallbackId != 0) {
            media.stopVoice(v.fallbackId);
        }
        remove(v);
    }

    @Override
    public void stopAll() {
        while (!voices.isEmpty()) {
            remove((Voice) voices.get(voices.size() - 1));
        }
        media.stopAll();
    }

    @Override
    public void autoPause() {
        if (context != null) {
            context.suspend();
        }
        media.autoPause();
    }

    @Override
    public void autoResume() {
        if (context != null) {
            context.resume();
        }
        media.autoResume();
    }

    @Override
    public void unloadSound(Object sound) {
        if (!(sound instanceof Sound)) {
            return;
        }
        Sound s = (Sound) sound;
        for (int i = voices.size() - 1; i >= 0; i--) {
            Voice v = (Voice) voices.get(i);
            if (v.sound == s) { // NOPMD CompareObjectsWithEquals
                if (v.fallbackId != 0) {
                    media.stopVoice(v.fallbackId);
                }
                remove(v);
            }
        }
        if (s.fallback != null) {
            media.unloadSound(s.fallback);
        }
        s.data = null;
    }

    @Override
    public void release() {
        stopAll();
        media.release();
        if (context != null) {
            context.close();
            context = null;
        }
    }

    @Override
    public void setVoiceCompletionListener(VoiceCompletionListener listener) {
        this.listener = listener;
    }
}
