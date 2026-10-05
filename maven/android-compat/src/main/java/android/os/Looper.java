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
    /// The uptime quitSafely() was called at, or -1: messages due by then
    /// are still delivered, later ones are dropped.
    private long safeQuitAt = -1;
    private Thread thread;
    /// One queue per looper, so an idle handler added through one
    /// `getQueue()` call can be removed through another.
    private final MessageQueue messageQueue;

    private Looper(boolean main) {
        this.main = main;
        this.messageQueue = new MessageQueue(this);
    }

    public static Looper getMainLooper() {
        bindMainIfEdt();
        return MAIN;
    }

    public static Looper myLooper() {
        if (bindMainIfEdt()) {
            return MAIN;
        }
        return LOCAL.get();
    }

    /// Records the event dispatch thread as the main looper's thread when
    /// called on it, and answers whether it was.
    ///
    /// The main looper is created with the class, which can be before the
    /// EDT exists, so its thread is bound lazily: the runtime calls
    /// [#prepareMainLooper()] on the EDT before the first activity starts,
    /// and every main-looper lookup made on the EDT re-binds it -- Codename
    /// One can replace the EDT, and the latest one is the main thread.
    private static boolean bindMainIfEdt() {
        if (!com.codename1.ui.Display.getInstance().isEdt()) {
            return false;
        }
        MAIN.thread = Thread.currentThread();
        return true;
    }

    public static void prepare() {
        if (LOCAL.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        Looper l = new Looper(false);
        l.thread = Thread.currentThread();
        LOCAL.set(l);
    }

    /// Binds the main looper to the calling thread when that is the event
    /// dispatch thread. The runtime calls it before starting an activity, as
    /// Android's `ActivityThread.main` does, so `getMainLooper().getThread()`
    /// names the EDT even when first asked from a background thread.
    public static void prepareMainLooper() {
        bindMainIfEdt();
    }

    /// Runs this thread's queue until [#quit()]. Background loopers only; the
    /// main looper is the event dispatch thread and never needs this.
    public static void loop() {
        Looper l = myLooper();
        if (l == null || l.main) {
            return;
        }
        // True when the queue has not gone idle since the last message: idle
        // handlers run once each time the queue runs out of due work.
        boolean idleDue = true;
        while (true) {
            Message next = null;
            long wait = 0;
            boolean runIdle = false;
            synchronized (l.queue) {
                if (l.quitting) {
                    return;
                }
                long now = SystemClock.uptimeMillis();
                if (l.safeQuitAt >= 0) {
                    now = l.safeQuitAt;
                }
                for (int i = 0; i < l.queue.size(); i++) {
                    Message m = l.queue.get(i);
                    if (m.when <= now && (next == null || m.when < next.when)) {
                        next = m;
                    }
                }
                if (next != null) {
                    l.queue.remove(next);
                } else if (l.safeQuitAt >= 0) {
                    // Everything due when quitSafely() ran has been delivered.
                    l.quitting = true;
                    l.queue.clear();
                    return;
                } else if (idleDue) {
                    // Run outside the lock, then look at the queue again:
                    // an idle handler may have posted work.
                    idleDue = false;
                    runIdle = true;
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
            if (runIdle) {
                l.messageQueue.runIdleHandlers();
            } else if (next != null) {
                next.target.dispatchMessage(next);
                idleDue = true;
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
        synchronized (queue) {
            if (!quitting && safeQuitAt < 0) {
                safeQuitAt = SystemClock.uptimeMillis();
            }
            queue.notifyAll();
        }
    }

    public boolean isCurrentThread() {
        return main ? bindMainIfEdt() : Thread.currentThread() == thread;
    }

    public Thread getThread() {
        if (main) {
            bindMainIfEdt();
        }
        return thread;
    }

    public MessageQueue getQueue() {
        return messageQueue;
    }

    /// The calling thread's looper's queue.
    public static MessageQueue myQueue() {
        Looper l = myLooper();
        if (l == null) {
            throw new IllegalStateException("The current thread must have a looper!");
        }
        return l.messageQueue;
    }

    /// True once `quit()` or `quitSafely()` has been called: the queue
    /// accepts no new messages. Called holding `queue`.
    boolean isQuittingLocked() {
        return quitting || safeQuitAt >= 0;
    }

    boolean isMain() {
        return main;
    }
}
