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

/// A thread of control that is not an OS thread.
///
/// One of these per connection is what lets a server keep a context per client
/// without keeping an OS THREAD per client. The difference is not stylistic: a
/// handoff between OS threads measured 21181ns on the machine this was built on,
/// and switching a virtual thread measured 2.6ns.
///
/// A virtual thread runs until it finishes or until it asks for bytes that have
/// not arrived, at which point it parks and the host thread goes and runs another
/// one. Parking happens inside the ordinary blocking calls, so the code a virtual
/// thread runs is written in the plain blocking style and does not know it is not
/// a thread -- which is the reason to have them rather than callbacks.
public final class VirtualThread {
    private VirtualThread() {
    }

    /// A virtual thread that will serve `fd` when first resumed.
    ///
    /// The stack is the C stack only. Java locals and the operand stack live in
    /// the virtual thread's own VM state, which is mapped lazily, so what this
    /// size buys is call DEPTH rather than data: it holds the C activation
    /// records of the Java methods the connection is nested inside.
    ///
    /// @return a handle, or 0 if the stack could not be allocated
    public static long create(int fd, int stackBytes) {
        return createImpl(fd, stackBytes);
    }

    /// A virtual thread that runs a background task: when first resumed it calls
    /// `Tasks.runVirtual(token)`. It has no descriptor --
    /// [#descriptorOf] answers -1 -- so its host runs it from the ring and
    /// never parks it on the poller.
    ///
    /// #### Returns
    ///
    /// a handle, or 0 if the stack could not be allocated
    public static long createTask(long token, int stackBytes) {
        return createTaskImpl(token, stackBytes);
    }

    /// [#resume]: the connection is done and the handle should be freed.
    public static final int FINISHED = 0;
    /// [#resume]: waiting for bytes; its descriptor goes back to the poller.
    public static final int PARKED_IO = 1;
    /// [#resume]: it gave up its turn but is ready to run again NOW.
    ///
    /// It is waiting on something that is not its socket -- the collector's
    /// allocation backpressure, or its own fairness yield. Putting it on the
    /// poller instead would wait for a client that is waiting for the response
    /// this virtual thread owes it, and the connection would hang for ever.
    public static final int RUNNABLE = 2;
    /// [#resume]: parked on an OUTBOUND descriptor -- a database socket, a TLS
    /// peer, an HTTP call -- rather than on its own. The host registers the
    /// descriptors [#waitCount] names with its poller and resumes it when one is
    /// ready or [#waitTimeout] runs out, running other virtual threads meanwhile.
    public static final int WAITING = 3;

    /// Run it until it parks, yields or finishes. One of the four constants.
    public static int resume(long handle) {
        return resumeImpl(handle);
    }

    /// How many descriptors a virtual thread that answered [#WAITING] waits on;
    /// 0 for none, which is a wait on the timeout alone.
    public static int waitCount(long handle) {
        return waitCountImpl(handle);
    }

    /// The `index`-th descriptor a [#WAITING] virtual thread waits on.
    public static int waitDescriptor(long handle, int index) {
        return waitDescriptorImpl(handle, index);
    }

    /// What it waits for on that descriptor: [Reactor#READ], [Reactor#WRITE] or both.
    public static int waitEvents(long handle, int index) {
        return waitEventsImpl(handle, index);
    }

    /// How long it is willing to wait, in milliseconds from now, or -1 for no bound.
    public static long waitTimeout(long handle) {
        return waitTimeoutImpl(handle);
    }

    /// The descriptor this virtual thread serves, or -1.
    public static int descriptorOf(long handle) {
        return descriptorImpl(handle);
    }

    /// Release it. Only valid once [#resume] has returned FINISHED.
    public static void free(long handle) {
        freeImpl(handle);
    }

    /// Step aside so the host thread can run another virtual thread, without
    /// waiting for anything.
    ///
    /// Parking happens by itself when bytes have not arrived. This is for the
    /// other case: a virtual thread that COULD keep going but has had its turn.
    /// A no-op when the caller is not a virtual thread.
    public static void yieldNow() {
        yieldImpl();
    }

    /// The handle of the virtual thread the caller runs on, or 0 on a host or
    /// platform thread. What a waiter hands its host so it can nap rather than
    /// be resumed again at once.
    public static long current() {
        return currentImpl();
    }

    /// Whether the caller is running on a virtual thread rather than a host thread.
    public static boolean isVirtual() {
        return isVirtualImpl();
    }

    /// Whether this build has virtual threads at all, which is not the same
    /// question as isVirtual(). The context switch is compiled in only on
    /// non-Windows aarch64/x86_64; elsewhere create() can only ever return 0.
    /// The server asks this to pick its default poll mode.
    public static boolean supported() {
        return supportedImpl();
    }

    private static native long createImpl(int fd, int stackBytes);
    private static native long createTaskImpl(long token, int stackBytes);
    private static native int resumeImpl(long handle);
    private static native int waitCountImpl(long handle);
    private static native int waitDescriptorImpl(long handle, int index);
    private static native int waitEventsImpl(long handle, int index);
    private static native long waitTimeoutImpl(long handle);
    private static native int descriptorImpl(long handle);
    private static native void freeImpl(long handle);
    private static native boolean isVirtualImpl();
    private static native long currentImpl();
    private static native boolean supportedImpl();
    private static native void yieldImpl();
    private static native void reportImpl();

    /// Print created/finished/freed counts to stderr, for diagnosis.
    public static void report() {
        reportImpl();
    }
}
