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

import java.util.HashMap;

/**
 * A thread is a thread of execution in a program. The Java Virtual Machine allows an application to have multiple threads of execution running concurrently.
 * Every thread has a priority. Threads with higher priority are executed in preference to threads with lower priority.
 * There are two ways to create a new thread of execution. One is to declare a class to be a subclass of Thread. This subclass should override the run method of class Thread. An instance of the subclass can then be allocated and started. For example, a thread that computes primes larger than a stated value could be written as follows:
 */
public class Thread implements java.lang.Runnable{
    /**
     * The maximum priority that a thread can have.
     * See Also:Constant Field Values
     */
    public static final int MAX_PRIORITY=10;

    /**
     * The minimum priority that a thread can have.
     * See Also:Constant Field Values
     */
    public static final int MIN_PRIORITY=1;

    /**
     * The default priority that is assigned to a thread.
     * See Also:Constant Field Values
     */
    public static final int NORM_PRIORITY=5;

    private Runnable target;
    private boolean alive;
    private String name;
    private int priority = NORM_PRIORITY;
    private long nativeThreadId;
    private static int activeThreads = 0;
    
    /**
     * Allocates a new Thread object.
     * Threads created this way must have overridden their run() method to actually do anything.
     */
    public Thread(){
        target = this;
    }

    /**
     * Allocates a new Thread object with a specific target object whose run method is called.
     * target - the object whose run method is called.
     */
    public Thread(java.lang.Runnable target){
         this.target = target;
    }

    /**
     * Allocates a new Thread object with the given target and name.
     * target - the object whose run method is called.name - the name of the new thread.
     */
    public Thread(java.lang.Runnable target, java.lang.String name){
         this.target = target;
         this.name = name;
    }

    /**
     * Allocates a new Thread object with the given name. Threads created this way must have overridden their run() method to actually do anything.
     * name - the name of the new thread.
     */
    public Thread(java.lang.String name){
         this.name = name;
         target = this;
    }

    /**
     * Returns the current number of active threads in the virtual machine.
     */
    public static int activeCount() {
        return activeThreads;
    }

    /**
     * Returns a reference to the currently executing Thread object.
     */
    public native static java.lang.Thread currentThread();

    private static native long getNativeThreadId();
    
    /**
     * Returns this thread's name. Note that in CLDC the name of the thread can only be set when creating the thread.
     */
    public final java.lang.String getName(){
        return name; 
    }

    /**
     * Returns this thread's priority.
     */
    public final int getPriority(){
        return priority; 
    }

    /**
     * Interrupts this thread. In an implementation conforming to the CLDC Specification, this operation is not required to cancel or clean up any pending I/O operations that the thread may be waiting for.
     */
    public void interrupt(){
    }

    /**
     * Tests if this thread is alive. A thread is alive if it has been started and has not yet died.
     */
    public final boolean isAlive(){
        return alive;
    }

    /**
     * Waits for this thread to die.
     */
    public final void join() throws java.lang.InterruptedException{
        // not very efficient but we don't use this method much...
        while(alive) {
            sleep(30);
        }
    }

    /**
     * Invoked from native code...
     */
    private void runImpl(long tid) {
        alive = true;
        nativeThreadId = tid;
        Long nid = new Long(nativeThreadId);
        activeThreads++;
        try {
            target.run();
        } catch(Throwable t) {
            t.printStackTrace();
        }
        activeThreads--;
        alive = false;
    }
    
    /**
     * If this thread was constructed using a separate Runnable run object, then that Runnable object's run method is called; otherwise, this method does nothing and returns.
     * Subclasses of Thread should override this method.
     */
    public void run(){
    }

    /**
     * Changes the priority of this thread.
     */
    public final void setPriority(int newPriority){
        this.priority = newPriority;
        setPriorityImpl(priority);
    }
    
    private native void setPriorityImpl(int p);

    /**
     * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of milliseconds. The thread does not lose ownership of any monitors.
     */
    public static native void sleep(long millis) throws java.lang.InterruptedException;

    /**
     * Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread.
     * The result is that two threads are running concurrently: the current thread (which returns from the call to the start method) and the other thread (which executes its run method).
     */
    public native void start();

    /**
     * Returns a string representation of this thread, including the thread's name and priority.
     */
    public java.lang.String toString(){
        return "Thread " + name; 
    }

    /**
     * Causes the currently executing thread object to temporarily pause and allow other threads to execute.
     */
    public static void yield(){
        try {
            sleep(1); 
        } catch(InterruptedException i) {}
    }

}
