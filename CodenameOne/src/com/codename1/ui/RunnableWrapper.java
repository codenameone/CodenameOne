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
import java.util.Vector;

/**
 * Class used by callSeriallyAndWait and invokeAndBlock and form to save code size
 * 
 * @author Shai Almog
 */
class RunnableWrapper implements Runnable {
    private static final Object THREADPOOL_LOCK = new Object();
    private static ArrayList<Runnable> threadPool = new ArrayList<Runnable>();

    private static int threadCount = 0;
    private static int maxThreadCount = 5;
    private static int availableThreads = 0;

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

    public RuntimeException getErr() {
        return err;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
    
    public boolean isDone() {
        return done;
    }

    public void run() {
        if(parentForm != null) {
            // set current form uses this portion to make sure all set current operations
            // occur on the EDT
            if(paint == null) {
                Display.getInstance().setCurrent(parentForm, reverse);
                return;
            }
            
            Dialog dlg = (Dialog)parentForm;
            while (!dlg.isDisposed()) {
                try {
                    synchronized(Display.lock) {
                        Display.lock.wait(40);
                    }
                } catch (InterruptedException ex) {
                }
            }
            parentForm.getStyle().setBgPainter(paint);
        } else {
            switch(type) {
                case 0: 
                    internal.run();
                    done = true;
                    synchronized(Display.lock) {
                        Display.lock.notify();
                    }
                    break;
                case 1:
                    try {
                        internal.run();
                    } catch(RuntimeException ex) {
                        this.err = ex;
                    }
                    break;
                case 2:
                    while(!done) {
                        synchronized(Display.lock) {
                            try {
                                Display.lock.wait(10);
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
                    while(!Display.getInstance().codenameOneExited) {
                        Runnable r = null;
                        synchronized(THREADPOOL_LOCK) {
                            if(threadPool.size() > 0) {
                                r = threadPool.get(0);
                                threadPool.remove(0);
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
                        if(r != null) {
                            r.run();
                        }
                    }
            }
        }
        done = true;
    }

    static void pushToThreadPool(Runnable r) {
        if(availableThreads == 0 && threadCount < maxThreadCount) {
            threadCount++;
            Thread poolThread = Display.getInstance().startThread(new RunnableWrapper(null, 4), "invokeAndBlock" + threadCount);
            poolThread.start();
        }
        synchronized(THREADPOOL_LOCK) {
            threadPool.add(r);
            THREADPOOL_LOCK.notify();
        }
    }
}
