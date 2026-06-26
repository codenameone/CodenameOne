/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.ios.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Records and replays simulator input. The window event router taps every
 * pointer/key event here on its way to the app; {@link #record} stamps each
 * with a time offset, and {@link #play} feeds them back through the same
 * {@link Dispatcher} with the original inter-event timing. Replays drive the
 * real app universe, so a recorded interaction reproduces exactly.
 */
public class InputRecorder implements com.codename1.impl.ios.sim.bridge.RecorderControl {
    /** Routes an event into the app/shell - the same path live input takes. */
    public interface Dispatcher {
        void pointer(int type, int x, int y);

        void key(int type, int code);
    }

    // each event: {timeOffsetMs, kind(0=pointer,1=key), type, a, b}
    private final List<long[]> events = new ArrayList<long[]>();
    private volatile boolean recording;
    private long startTime;
    private final Dispatcher dispatcher;

    public InputRecorder(Dispatcher d) {
        this.dispatcher = d;
    }

    @Override
    public synchronized void start() {
        events.clear();
        startTime = System.currentTimeMillis();
        recording = true;
    }

    @Override
    public synchronized void stop() {
        recording = false;
    }

    @Override
    public boolean isRecording() {
        return recording;
    }

    @Override
    public synchronized int count() {
        return events.size();
    }

    /** Captures one event while recording (no-op otherwise). kind 0=pointer 1=key. */
    public synchronized void record(int kind, int type, int a, int b) {
        if (!recording) {
            return;
        }
        events.add(new long[]{System.currentTimeMillis() - startTime, kind, type, a, b});
    }

    /** Routes one event through the dispatcher (live input and replay share this). */
    public void inject(int kind, int type, int a, int b) {
        if (kind == 0) {
            dispatcher.pointer(type, a, b);
        } else {
            dispatcher.key(type, a);
        }
    }

    @Override
    public void play() {
        final long[][] snapshot;
        synchronized (this) {
            if (recording || events.isEmpty()) {
                return;
            }
            snapshot = events.toArray(new long[0][]);
        }
        new Thread("cn1sim-replay") {
            public void run() {
                long last = 0;
                for (long[] e : snapshot) {
                    long delay = e[0] - last;
                    last = e[0];
                    if (delay > 0) {
                        try {
                            Thread.sleep(Math.min(delay, 5000));
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                    inject((int) e[1], (int) e[2], (int) e[3], (int) e[4]);
                }
            }
        }.start();
    }
}
