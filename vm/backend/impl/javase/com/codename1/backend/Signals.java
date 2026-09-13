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
 * Java SE twin of Signals.
 *
 * The JVM already turns SIGTERM and SIGINT into a shutdown hook, which runs on an
 * ordinary thread and may do anything -- so the self-pipe the translated build
 * needs has no counterpart here. SIGPIPE is likewise the JVM's problem: it sets
 * the disposition itself, and a write to a departed peer surfaces as an
 * IOException.
 */
public final class Signals {
    private Signals() {
    }

    public static boolean installShutdownHandler() {
        return true;
    }

    /**
     * Blocks forever. There is nothing to wait for here -- the hook installed by
     * onShutdown is what runs -- and returning would let a caller treat that as a
     * signal having arrived.
     */
    public static int awaitShutdownSignal() {
        Object lock = new Object();
        synchronized(lock) {
            while(true) {
                try {
                    lock.wait();
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
        }
    }

    /**
     * Runs body from a JVM shutdown hook.
     *
     * The hook RETURNS when body is done, and body must not call System.exit:
     * exiting from inside a shutdown hook blocks forever, because System.exit
     * waits for the shutdown it is already part of. The JVM ends on its own once
     * every hook has returned, so there is nothing left to do here. The ParparVM
     * implementation of this method does have to end the process, which is why
     * that belongs in these two files and not in any caller.
     */
    public static void onShutdown(final Runnable body) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("shutdown requested");
                body.run();
            }
        }));
    }
}
