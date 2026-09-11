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

/**
 * Turns SIGTERM into an ordinary blocking call, so a server can shut down cleanly
 * when its container asks it to.
 *
 * The handler itself does one async-signal-safe write() to a pipe, and this class
 * turns that into an ordinary blocking read. Calling into the VM from a handler --
 * allocating, taking a monitor, touching the collector -- is undefined, and
 * blocking the signals and calling sigwait() does not work either: ParparVM starts
 * its collector thread before main(), so that thread never inherits the mask and
 * dies on the default action.
 */
public final class Signals {
    private Signals() {
    }

    /**
     * Installs the shutdown handlers and ignores SIGPIPE. Safe to call more than
     * once. Writing to a socket whose peer has gone is routine for a server, and
     * SIGPIPE's default action is to kill the process; ignored, the write returns
     * an error like any other.
     */
    public static boolean installShutdownHandler() {
        return blockImpl() == 0;
    }

    /** Blocks until SIGINT or SIGTERM arrives. Returns the signal number, or -1. */
    public static int awaitShutdownSignal() {
        return awaitImpl();
    }

    /**
     * Runs body on a dedicated thread when a shutdown signal arrives.
     * blockShutdownSignals must already have been called.
     */
    public static void onShutdown(final Runnable body) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                int signo = awaitShutdownSignal();
                if(signo > 0) {
                    System.out.println("signal " + signo + " received, shutting down");
                } else {
                    // NOT a signal: the wait failed, which means the handler was
                    // never installed. Saying so matters because everything below
                    // still runs -- the server is stopped and the process exits --
                    // and without this line that is indistinguishable from an
                    // ordinary shutdown, seconds after start, with no reason given.
                    // The generated bootstrap now refuses to start when the install
                    // fails, so this covers a hand-written main that does not.
                    System.err.println("the shutdown wait failed (" + signo + "), so no "
                            + "signal was ever waited for; stopping anyway");
                }
                body.run();
                // Here rather than in body: stopping the server only unblocks the
                // reactor, and every other thread is detached, so something has to
                // end the process. Callers must NOT do this themselves -- the same
                // body runs from a JVM shutdown hook under the JavaSE
                // implementation, where exiting deadlocks.
                System.exit(0);
            }
        });
        t.start();
    }

    private static native int blockImpl();
    private static native int awaitImpl();
}
