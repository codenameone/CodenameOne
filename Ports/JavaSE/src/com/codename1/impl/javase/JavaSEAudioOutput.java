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

import com.codename1.media.audio.AudioOutputPeer;
import com.codename1.media.audio.AudioRenderCallback;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/// Real-time output for `com.codename1.media.audio.AudioContext` on the desktop
/// and in the simulator: a `SourceDataLine` fed by a render thread. Writing to
/// the line blocks once its buffer is full, which is what paces rendering.
class JavaSEAudioOutput implements AudioOutputPeer {
    private static final int FRAMES = 256;
    private final SourceDataLine line;
    private final int sampleRate;
    private final int channels;
    private final AudioRenderCallback callback;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final int bufferFrames;

    JavaSEAudioOutput(int requestedRate, int channels, AudioRenderCallback callback) throws Exception {
        this.sampleRate = requestedRate > 0 ? requestedRate : 48000;
        this.channels = channels;
        this.callback = callback;
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
        line.open(format, FRAMES * channels * 2 * 8);
        bufferFrames = line.getBufferSize() / (channels * 2);
        Thread t = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "CN1AudioContext-render");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    static boolean isAvailable() {
        try {
            AudioFormat format = new AudioFormat(48000, 16, 2, true, false);
            return AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format));
        } catch (Throwable t) {
            return false;
        }
    }

    private void loop() {
        float[] pcm = new float[FRAMES * channels];
        byte[] bytes = new byte[FRAMES * channels * 2];
        // The line is started and stopped here rather than by start()/stop():
        // a write blocked on a stopped line would never return, and close()
        // could not reach line.close().
        boolean lineOn = false;
        while (alive.get()) {
            if (!running.get()) {
                if (lineOn) {
                    line.stop();
                    lineOn = false;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
                continue;
            }
            if (!lineOn) {
                line.start();
                lineOn = true;
            }
            callback.render(pcm, FRAMES, channels);
            for (int i = 0, b = 0; i < pcm.length; i++) {
                float v = pcm[i];
                if (v > 1f) {
                    v = 1f;
                } else if (v < -1f) {
                    v = -1f;
                }
                int s = (int) (v * 32767f);
                bytes[b++] = (byte) s;
                bytes[b++] = (byte) (s >> 8);
            }
            line.write(bytes, 0, bytes.length);
        }
        line.close();
    }

    public void start() {
        running.set(true);
    }

    public void stop() {
        running.set(false);
    }

    public void close() {
        running.set(false);
        alive.set(false);
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public double getOutputLatency() {
        return bufferFrames / (double) sampleRate;
    }
}
