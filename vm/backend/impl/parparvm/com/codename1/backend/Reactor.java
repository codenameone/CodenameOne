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
package com.codename1.backend;

import java.io.IOException;

/**
 * Readiness notification over epoll (Linux) or kqueue (macOS/BSD).
 *
 * Level-triggered: the poller hands a ready descriptor to a worker and forgets
 * about it until the worker gives it back. Edge-triggered would require draining
 * every descriptor to EAGAIN on each wake-up, which is the opposite of that.
 */
public final class Reactor {
    public static final int READ = 1;
    public static final int WRITE = 2;
    /**
     * Deliver an event for this descriptor ONCE and then disarm it, until
     * {@link #modify} re-arms it.
     *
     * This is what lets the worker threads poll the same set directly rather
     * than a reactor thread dispatching to them: the kernel guarantees exactly
     * one waiter is handed a given descriptor, so two workers cannot land on one
     * connection. Without it a level-triggered set reports the same descriptor
     * ready to every waiter at once.
     */
    public static final int ONESHOT = 4;

    private int poller;

    private Reactor(int poller) {
        this.poller = poller;
    }

    public static Reactor create() throws IOException {
        int p = createImpl();
        if(p < 0) {
            throw new IOException("No readiness poller on this platform "
                    + "(epoll and kqueue are both unavailable)");
        }
        return new Reactor(p);
    }

    public void add(int fd, int events) throws IOException {
        if(registerImpl(poller, fd, events, false) != 0) {
            throw new IOException("Could not watch fd " + fd);
        }
    }

    public void modify(int fd, int events) throws IOException {
        if(registerImpl(poller, fd, events, true) != 0) {
            throw new IOException("Could not re-arm fd " + fd);
        }
    }

    public void remove(int fd) {
        unregisterImpl(poller, fd);
    }

    /**
     * Blocks until something is ready, then fills readyFds and returns how many.
     * A timeout below zero waits forever.
     */
    public int await(int[] readyFds, int timeoutMillis) throws IOException {
        int n = waitImpl(poller, readyFds, timeoutMillis);
        if(n < 0) {
            throw new IOException("Poller failed");
        }
        return n;
    }

    public void close() {
        if(poller >= 0) {
            int p = poller;
            poller = -1;
            ServerSocket.closeFd(p);
        }
    }

    private static native int createImpl();
    private static native int registerImpl(int poller, int fd, int events, boolean modify);
    private static native int unregisterImpl(int poller, int fd);
    private static native int waitImpl(int poller, int[] readyFds, int timeoutMillis);
}
