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
package android.os;

/// A message queue owner. There is exactly one looper, the main one, and it is
/// Codename One's event dispatch thread; `Looper.prepare()` on another thread
/// is accepted so `HandlerThread`-style code runs, but its handlers post to
/// that thread's own simple queue.
public final class Looper {

    private static final Looper MAIN = new Looper(true);
    private static final ThreadLocal<Looper> LOCAL = new ThreadLocal<Looper>();

    private final boolean main;
    final java.util.ArrayList<Message> queue = new java.util.ArrayList<Message>();
    private boolean quitting;
    private Thread thread;

    private Looper(boolean main) {
        this.main = main;
    }

    public static Looper getMainLooper() {
        return MAIN;
    }

    public static Looper myLooper() {
        if (com.codename1.ui.Display.getInstance().isEdt()) {
            return MAIN;
        }
        return LOCAL.get();
    }

    public static void prepare() {
        if (LOCAL.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        Looper l = new Looper(false);
        l.thread = Thread.currentThread();
        LOCAL.set(l);
    }

    public static void prepareMainLooper() {
    }

    /// Runs this thread's queue until [#quit()]. Background loopers only; the
    /// main looper is the event dispatch thread and never needs this.
    public static void loop() {
        Looper l = myLooper();
        if (l == null || l.main) {
            return;
        }
        while (true) {
            Message next = null;
            long wait = 0;
            synchronized (l.queue) {
                if (l.quitting) {
                    return;
                }
                long now = SystemClock.uptimeMillis();
                for (int i = 0; i < l.queue.size(); i++) {
                    Message m = l.queue.get(i);
                    if (m.when <= now && (next == null || m.when < next.when)) {
                        next = m;
                    }
                }
                if (next != null) {
                    l.queue.remove(next);
                } else {
                    wait = 50;
                    for (Message m : l.queue) {
                        wait = Math.min(wait, Math.max(1, m.when - now));
                    }
                    try {
                        l.queue.wait(wait);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            if (next != null) {
                next.target.dispatchMessage(next);
            }
        }
    }

    public void quit() {
        synchronized (queue) {
            quitting = true;
            queue.clear();
            queue.notifyAll();
        }
    }

    public void quitSafely() {
        quit();
    }

    public boolean isCurrentThread() {
        return main ? com.codename1.ui.Display.getInstance().isEdt() : Thread.currentThread() == thread;
    }

    public Thread getThread() {
        return thread;
    }

    public MessageQueue getQueue() {
        return new MessageQueue();
    }

    boolean isMain() {
        return main;
    }
}
