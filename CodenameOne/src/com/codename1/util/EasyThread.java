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

package com.codename1.util;

import com.codename1.io.Util;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * An easy API for working with threads similar to call serially/and wait that allows us to
 * create a thread and dispatch tasks to it.
 *
 * @author Shai Almog
 */
public final class EasyThread {
    private static final List<ErrorListener> globalErrorListenenrs = new ArrayList<ErrorListener>();
    private final Object LOCK = new Object();
    private final Thread t;
    private final ArrayList<Object> queue = new ArrayList<Object>();
    private List<ErrorListener> errorListenenrs;
    private boolean running = true;

    private EasyThread(String name) {
        t = Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                Object current = null;
                Object resultCallback = null;
                while (running) {
                    try {
                        synchronized (LOCK) {
                            if (queue.size() > 0) {
                                current = queue.get(0);
                                if (current instanceof RunnableWithResult) {
                                    resultCallback = queue.get(1);
                                    queue.remove(0);
                                }
                                queue.remove(0);
                            } else {
                                Util.wait(LOCK);
                            }
                        }
                        if (current != null) {
                            if (current instanceof Runnable) {
                                ((Runnable) current).run();
                            } else {
                                ((RunnableWithResult) current).run((SuccessCallback) resultCallback);
                            }
                        }
                    } catch (Throwable t) {
                        fireEvent(errorListenenrs, current, t);
                        fireEvent(globalErrorListenenrs, current, t);
                    }
                    current = null;
                    resultCallback = null;
                }
            }
        }, name);
        t.start();
    }

    /**
     * Starts a new thread
     *
     * @param name the display name for the thread
     * @return a new thread instance
     */
    public static EasyThread start(String name) {
        return new EasyThread(name);
    }

    /**
     * Adds a callback for error events, notice that this code isn't thread
     * safe and should be invoked synchronously. This method must never be
     * invoked from within the resulting callback code!
     *
     * @param err the error callback
     */
    public static void addGlobalErrorListener(ErrorListener err) {
        synchronized (globalErrorListenenrs) {
            globalErrorListenenrs.add(err);
        }
    }

    /**
     * Removes a callback for error events, notice that this code isn't thread
     * safe and should be invoked synchronously. This method must never be
     * invoked from within the resulting callback code!
     *
     * @param err the error callback
     */
    public static void removeGlobalErrorListener(ErrorListener err) {
        synchronized (globalErrorListenenrs) {
            globalErrorListenenrs.remove(err);
        }
    }

    private void fireEvent(List<ErrorListener> lst, Object current, Throwable t) {
        if (lst != null) {
            List<ErrorListener> snapshot;
            synchronized (lst) {
                snapshot = new ArrayList<ErrorListener>(lst);
            }
            for (ErrorListener e : snapshot) {
                e.onError(this, current, t);
            }
        }
    }

    /**
     * Runs the given object asynchronously on the thread and returns the result object
     *
     * @param r runs this method
     * @param t object is passed to the success callback
     */
    public <T> void run(RunnableWithResult<T> r, SuccessCallback<T> t) {
        synchronized (LOCK) {
            queue.add(r);
            queue.add(t);
            LOCK.notifyAll();
        }
    }

    /**
     * Runs the given runnable on the thread, the method returns immediately
     *
     * @param r the runnable
     */
    public void run(Runnable r) {
        synchronized (LOCK) {
            queue.add(r);
            LOCK.notifyAll();
        }
    }

    /**
     * Runs the given runnable on the thread and blocks until it completes, returns the value object
     *
     * @param r the runnable with result that will execute on the thread
     * @return value returned by r
     */
    public <T> T run(final RunnableWithResultSync<T> r) {
        // we need the flag and can't use the result object. Since null would be a valid value for the result,
        // we would have a hard time of detecting the case of code completing before the wait call
        final boolean[] flag = new boolean[1];
        final Object[] result = new Object[1];
        final SuccessCallback<T> sc = new RunSuccessCallback<T>(flag, result);
        RunnableWithResult<T> rr = new RunCallbackRunnableWithResult<T>(sc, r);
        synchronized (LOCK) {
            queue.add(rr);
            queue.add(sc);
            LOCK.notifyAll();
        }
        Display.getInstance().invokeAndBlock(new RunInvokeAndBlockRunnable(flag));
        return (T) result[0];
    }

    /**
     * Invokes the given runnable on the thread and waits for its execution to complete
     *
     * @param r the runnable
     */
    public void runAndWait(final Runnable r) {
        final boolean[] flag = new boolean[1];
        synchronized (LOCK) {
            queue.add(new InQueueRunnable(r, flag));
            LOCK.notifyAll();
        }
        Display.getInstance().invokeAndBlock(new RunAndWaitRunnable(flag));
    }

    /**
     * Stops the thread once the current task completes
     */
    public void kill() {
        synchronized (LOCK) {
            running = false;
            LOCK.notifyAll();
        }
    }

    /**
     * Returns true if the current thread is the easy thread and false othewise similar
     * to the isEDT method
     *
     * @return true if we are currently within this easy thread
     */
    public boolean isThisIt() {
        return t == Thread.currentThread(); //NOPMD CompareObjectsWithEquals
    }

    /**
     * Adds a callback for error events, notice that this code isn't thread
     * safe and should be invoked synchronously. This method must never be
     * invoked from within the resulting callback code!
     *
     * @param err the error callback
     */
    public void addErrorListener(ErrorListener err) {
        if (errorListenenrs == null) {
            errorListenenrs = new ArrayList<ErrorListener>();
        }
        errorListenenrs.add(err);
    }

    /**
     * Removes a callback for error events, notice that this code isn't thread
     * safe and should be invoked synchronously. This method must never be
     * invoked from within the resulting callback code!
     *
     * @param err the error callback
     */
    public void removeErrorListener(ErrorListener err) {
        if (errorListenenrs == null) {
            return;
        }
        List<ErrorListener> l = new ArrayList<ErrorListener>();
        l.addAll(errorListenenrs);
        l.remove(err);
        errorListenenrs = l;
    }

    /**
     * Changes the priority of this EasyThread.
     *
     * @param newPriority priority to set this thread to
     */
    public final void setPriority(int newPriority) {
        t.setPriority(newPriority);
    }

    /**
     * Callback listener for errors on easy thread
     */
    public interface ErrorListener<T> {
        /**
         * Invoked when an exception is thrown on an easy thread. Notice
         * this callback occurs within the thread and not on the EDT. This
         * method blocks the current easy thread until it completes.
         *
         * @param t        the thread
         * @param callback the callback that triggered the exception
         * @param error    the exception that occurred
         */
        void onError(EasyThread t, T callback, Throwable error);
    }

    private static class RunAndWaitRunnable implements Runnable {
        private final boolean[] flag;

        public RunAndWaitRunnable(boolean[] flag) {
            this.flag = flag;
        }

        @Override
        public void run() {
            synchronized (flag) {
                if (!flag[0]) {
                    Util.wait(flag);
                }
            }
        }
    }

    private static class InQueueRunnable implements Runnable {
        private final Runnable r;
        private final boolean[] flag;

        public InQueueRunnable(Runnable r, boolean[] flag) {
            this.r = r;
            this.flag = flag;
        }

        @Override
        public void run() {
            try {
                r.run();
            } finally {
                synchronized (flag) {
                    flag[0] = true;
                    flag.notifyAll();
                }
            }
        }
    }

    private static class RunInvokeAndBlockRunnable implements Runnable {
        private final boolean[] flag;

        public RunInvokeAndBlockRunnable(boolean[] flag) {
            this.flag = flag;
        }

        @Override
        public void run() {
            synchronized (flag) {
                if (!flag[0]) {
                    Util.wait(flag);
                }
            }
        }
    }

    private static class RunCallbackRunnableWithResult<T> implements RunnableWithResult<T> {
        private final SuccessCallback<T> sc;
        private final RunnableWithResultSync<T> r;

        public RunCallbackRunnableWithResult(SuccessCallback<T> sc, RunnableWithResultSync<T> r) {
            this.sc = sc;
            this.r = r;
        }

        @Override
        public void run(SuccessCallback<T> onSuccess) {
            sc.onSucess(r.run());
        }
    }

    private static class RunSuccessCallback<T> implements SuccessCallback<T> {
        private final boolean[] flag;
        private final Object[] result;

        public RunSuccessCallback(boolean[] flag, Object[] result) {
            this.flag = flag;
            this.result = result;
        }

        @Override
        public void onSucess(T value) {
            synchronized (flag) {
                result[0] = value;
                flag[0] = true;
                flag.notifyAll();
            }
        }
    }
}
