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
public class EasyThread {
    private List<ErrorListener> errorListenenrs;
    private static List<ErrorListener> globalErrorListenenrs;
    private Thread t;
    private boolean running = true;
    private ArrayList<Object> queue = new ArrayList<Object>();
    private final Object LOCK = new Object();
    private EasyThread(String name) {
        t = Display.getInstance().startThread(new Runnable() {
            public void run() {
                Object current = null;
                Object resultCallback = null;
                while(running) {
                    try {
                        synchronized(LOCK) {
                            if(queue.size() > 0) {
                                current = queue.get(0);
                                if(current instanceof RunnableWithResult) {
                                    resultCallback = queue.get(1);
                                    queue.remove(0);
                                }
                                queue.remove(0);
                            } else {
                                Util.wait(LOCK);
                            }
                        }
                        if(current != null) {
                            if(current instanceof Runnable) {
                                ((Runnable)current).run();
                            } else {
                                ((RunnableWithResult)current).run((SuccessCallback)resultCallback);
                            }
                        }
                    } catch(Throwable t) {
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
    
    private void fireEvent(List<ErrorListener> lst, Object current, Throwable t) {
        if(lst != null) {
            for(ErrorListener e : lst) {
                e.onError(this, current, t);
            }
        }
    }
    
    /**
     * Starts a new thread
     * @param name the display name for the thread
     * @return a new thread instance
     */
    public static EasyThread start(String name) {
        return new EasyThread(name);
    }
    
    /**
     * Runs the given object asynchronously on the thread and returns the result object
     * @param r runs this method
     * @param t object is passed to the success callback
     */
    public <T> void run(RunnableWithResult<T> r, SuccessCallback<T> t) {
        synchronized(LOCK) {
            queue.add(r);
            queue.add(t);
            LOCK.notify();
        }
    }

    
    /**
     * Runs the given runnable on the thread, the method returns immediately
     * @param r the runnable
     */
    public void run(Runnable r) {
        synchronized(LOCK) {
            queue.add(r);
            LOCK.notify();
        }
    }

    /**
     * Runs the given runnable on the thread and blocks until it completes, returns the value object
     * 
     * @param r the runnable with result that will execute on the thread
     * @return value returned by r
     */
    public <T> T run(final RunnableWithResultSync<T> r) {
        // we need the flag and can't use the result object. Since null would be a valid value for the result
        // we would have a hard time of detecting the case of the code completing before the wait call
        final boolean[] flag = new boolean[1];
        final Object[] result = new Object[1];
        final SuccessCallback<T> sc = new SuccessCallback<T>() {
            public void onSucess(T value) {
                synchronized(flag) {
                    result[0] = value;
                    flag[0] = true;
                    flag.notify();
                }
            }
        };
        RunnableWithResult<T> rr = new RunnableWithResult<T>() {
            public void run(SuccessCallback<T> onSuccess) {
                sc.onSucess(r.run());
            }
        };
        synchronized(LOCK) {
            queue.add(rr);
            queue.add(sc);
            LOCK.notify();
        }
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                synchronized(flag) {
                    if(!flag[0]) {
                        Util.wait(flag);
                    }
                }
            }
        });
        return (T)result[0];
    }
    
    
    /**
     * Invokes the given runnable on the thread and waits for its execution to complete
     * @param r the runnable
     */
    public void runAndWait(final Runnable r) {
        final boolean[] flag = new boolean[1];
        synchronized(LOCK) {
            queue.add(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        synchronized (flag) {
                            flag[0] = true;
                            flag.notify();
                        }
                    }
                }
            });
            LOCK.notify();
        }
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                synchronized(flag) {
                    if(!flag[0]) {
                        Util.wait(flag);
                    }
                }
            }
        });
    }
    
    
    /**
     * Stops the thread once the current task completes
     */
    public void kill() {
        synchronized(LOCK) {
            running = false;
            LOCK.notify();
        }
    }
    
    /**
     * Returns true if the current thread is the easy thread and false othewise similar 
     * to the isEDT method
     * @return true if we are currently within this easy thread
     */
    public boolean isThisIt() {
        return t == Thread.currentThread();
    }
    
    /**
     * Adds a callback for error events, notice that this code isn't thread 
     * safe and should be invoked synchronously. This method must never be 
     * invoked from within the resulting callback code!
     * @param err the error callback
     */
    public void addErrorListener(ErrorListener err) {
        if(errorListenenrs == null) {
            errorListenenrs = new ArrayList<ErrorListener>();
        }
        errorListenenrs.add(err);
    }
    
    /**
     * Removes a callback for error events, notice that this code isn't thread 
     * safe and should be invoked synchronously. This method must never be 
     * invoked from within the resulting callback code!
     * @param err the error callback
     */
    public void removeErrorListener(ErrorListener err) {
        if(errorListenenrs == null) {
            return;
        }
        List<ErrorListener> l = new ArrayList<ErrorListener>();
        l.addAll(errorListenenrs);
        l.remove(err);
        errorListenenrs = l;
    }
    
    
    /**
     * Adds a callback for error events, notice that this code isn't thread 
     * safe and should be invoked synchronously. This method must never be 
     * invoked from within the resulting callback code!
     * @param err the error callback
     */
    public static void addGlobalErrorListener(ErrorListener err) {
        if(globalErrorListenenrs == null) {
            globalErrorListenenrs = new ArrayList<ErrorListener>();
        }
        globalErrorListenenrs.add(err);
    }
    
    /**
     * Removes a callback for error events, notice that this code isn't thread 
     * safe and should be invoked synchronously. This method must never be 
     * invoked from within the resulting callback code!
     * @param err the error callback
     */
    public static void removeGlobalErrorListener(ErrorListener err) {
        if(globalErrorListenenrs == null) {
            return;
        }
        List<ErrorListener> l = new ArrayList<ErrorListener>();
        l.addAll(globalErrorListenenrs);
        l.remove(err);
        globalErrorListenenrs = l;
    }
    
    
    /**
     * Callback listener for errors on easy thread
     */
    public static interface ErrorListener<T> {
        /**
         * Invoked when an exception is thrown on an easy thread. Notice
         * this callback occurs within the thread and not on the EDT. This
         * method blocks the current easy thread until it completes.
         * @param t the thread
         * @param callback the callback that triggered the exception
         * @param error the exception that occurred
         */
        void onError(EasyThread t, T callback, Throwable error);
    }
    
    /**
     * Changes the priority of this EasyThread.
     *
     * @param newPriority priority to set this thread to
     */
    public final void setPriority(int newPriority) {
        t.setPriority(newPriority);
    }
}
