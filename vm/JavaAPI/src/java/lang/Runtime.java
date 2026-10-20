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

package java.lang;


import java.util.ArrayList;

/**
 * Every Java application has a single instance of class Runtime that allows the application to interface with the environment in which the application is running. The current runtime can be obtained from the getRuntime method.
 * An application cannot create its own instance of this class.
 * Since: JDK1.0, CLDC 1.0 See Also:getRuntime()
 */
public class Runtime {
    private static final Runtime instance = new Runtime();

    /** The registered shutdown hooks; also the lock for them and for {@link #shuttingDown}. */
    private final ArrayList<Thread> hooks = new ArrayList<Thread>();

    /** Whether exit has begun, after which hooks are neither run again nor accepted. */
    private boolean shuttingDown;

    private Runtime() {
    }

    /**
     * Terminates the currently running Java application. This method never returns normally.
     * The argument serves as a status code; by convention, a nonzero status code indicates abnormal termination.
     *
     * The registered shutdown hooks run first, one after another on this
     * thread, each in its own try so one that throws does not stop the rest.
     * Then the process ends with the status.
     *
     * A second call, including one the platform's own close makes, skips
     * all of that and ends the process at once.
     */
    public void exit(int status){
        // An array, not a list: the element is typed without a cast, so no
        // checkcast sits inside the try below (ParparVM does not check one).
        Thread[] run = null;
        synchronized (hooks) {
            if (!shuttingDown) {
                shuttingDown = true;
                run = hooks.toArray(new Thread[hooks.size()]);
                hooks.clear();
            }
        }
        if (run != null) {
            // In order, on this thread, never started: Codename One is not the
            // JDK. Exit is called from the EDT on every port, a platform may
            // end the process the moment it returns, and starting threads
            // there is what would leave a hook half run. Hooks that wait on
            // one another (one awaiting a latch the next counts down) are
            // therefore not supported; each hook must finish on its own.
            for (int i = 0; i < run.length; i++) {
                Thread hook = run[i];
                try {
                    hook.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        exitImpl(status);
    }

    /**
     * Ends the process at once, without running the shutdown hooks or asking
     * the platform to close the application.
     */
    public void halt(int status) {
        synchronized (hooks) {
            shuttingDown = true;
            hooks.clear();
        }
        exitImpl(status);
    }

    private static native void exitImpl(int status);

    /**
     * Registers a hook that exit runs before the application closes. The
     * hook's run method is called on the exiting thread; the thread is never
     * started.
     */
    public void addShutdownHook(Thread hook) {
        if (hook == null) {
            throw new NullPointerException();
        }
        synchronized (hooks) {
            if (shuttingDown) {
                throw new IllegalStateException("Shutdown in progress");
            }
            // Only a running hook is refused: JDK 8 accepts one that has
            // already terminated too (its isAlive() is false as well).
            if (hook.isAlive()) {
                throw new IllegalArgumentException("Hook already running");
            }
            if (hooks.contains(hook)) {
                throw new IllegalArgumentException("Hook previously registered");
            }
            hooks.add(hook);
        }
    }

    /**
     * Unregisters a hook; whether it was registered.
     */
    public boolean removeShutdownHook(Thread hook) {
        if (hook == null) {
            throw new NullPointerException();
        }
        synchronized (hooks) {
            if (shuttingDown) {
                throw new IllegalStateException("Shutdown in progress");
            }
            return hooks.remove(hook);
        }
    }

    /**
     * The number of processors available to the program. The class library
     * cannot ask the platform, so it answers 1, the one answer that is
     * never too many for a program sizing a thread pool by it.
     */
    public int availableProcessors() {
        return 1;
    }

    /**
     * The most memory the program may use; the runtime does not expose a
     * limit, so this is the total memory, as totalMemory answers it.
     */
    public long maxMemory() {
        return totalMemory();
    }

    /**
     * Returns the amount of free memory in the system. Calling the gc method may result in increasing the value returned by freeMemory.
     */
    public long freeMemory(){
        return freeMemoryImpl();
    }

    /**
     * Runs the garbage collector. Calling this method suggests that the Java Virtual Machine expend effort toward recycling unused objects in order to make the memory they currently occupy available for quick reuse. When control returns from the method call, the Java Virtual Machine has made its best effort to recycle all discarded objects.
     * The name gc stands for "garbage collector". The Java Virtual Machine performs this recycling process automatically as needed even if the gc method is not invoked explicitly.
     * The method System.gc() is the conventional and convenient means of invoking this method.
     */
    public void gc(){
        System.gc();
    }

    /**
     * Returns the runtime object associated with the current Java application. Most of the methods of class Runtime are instance methods and must be invoked with respect to the current runtime object.
     */
    public static java.lang.Runtime getRuntime(){
        return instance;
    }

    /**
     * Returns the total amount of memory in the Java Virtual Machine. The value returned by this method may vary over time, depending on the host environment.
     * Note that the amount of memory required to hold an object of any given type may be implementation-dependent.
     */
    public long totalMemory(){
        return totalMemoryImpl();
    }

    private static native long totalMemoryImpl();
    private static native long freeMemoryImpl();
}
