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

/**
 * An easy API for working with threads similar to call serially/and wait that allows us to 
 * create a thread and dispatch tasks to it.
 *
 * @author Shai Almog
 */
public class EasyThread {
    private Thread t;
    private boolean running;
    private ArrayList<Object> queue = new ArrayList<Object>();
    private static final Object LOCK = new Object();
    private EasyThread(String name) {
        t = Display.getInstance().startThread(new Runnable() {
            public void run() {
                Object current = null;
                Object resultCallback = null;
                while(running) {
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
                }
            }
        }, name);
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
                    r.run();
                    synchronized(flag) {
                        flag[0] = true;
                        flag.notify();
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
}
