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

package com.codename1.ui.animations;

/**
 * Parent class representing an animation object within the AnimationManager queue.
 *
 * @author Shai Almog
 */
public abstract class ComponentAnimation {
    private Object notifyLock;
    private Runnable onCompletion;
    
    public abstract boolean isInProgress();
    
    protected abstract void updateState();
    
    public final void updateAnimationState() {
        updateState();
        if(!isInProgress()) {
            if(notifyLock != null) {
                synchronized(notifyLock) {
                    notifyLock.notify();
                }
            }
            if(onCompletion != null) {
                onCompletion.run();
            }
        }
    }
    
    /**
     * This method is used internally by the addAnimationAndBlock method of AnimationManager and shouldn't
     * be used outside of that.
     * @param l the lock object
     */
    public final void setNotifyLock(Object l) {
        if(notifyLock != null) {
            throw new RuntimeException("setNotifyLock shouldn't be invoked more than once"); 
        }
        this.notifyLock = l;
    }

    
    /**
     * This method is used internally by the addAnimation method of AnimationManager and shouldn't
     * be used outside of that.
     * @param r the callback
     */
    public final void setOnCompletion(Runnable r) {
        if(onCompletion != null) {
            throw new RuntimeException("setOnCompletion shouldn't be invoked more than once"); 
        }
        this.onCompletion = r;
    }
}
