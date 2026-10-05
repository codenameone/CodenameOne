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

/// The queue behind a looper; exposed only for `addIdleHandler`.
///
/// Idle handlers run on the looper that owns the queue. The main queue runs
/// them on Codename One's event dispatch thread when it goes idle. A
/// background looper's queue runs them on its own thread, from
/// [Looper#loop()], each time it runs out of due messages; there the list
/// is confined to that thread, so a registration made from another thread
/// is handed over as a message rather than shared.
public final class MessageQueue {

    public interface IdleHandler {
        boolean queueIdle();
    }

    private final Looper looper;

    /// The handlers registered and not yet removed. An idle run checks it
    /// first, so a removed handler is neither invoked again nor rescheduled.
    private final java.util.ArrayList<IdleHandler> idleHandlers = new java.util.ArrayList<IdleHandler>();

    MessageQueue(Looper looper) {
        this.looper = looper;
    }

    public void addIdleHandler(final IdleHandler handler) {
        if (handler == null) {
            throw new NullPointerException("Can't add a null IdleHandler");
        }
        if (looper.isMain()) {
            idleHandlers.add(handler);
            scheduleIdle(handler);
            return;
        }
        if (looper.isCurrentThread()) {
            idleHandlers.add(handler);
            return;
        }
        new Handler(looper).post(new Runnable() {
            @Override
            public void run() {
                idleHandlers.add(handler);
            }
        });
    }

    private void scheduleIdle(final IdleHandler handler) {
        com.codename1.ui.CN.callSeriallyOnIdle(new Runnable() {
            @Override
            public void run() {
                if (!idleHandlers.contains(handler)) {
                    return;
                }
                if (handler.queueIdle()) {
                    scheduleIdle(handler);
                } else {
                    idleHandlers.remove(handler);
                }
            }
        });
    }

    /// Runs a background looper's idle handlers once, on its own thread.
    /// Called by [Looper#loop()] when no message is due.
    void runIdleHandlers() {
        if (idleHandlers.isEmpty()) {
            return;
        }
        IdleHandler[] snapshot = idleHandlers.toArray(new IdleHandler[idleHandlers.size()]);
        for (IdleHandler h : snapshot) {
            if (idleHandlers.contains(h) && !h.queueIdle()) {
                idleHandlers.remove(h);
            }
        }
    }

    public void removeIdleHandler(final IdleHandler handler) {
        if (looper.isMain() || looper.isCurrentThread()) {
            idleHandlers.remove(handler);
            return;
        }
        new Handler(looper).post(new Runnable() {
            @Override
            public void run() {
                idleHandlers.remove(handler);
            }
        });
    }

    /// True when no message is due now. A background looper answers from
    /// its queue, as Android does: a message posted for later does not make
    /// it busy. The main looper always answers true: its messages are
    /// Codename One EDT callbacks, held per handler and interleaved with the
    /// EDT's own serial calls and input, so there is no single queue here to
    /// ask, and idle work belongs in [#addIdleHandler(IdleHandler)], which
    /// does wait for the EDT to go idle.
    public boolean isIdle() {
        return looper.isMain() || !looper.hasDueMessage();
    }
}
