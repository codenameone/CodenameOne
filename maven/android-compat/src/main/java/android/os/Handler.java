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

import com.codename1.ui.CN;

import java.util.ArrayList;

/// Posts runnables and messages to a looper's thread. For the main looper
/// that is Codename One's event dispatch thread.
///
/// A handler is a thread boundary by definition -- worker threads post to the
/// UI through it -- so its pending list is the one place in this runtime that
/// synchronizes. Execution itself always happens on the target thread.
public class Handler {

    public interface Callback {
        boolean handleMessage(Message msg);
    }

    private final Looper looper;
    private final Callback callback;
    /// Messages posted to the main looper and not yet run, so they can be removed.
    private final ArrayList<Message> pending = new ArrayList<Message>();

    public Handler() {
        this(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper(), null);
    }

    public Handler(Callback callback) {
        this(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper(), callback);
    }

    public Handler(Looper looper) {
        this(looper, null);
    }

    public Handler(Looper looper, Callback callback) {
        this.looper = looper;
        this.callback = callback;
    }

    public static Handler createAsync(Looper looper) {
        return new Handler(looper);
    }

    public final Looper getLooper() {
        return looper;
    }

    public void handleMessage(Message msg) {
    }

    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            msg.callback.run();
        } else if (callback == null || !callback.handleMessage(msg)) {
            handleMessage(msg);
        }
    }

    public final boolean post(Runnable r) {
        return sendMessageDelayed(getPostMessage(r, null), 0);
    }

    public final boolean postDelayed(Runnable r, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r, null), delayMillis);
    }

    public final boolean postDelayed(Runnable r, Object token, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r, token), delayMillis);
    }

    public final boolean postAtTime(Runnable r, long uptimeMillis) {
        return sendMessageAtTime(getPostMessage(r, null), uptimeMillis);
    }

    public final boolean postAtTime(Runnable r, Object token, long uptimeMillis) {
        return sendMessageAtTime(getPostMessage(r, token), uptimeMillis);
    }

    public final boolean postAtFrontOfQueue(Runnable r) {
        return enqueue(getPostMessage(r, null), 0, true);
    }

    private Message getPostMessage(Runnable r, Object token) {
        Message m = Message.obtain(this, r);
        m.token = token;
        return m;
    }

    public final Message obtainMessage() {
        return Message.obtain(this);
    }

    public final Message obtainMessage(int what) {
        return Message.obtain(this, what);
    }

    public final Message obtainMessage(int what, Object obj) {
        return Message.obtain(this, what, obj);
    }

    public final Message obtainMessage(int what, int arg1, int arg2) {
        return Message.obtain(this, what, arg1, arg2);
    }

    public final Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        return Message.obtain(this, what, arg1, arg2, obj);
    }

    public final boolean sendMessage(Message msg) {
        return sendMessageDelayed(msg, 0);
    }

    public final boolean sendEmptyMessage(int what) {
        return sendMessageDelayed(obtainMessage(what), 0);
    }

    public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        return sendMessageDelayed(obtainMessage(what), delayMillis);
    }

    public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
        return sendMessageAtTime(obtainMessage(what), uptimeMillis);
    }

    public final boolean sendMessageDelayed(Message msg, long delayMillis) {
        return sendMessageAtTime(msg, SystemClock.uptimeMillis() + Math.max(0, delayMillis));
    }

    public final boolean sendMessageAtFrontOfQueue(Message msg) {
        return enqueue(msg, 0, true);
    }

    public boolean sendMessageAtTime(final Message msg, long uptimeMillis) {
        return enqueue(msg, uptimeMillis, false);
    }

    /// Queues `msg` for `uptimeMillis`. A front-of-queue message is due at
    /// time 0 and goes ahead of everything else this handler has due, as on
    /// Android; the latest one posted runs first.
    ///
    /// On the main looper every message gets its own EDT callback, but a
    /// callback runs the earliest message due rather than the one it was
    /// scheduled for, so a message that jumped the queue runs first. That
    /// orders this handler's work only: other handlers' callbacks keep their
    /// place in the EDT queue. Every callback either runs its own message or
    /// leaves it to the callback of the message it ran instead, so nothing
    /// is lost or run twice.
    private boolean enqueue(final Message msg, long uptimeMillis, boolean front) {
        msg.target = this;
        msg.when = uptimeMillis;
        if (!looper.isMain()) {
            synchronized (looper.queue) {
                if (looper.isQuittingLocked()) {
                    // The loop has stopped (or is draining for quitSafely)
                    // and will never deliver it; Android refuses the message.
                    return false;
                }
                if (front) {
                    looper.queue.add(0, msg);
                } else {
                    looper.queue.add(msg);
                }
                looper.queue.notifyAll();
            }
            return true;
        }
        synchronized (pending) {
            if (front) {
                pending.add(0, msg);
            } else {
                pending.add(msg);
            }
        }
        Runnable run = new Runnable() {
            @Override
            public void run() {
                Message next;
                synchronized (pending) {
                    next = nextDue(msg);
                    if (next == null) {
                        return;
                    }
                    pending.remove(next);
                }
                dispatchMessage(next);
            }
        };
        long delay = uptimeMillis - SystemClock.uptimeMillis();
        if (delay <= 0) {
            CN.callSerially(run);
        } else {
            CN.setTimeout((int) Math.min(Integer.MAX_VALUE, delay), run);
        }
        return true;
    }

    /// The pending message to run now: the earliest due, first posted on a
    /// tie. `own` -- the message the calling callback was scheduled for --
    /// counts as due even if its timer fired a little early, so a callback
    /// never drops the message it exists for. Called holding `pending`.
    private Message nextDue(Message own) {
        long limit = SystemClock.uptimeMillis();
        if (!own.canceled && pending.contains(own) && own.when > limit) {
            limit = own.when;
        }
        Message next = null;
        for (int i = 0, n = pending.size(); i < n; i++) {
            Message m = pending.get(i);
            if (m.when <= limit && (next == null || m.when < next.when)) {
                next = m;
            }
        }
        return next;
    }

    public final void removeCallbacks(Runnable r) {
        removeMatching(r, null, -1, false, true);
    }

    public final void removeCallbacks(Runnable r, Object token) {
        removeMatching(r, token, -1, true, true);
    }

    public final void removeMessages(int what) {
        removeMatching(null, null, what, false, false);
    }

    public final void removeMessages(int what, Object object) {
        removeMatching(null, object, what, true, false);
    }

    public final void removeCallbacksAndMessages(Object token) {
        if (!looper.isMain()) {
            synchronized (looper.queue) {
                for (int i = looper.queue.size() - 1; i >= 0; i--) {
                    Message m = looper.queue.get(i);
                    if (m.target == this && (token == null || m.token == token || m.obj == token)) {
                        looper.queue.remove(i);
                    }
                }
            }
            return;
        }
        synchronized (pending) {
            for (int i = pending.size() - 1; i >= 0; i--) {
                Message m = pending.get(i);
                if (token == null || m.token == token || m.obj == token) {
                    m.canceled = true;
                    pending.remove(i);
                }
            }
        }
    }

    private void removeMatching(Runnable r, Object token, int what, boolean matchToken, boolean callbacks) {
        ArrayList<Message> list = looper.isMain() ? pending : looper.queue;
        synchronized (list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Message m = list.get(i);
                if (m.target != this) {
                    continue;
                }
                boolean match = callbacks ? m.callback == r : (m.callback == null && m.what == what);
                if (match && matchToken && token != null) {
                    match = m.token == token || m.obj == token;
                }
                if (match) {
                    m.canceled = true;
                    list.remove(i);
                }
            }
        }
    }

    public final boolean hasMessages(int what) {
        ArrayList<Message> list = looper.isMain() ? pending : looper.queue;
        synchronized (list) {
            for (Message m : list) {
                if (m.target == this && m.callback == null && m.what == what) {
                    return true;
                }
            }
        }
        return false;
    }

    public final boolean hasMessages(int what, Object object) {
        ArrayList<Message> list = looper.isMain() ? pending : looper.queue;
        synchronized (list) {
            for (Message m : list) {
                if (m.target == this && m.callback == null && m.what == what && (object == null || m.obj == object)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final boolean hasCallbacks(Runnable r) {
        ArrayList<Message> list = looper.isMain() ? pending : looper.queue;
        synchronized (list) {
            for (Message m : list) {
                if (m.target == this && m.callback == r) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Handler (" + getClass().getName() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}
