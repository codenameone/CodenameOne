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
package com.codename1.ui.util;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Animation;

/**
 * Simple timer callback that is invoked on the CodenameOne EDT thread rather
 * than on a separate thread. Notice that the accuracy of this timer is very low!
 * A timer must be linked to a specific form 
 * 
 * @author Shai Almog
 */
public class UITimer {
    private Runnable internalRunnable;
    private Form bound;
    private long lastEllapse;
    private int ms;
    private boolean repeat;
    private Internal i = new Internal();
    
    /**
     * This constructor is useful when deriving this class to implement a timer.
     */
    protected UITimer() {
    }
    
    /**
     * Constructor that accepts a runnable to invoke on timer elapse
     * 
     * @param r runnable instance
     */
    public UITimer(Runnable r) {
        internalRunnable = r;
    }

    
    
    /**
     * Binds the timer to start at the given schedule
     * 
     * @param timeMillis the time from now in milliseconds
     * @param repeat whether the timer repeats
     * @param bound  the form to which the timer is bound
     */
    public void schedule(int timeMillis, boolean repeat, Form bound) {
        lastEllapse = System.currentTimeMillis();
        ms = timeMillis;
        this.repeat = repeat;
        this.bound = bound;
        bound.registerAnimated(i);
    }
    
    /**
     * Stops executing the timer
     */
    public void cancel() {
        bound.deregisterAnimated(i);
    }
    

    void testEllapse() {
        long t = System.currentTimeMillis();
        if(t - lastEllapse >= ms) {
            if(!repeat) {
                Display.getInstance().getCurrent().deregisterAnimated(i);
            }
            lastEllapse = t;
            i.run();
        }
    }

    
    class Internal implements Runnable, Animation {
        /**
         * @inheritDoc
         */
        public boolean animate() {
            testEllapse();
            return false;
        }

        /**
         * @inheritDoc
         */
        public void paint(Graphics g) {
        }
        
        /**
         * Invoked when the timer elapses
         */
        public void run() {
            if(internalRunnable != null) {
                internalRunnable.run();
            }
        }
    }
}
