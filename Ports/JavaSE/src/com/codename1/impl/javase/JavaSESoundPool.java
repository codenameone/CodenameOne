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
package com.codename1.impl.javase;

import com.codename1.media.SoundPoolPeer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/// Desktop / simulator low latency sound pool.
///
/// Implemented as a small software mixer: every loaded sound is decoded once to
/// 44.1kHz stereo float PCM, and a single `SourceDataLine` is fed by a mixer thread
/// that sums all active voices. Because the mixer owns sample playback directly it
/// supports per voice volume, stereo pan, pitch/rate (via fractional resampling)
/// and unbounded polyphony up to the configured voice cap -- matching what the
/// native mobile backends provide so the simulator behaves like a device.
class JavaSESoundPool implements com.codename1.media.CompletionAwareSoundPoolPeer {
    private static final float SAMPLE_RATE = 44100f;
    private static final int CHANNELS = 2;
    private static final int BUFFER_FRAMES = 1024;

    private final int maxStreams;
    private final SourceDataLine line;
    private final AudioFormat outputFormat;
    private final Map voices = new HashMap();
    private final Voice[] active;
    private int activeCount;
    private int nextVoiceId = 1;
    private volatile boolean alive = true;
    private final Thread mixerThread;
    private volatile com.codename1.media.VoiceCompletionListener completionListener;

    @Override
    public void setVoiceCompletionListener(com.codename1.media.VoiceCompletionListener listener) {
        this.completionListener = listener;
    }

    private static final class Sound {
        float[] pcm;   // interleaved stereo
        int frames;
    }

    private static final class Voice {
        Sound sound;
        double pos;        // fractional frame position
        float rate;
        float gainL;
        float gainR;
        int loopsRemaining; // -1 == infinite
        boolean paused;
        boolean stopped;    // explicitly stopped (vs. finished naturally)
        int voiceId;
    }

