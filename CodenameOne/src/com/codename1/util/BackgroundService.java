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

import com.codename1.impl.CodenameOneImplementation;

/**
 * Encapsulates a background task that can continue to run even if the app
 * has been closed.  It can be used to execute something immediately or on a timer
 * to execute at some point in the future.  Additionally it can be set up to
 * repeat the task on an interval.
 * @author shannah
 */
public class BackgroundService implements Runnable  {
    
    /**
     * Indicates that platform has full support for background services.
     * @see #getBackgroundServiceSupport()
     */
    public static final int SUPPORT_UNLIMITED = 1;
    
    /**
     * Indicates that platform has limited support for background services.  This
     * means that while background services will run, there may be an expiry time
     * on the service.  
     * @see #getBackgroundServiceSupport() 
     * @see #getTimeToExpiration() 
     */
    public static final int SUPPORT_LIMITED = 2;
    
    /**
     * Indicates that the platform doesn't support background services at all. 
     * If you attempt to run a background service on such a platform, it should
     * throw an exception.
     * @see #getBackgroundServiceSupport() 
     */
    public static final int SUPPORT_NONE = 0;
    
    /**
     * Indicates that the platform doesn't support true background services, but it
     * will emulate them as long as the app is running.  Emulation is basically
     * just running the service in a background thread.  If the app is suspended
     * or killed, then so is the background service.
     * @see #getBackgroundServiceSupport() 
     */
    public static final int SUPPORT_EMULATED = 3;
    
    
    /**
     * A name for the background service.
     */
    private String name = "BackgroundService";
    private Runnable task, cleanup;
    //private int timeout;
    //private int interval;
    //private Date startTime;
    private boolean complete;
    private Object peer;
    
    /**
     * Invoked internally from Display, this method is for internal use only
     * 
     * @param impl implementation instance
     */
    public static void setImplementation(CodenameOneImplementation impl) {
        BackgroundService.impl = impl;
    }
    
    public BackgroundService() {
        this(new Runnable() {

            public void run() {
                
            }
          
        }, new Runnable() {

            public void run() {
                
            }
          
        });

    }
    
    /**
     * Creates a new BackgroundService to run the given task.
     * @param task The task to be run in the service.
     * @param cleanup Runnable to clean up after the task is either complete
     *     or killed.  If the task runs to completion, then isComplete()
     *     will return <code>true</code>.
     */
    public BackgroundService(Runnable task, Runnable cleanup) {
        this.task = task;
        this.cleanup = cleanup;
    }
    
    /**
     * Creates a new BackgroundService to run the given task.
     * @param task The task to be run in the service.
     */
    public BackgroundService(Runnable task) {
        this(task, new Runnable() {

            public void run() {
                
            }
            
        });
    }
    
    /**
     * Gets the number of milliseconds until the service expires.  When it expires
     * the operating system will interrupt the task and call the <code>cleanup</code>
     * callback, which will have an opportunity to do final cleanup quickly.
     * @return The number of milliseconds until the service expires.
     */
    public long getTimeToExpiration() {
        return impl.getTimeToBackgroundServiceExpiration(peer);
    }
    
    
    /**
     * Starts the background service.
     */
    public void start() {
        if (getBackgroundServiceSupport() == SUPPORT_NONE) {
            throw new RuntimeException("Background Services are not supported on this platform.");
        }
        peer = impl.startBackgroundService(this);
    }
    
    
    
    private static CodenameOneImplementation impl;

    /**
     * Gets the task that will be run by this service.
     * @return the task to be run.
     */
    public Runnable getTask() {
        return task;
    }

    
    public void setTask(Runnable r) {
        this.task = r;
    }

    /**
     * Gets the callback to be executed when the task completes or the service
     * expires, whichever comes first.
     * @return the cleanup
     */
    public Runnable getCleanup() {
        return cleanup;
    }
    
    public void setCleanup(Runnable r) {
        this.cleanup = r;
    }


//    /**
//     * If <code>timeout > 0</code>, the service will be delayed.
//     * @return the timeout in milliseconds before the service should start.
//     */
//    public int getTimeout() {
//        return timeout;
//    }
//
//    /**
//     * Sets the <code>timeout</code> delay to wait before the service begins
//     * execution.
//     * @param timeout the timeout to set
//     */
//    public void setTimeout(int timeout) {
//        this.timeout = timeout;
//    }
//
//    /**
//     * If <code>interval > 0</code> this service will repeat the task at the 
//     * specified interval.
//     * @return the repeat interval for the service in milliseconds.
//     */
//    public int getInterval() {
//        return interval;
//    }
//
//    /**
//     * Sets the interval period for scheduling the task.  If this value is set to
//     * greater than zero, the service will repeat the task every <code>interval</code>
//     * milliseconds.
//     * @param interval the interval to set.
//     */
//    public void setInterval(int interval) {
//        this.interval = interval;
//    }

    /**
     * Checks to see if the task was run through to completion.  If the service
     * expired before the task completed, then the <code>cleanup</code> callback
     * can call this to find that out.
     * @return the complete
     */
    public boolean isComplete() {
        return complete;
    }
    
    
    /**
     * Sets the <code>complete</code> to indicate that the task ran through 
     * to completion.
     * @param complete True to indicate that the task ran to completion. 
     */
    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    /**
     * Gets the name of the background service.  Used for debugging purposes.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the background service.  Used for debugging purposes.
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

//    /**
//     * Gets the start time for the service.  If this is specified, then the 
//     * service will be scheduled like a calendar event to be executed in the 
//     * future.
//     * @return the startTime  The time to start the service.
//     */
//    public Date getStartTime() {
//        return startTime;
//    }
//
//    /**
//     * Sets the start time for the service.  This optionally allows you to schedule the
//     * service to run at a point in the future.  On supported platforms, this will cause
//     * the OS to start up the app even if it is closed, to run the service in the background.
//     * @param startTime the startTime to set
//     */
//    public void setStartTime(Date startTime) {
//        this.startTime = startTime;
//    }
//    
    /**
     * Checks the support level of the current platform for background services.
     * @return Should return one of the constants: {@link #SUPPORT_EMULATED}, {@link #SUPPORT_LIMITED},
     *  {@link #SUPPORT_NONE}, or {@link #SUPPORT_UNLIMITED}.
     */
    public static int getBackgroundServiceSupport() {
        return impl.getBackgroundServiceSupport();
    }

    public void run() {
        if (task != null) {
            task.run();
        }
    }
    
    
}
