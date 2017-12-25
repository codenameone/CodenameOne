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
package java.lang;
/**
 * A thread is a thread of execution in a program. The Java Virtual Machine allows an application to have multiple threads of execution running concurrently.
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

    /**
     * Allocates a new Thread object.
     * Threads created this way must have overridden their run() method to actually do anything.
     */
    public Thread(){
         //TODO codavaj!!
    }

    /**
     * Allocates a new Thread object with a specific target object whose run method is called.
     * target - the object whose run method is called.
     */
    public Thread(java.lang.Runnable target){
         //TODO codavaj!!
    }

    /**
     * Allocates a new Thread object with the given target and name.
     * target - the object whose run method is called.name - the name of the new thread.
     */
    public Thread(java.lang.Runnable target, java.lang.String name){
         //TODO codavaj!!
    }

    /**
     * Allocates a new Thread object with the given name. Threads created this way must have overridden their run() method to actually do anything.
     * name - the name of the new thread.
     */
    public Thread(java.lang.String name){
         //TODO codavaj!!
    }

    /**
     * Returns the current number of active threads in the virtual machine.
     */
    public static int activeCount(){
        return 0; //TODO codavaj!!
    }

    /**
     * Returns a reference to the currently executing Thread object.
     */
    public static java.lang.Thread currentThread(){
        return null; //TODO codavaj!!
    }

    /**
     * Returns this thread's name. Note that in CLDC the name of the thread can only be set when creating the thread.
     */
    public final java.lang.String getName(){
        return null; //TODO codavaj!!
    }

    /**
     * Returns this thread's priority.
     */
    public final int getPriority(){
        return 0; //TODO codavaj!!
    }

    /**
     * Interrupts this thread. In an implementation conforming to the CLDC Specification, this operation is not required to cancel or clean up any pending I/O operations that the thread may be waiting for.
     */
    public void interrupt(){
        return; //TODO codavaj!!
    }

    /**
     * Tests if this thread is alive. A thread is alive if it has been started and has not yet died.
     */
    public final boolean isAlive(){
        return false; //TODO codavaj!!
    }

    /**
     * Waits for this thread to die.
     */
    public final void join() throws java.lang.InterruptedException{
        return; //TODO codavaj!!
    }

    /**
     * If this thread was constructed using a separate Runnable run object, then that Runnable object's run method is called; otherwise, this method does nothing and returns.
     * Subclasses of Thread should override this method.
     */
    public void run(){
        return; //TODO codavaj!!
    }

    /**
     * Changes the priority of this thread.
     */
    public final void setPriority(int newPriority){
        return; //TODO codavaj!!
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of milliseconds. The thread does not lose ownership of any monitors.
     */
    public static void sleep(long millis) throws java.lang.InterruptedException{
        return; //TODO codavaj!!
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread.
     * The result is that two threads are running concurrently: the current thread (which returns from the call to the start method) and the other thread (which executes its run method).
     */
    public void start(){
        return; //TODO codavaj!!
    }

    /**
     * Returns a string representation of this thread, including the thread's name and priority.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /**
     * Causes the currently executing thread object to temporarily pause and allow other threads to execute.
     */
    public static void yield(){
        return; //TODO codavaj!!
    }

}
