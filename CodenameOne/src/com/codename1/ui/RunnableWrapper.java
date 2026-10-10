/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import java.util.ArrayList;

/// Class used by callSeriallyAndWait and invokeAndBlock and form to save code size
class RunnableWrapper implements Runnable {
    private static final Object THREADPOOL_LOCK = new Object();
    private static final ArrayList<Runnable> threadPool = new ArrayList<Runnable>();

    private static int threadCount = 0;
    private static int maxThreadCount = 5;
    private static int availableThreads = 0;

    /// Counts the calls to `#retireThreadPool()`. A pool thread remembers the count it
    /// started under and leaves once it is idle and the count has moved on. Read and written
    /// under `THREADPOOL_LOCK` only.
    private static int retirements = 0;

    private boolean done = false;
    private Runnable internal;
    private int type;
    private RuntimeException err;
    private Form parentForm;
    private Painter paint;
    private boolean reverse;

    public RunnableWrapper(Form parentForm, Painter paint, boolean reverse) {
        this.parentForm = parentForm;
        this.paint = paint;
        this.reverse = reverse;
    }

    public RunnableWrapper(Runnable internal, int type) {
        this.internal = internal;
        this.type = type;
    }

    static void pushToThreadPool(Runnable r) {
        // Under the lock since a retiring thread gives its place back under it: counted here
        // without it, the two could lose each other's update and leave the pool one thread
        // short for good.
        synchronized (THREADPOOL_LOCK) {
            if (availableThreads == 0 && threadCount < maxThreadCount) {
                threadCount++;
                Thread poolThread = Display.getInstance().startThread(new RunnableWrapper(null, 4), "invokeAndBlock" + threadCount);
                poolThread.start();
            }
            threadPool.add(r);
            THREADPOOL_LOCK.notifyAll();
        }
    }

    /// Lets the pool's threads end: each one leaves as soon as it has nothing to run, and
    /// the next `invokeAndBlock` starts the threads it needs.
    ///
    /// A pool thread otherwise waits for work for as long as the process lives. That is what
    /// an application wants, and it is why nothing in the framework calls this. A host that
    /// runs several generations of Codename One in one process, each in a class loader of
    /// its own -- the simulator's test runner gives every test one -- needs the opposite: a
    /// thread still waiting is a live reference to every class and every static of a
    /// generation that is over, so none of it can be collected.
    static void retireThreadPool() {
        synchronized (THREADPOOL_LOCK) {
            retirements++;
            THREADPOOL_LOCK.notifyAll();
        }
    }

    static void setMaxThreadCount(int maxThreadCount) {
        RunnableWrapper.maxThreadCount = maxThreadCount;
    }

    public RuntimeException getErr() {
        return err;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    public void run() {
        if (parentForm != null) {
            // set current form uses this portion to make sure all set current operations
            // occur on the EDT
            if (paint == null) {
                Display.getInstance().setCurrent(parentForm, reverse);
                return;
            }

            Dialog dlg = (Dialog) parentForm;
            while (!dlg.isDisposed()) {
                try {
                    synchronized (Display.lock) {
                        if (!dlg.isDisposed()) {
                            Display.lock.wait(40);
                        }
                    }
                } catch (InterruptedException ex) {
                }
            }
            parentForm.getStyle().setBgPainter(paint);
        } else {
            switch (type) {
                case 0:
                    internal.run();
                    synchronized (Display.lock) {
                        done = true;
                        Display.lock.notifyAll();
                    }
                    break;
                case 1:
                    try {
                        internal.run();
                    } catch (RuntimeException ex) {
                        this.err = ex;
                    }
                    break;
                case 2:
                    while (!done) {
                        synchronized (Display.lock) {
                            try {
                                if (!done) {
                                    Display.lock.wait(10);
                                }
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    break;
                case 3:
                    Display.getInstance().mainEDTLoop();
                    break;
                case 4:
                    int startedUnder;
                    synchronized (THREADPOOL_LOCK) {
                        startedUnder = retirements;
                    }
                    while (!Display.getInstance().codenameOneExited) {
                        Runnable r = null;
                        synchronized (THREADPOOL_LOCK) {
                            if (!threadPool.isEmpty()) {
                                r = threadPool.get(0);
                                threadPool.remove(0);
                            } else if (startedUnder != retirements) {
                                // Retired, and idle. The place is given back under the lock
                                // the next request counts under, so that request starts a
                                // thread of its own and is not left waiting for this one.
                                threadCount--;
                                break;
                            } else {
                                try {
                                    availableThreads++;
                                    THREADPOOL_LOCK.wait();
                                    availableThreads--;
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        if (r != null) {
                            r.run();
                        }
                    }
            }
        }
        done = true;
    }
}