    JavaSESoundPool(int maxStreams) throws Exception {
        this.maxStreams = maxStreams < 1 ? 1 : maxStreams;
        this.active = new Voice[this.maxStreams];
        outputFormat = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outputFormat);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(outputFormat, BUFFER_FRAMES * CHANNELS * 2 * 4);
        line.start();
        mixerThread = new Thread(new Runnable() {
            public void run() {
                runMixer();
            }
        }, "JavaSESoundPool-mixer");
        mixerThread.setDaemon(true);
        mixerThread.start();
    }

    private void runMixer() {
        float[] mix = new float[BUFFER_FRAMES * CHANNELS];
        byte[] out = new byte[BUFFER_FRAMES * CHANNELS * 2];
        while (alive) {
            for (int i = 0; i < mix.length; i++) {
                mix[i] = 0f;
            }
            java.util.List completed = null;
            synchronized (this) {
                for (int v = 0; v < activeCount; v++) {
                    mixVoice(active[v], mix);
                }
                // compact finished voices (those marked sound == null)
                int w = 0;
                for (int v = 0; v < activeCount; v++) {
                    if (active[v].sound != null) {
                        active[w++] = active[v];
                    } else {
                        voices.remove(Integer.valueOf(active[v].voiceId));
                        if (!active[v].stopped) {
                            if (completed == null) {
                                completed = new java.util.ArrayList();
                            }
                            completed.add(Integer.valueOf(active[v].voiceId));
                        }
                    }
                }
                for (int v = w; v < activeCount; v++) {
                    active[v] = null;
                }
                activeCount = w;
            }
            if (completed != null) {
                com.codename1.media.VoiceCompletionListener l = completionListener;
                if (l != null) {
                    for (int i = 0; i < completed.size(); i++) {
                        l.onVoiceComplete(((Integer) completed.get(i)).intValue());
                    }
                }
            }
            for (int i = 0; i < mix.length; i++) {
                float s = mix[i];
                if (s > 1f) {
                    s = 1f;
                } else if (s < -1f) {
                    s = -1f;
                }
                int sample = (int) (s * 32767f);
                out[i * 2] = (byte) (sample & 0xff);
                out[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
            }
            line.write(out, 0, out.length);
        }
    }

    /// Mixes one voice into the buffer. Must be called holding the monitor.
    private void mixVoice(Voice voice, float[] mix) {
        if (voice.paused || voice.sound == null) {
            return;
        }
        Sound s = voice.sound;
        for (int f = 0; f < BUFFER_FRAMES; f++) {
            if (voice.pos >= s.frames) {
                if (voice.loopsRemaining != 0) {
                    if (voice.loopsRemaining > 0) {
                        voice.loopsRemaining--;
                    }
                    voice.pos -= s.frames;
                } else {
                    voice.sound = null; // finished; compacted after this pass
                    return;
                }
            }
            int i0 = (int) voice.pos;
            int i1 = i0 + 1 >= s.frames ? i0 : i0 + 1;
            float frac = (float) (voice.pos - i0);
            float l = s.pcm[i0 * 2] * (1 - frac) + s.pcm[i1 * 2] * frac;
            float r = s.pcm[i0 * 2 + 1] * (1 - frac) + s.pcm[i1 * 2 + 1] * frac;
            mix[f * 2] += l * voice.gainL;
            mix[f * 2 + 1] += r * voice.gainR;
            voice.pos += voice.rate;
        }
    }

    private Sound decode(InputStream in) throws IOException {
        AudioInputStream src = null;
        AudioInputStream pcm = null;
        try {
            src = AudioSystem.getAudioInputStream(in);
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE,
                    16, CHANNELS, CHANNELS * 2, SAMPLE_RATE, false);
            pcm = AudioSystem.getAudioInputStream(target, src);
            byte[] bytes = readAll(pcm);
            int frames = bytes.length / (CHANNELS * 2);
            float[] data = new float[frames * CHANNELS];
            for (int i = 0; i < frames * CHANNELS; i++) {
                int lo = bytes[i * 2] & 0xff;
                int hi = bytes[i * 2 + 1];
                short val = (short) ((hi << 8) | lo);
                data[i] = val / 32768f;
            }
            Sound snd = new Sound();
            snd.pcm = data;
            snd.frames = frames;
            return snd;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            close(pcm);
            close(src);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        int r;
        while ((r = in.read(buf)) > 0) {
            bos.write(buf, 0, r);
        }
        return bos.toByteArray();
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    public Object loadSound(InputStream data, String mimeType) throws IOException {
        try {
            return decode(data);
        } finally {
            close(data);
        }
    }

    public Object loadSound(String uri) throws IOException {
        InputStream in = JavaSESoundPool.class.getResourceAsStream(uri);
        if (in == null) {
            in = new ByteArrayInputStream(new byte[0]);
        }
        return loadSound(in, null);
    }

    public synchronized int play(Object soundHandle, float volume, float pan, float rate, int loop) {
        if (activeCount >= maxStreams) {
            return -1;
        }
        Sound s = (Sound) soundHandle;
        if (s == null || s.frames == 0) {
            return -1;
        }
        Voice v = new Voice();
        v.sound = s;
        v.pos = 0;
        v.rate = rate <= 0 ? 1f : rate;
        v.loopsRemaining = loop;
        applyGains(v, volume, pan);
        v.voiceId = nextVoiceId++;
        active[activeCount++] = v;
        voices.put(Integer.valueOf(v.voiceId), v);
        return v.voiceId;
    }

    private static void applyGains(Voice v, float volume, float pan) {
        if (volume < 0) {
            volume = 0;
        }
        if (volume > 1) {
            volume = 1;
        }
        if (pan < -1) {
            pan = -1;
        }
        if (pan > 1) {
            pan = 1;
        }
        // constant power pan
        float left = (float) Math.cos((pan + 1) * Math.PI / 4);
        float right = (float) Math.sin((pan + 1) * Math.PI / 4);
        v.gainL = volume * left;
        v.gainR = volume * right;
    }

    public synchronized void setVolume(int voiceId, float volume) {
        Voice v = (Voice) voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            // recover current pan from gains then re-apply
            float pan = panOf(v);
            applyGains(v, volume, pan);
        }
    }

    public synchronized void setRate(int voiceId, float rate) {
        Voice v = (Voice) voices.get(Integer.valueOf(voiceId));
        if (v != null && rate > 0) {
            v.rate = rate;
        }
    }

    public synchronized void setPan(int voiceId, float pan) {
        Voice v = (Voice) voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            float vol = (float) Math.sqrt(v.gainL * v.gainL + v.gainR * v.gainR);
            applyGains(v, vol, pan);
        }
    }

    private static float panOf(Voice v) {
        // invert constant power pan: angle = atan2(right, left); pan = angle/(PI/4)-1
        float angle = (float) Math.atan2(v.gainR, v.gainL);
        return angle / ((float) Math.PI / 4) - 1f;
    }

    public synchronized void pauseVoice(int voiceId) {
        Voice v = (Voice) voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            v.paused = true;
        }
    }

    public synchronized void resumeVoice(int voiceId) {
        Voice v = (Voice) voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            v.paused = false;
        }
    }

    public synchronized void stopVoice(int voiceId) {
        Voice v = (Voice) voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            v.stopped = true;
            v.sound = null; // compacted out on next mixer pass
        }
    }

    public synchronized void stopAll() {
        for (int v = 0; v < activeCount; v++) {
            active[v].stopped = true;
            active[v].sound = null;
        }
    }

    public synchronized void autoPause() {
        for (int v = 0; v < activeCount; v++) {
            active[v].paused = true;
        }
    }

    public synchronized void autoResume() {
        for (int v = 0; v < activeCount; v++) {
            active[v].paused = false;
        }
    }

    public void unloadSound(Object sound) {
        // PCM buffers are reclaimed by GC once the SoundEffect is dropped; nothing
        // platform specific to release.
    }

    public synchronized void release() {
        alive = false;
        stopAll();
        try {
            line.drain();
        } catch (Throwable t) {
        }
        line.stop();
        line.close();
    }
}
