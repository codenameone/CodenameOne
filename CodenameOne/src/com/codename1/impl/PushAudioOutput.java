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
package com.codename1.impl;

import com.codename1.media.audio.AudioOutputPeer;
import com.codename1.media.audio.AudioRenderCallback;

import java.util.concurrent.atomic.AtomicBoolean;

/// Base for a port's real-time audio output that is fed by pushing PCM into a
/// native queue, for `CodenameOneImplementation.openAudioOutput`.
///
/// A Java thread renders blocks of [#FRAMES] frames with the
/// [AudioRenderCallback] and hands them to [#offer(float[], int, int)], which
/// copies what fits into the port's queue and never blocks; when nothing fits
/// the thread sleeps for a millisecond and tries again. The device's own audio
/// thread drains the queue. Keeping every wait on the Java side means no
/// native method ever blocks a translated thread, and the device's callback
/// never calls into Java.
public abstract class PushAudioOutput implements AudioOutputPeer {
    /// Frames rendered per block.
    protected static final int FRAMES = 256;

    private final AudioRenderCallback callback;
    private final int sampleRate;
    private final int channels;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean alive = new AtomicBoolean(true);

    /// Creates the output. Call [#startThread()] once the native side is open.
    ///
    /// #### Parameters
    ///
    /// - `sampleRate`: the device rate in Hz
    ///
    /// - `channels`: interleaved channels per frame
    ///
    /// - `callback`: what renders the audio
    protected PushAudioOutput(int sampleRate, int channels, AudioRenderCallback callback) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.callback = callback;
    }

    /// Starts the render thread.
    protected final void startThread() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "CN1AudioContext-render");
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    /// Copies up to `frames` interleaved frames from `pcm`, starting at frame
    /// `offset`, into the device queue without blocking.
    ///
    /// #### Parameters
    ///
    /// - `pcm`: interleaved samples
    ///
    /// - `offset`: first frame to copy
    ///
    /// - `frames`: frames available
    ///
    /// #### Returns
    ///
    /// the frames accepted, 0 when the queue is full
    protected abstract int offer(float[] pcm, int offset, int frames);

    /// Starts or resumes the device.
    protected abstract void nativeStart();

    /// Pauses the device, keeping what is queued.
    protected abstract void nativeStop();

    /// Releases the device; called once, on the render thread, after it stops.
    protected abstract void nativeClose();

    private void loop() {
        float[] pcm = new float[FRAMES * channels];
        while (alive.get()) {
            if (!running.get()) {
                sleep(10);
                continue;
            }
            callback.render(pcm, FRAMES, channels);
            int done = 0;
            while (done < FRAMES && alive.get()) {
                int n = offer(pcm, done, FRAMES - done);
                if (n <= 0) {
                    sleep(1);
                } else {
                    done += n;
                }
            }
        }
        nativeClose();
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start() {
        nativeStart();
        running.set(true);
    }

    @Override
    public void stop() {
        running.set(false);
        nativeStop();
    }

    @Override
    public void close() {
        running.set(false);
        alive.set(false);
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getChannels() {
        return channels;
    }
}
