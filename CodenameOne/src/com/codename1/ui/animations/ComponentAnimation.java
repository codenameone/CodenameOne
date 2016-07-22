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
    private int step = -1;
    
    /**
     * Step mode allows stepping thru an animation one frame at a time, e.g. when scrolling down an animation
     * might change title elements then change them back as we scroll up.
     * @return true if this animation can be stepped in which case the setStep etc. methods should work.
     */
    public boolean isStepModeSupported() {
        return false;
    }
    
    /**
     * Sets the current animation step to a value between 0 and maxSteps
     * @param step the current step
     */
    public void setStep(int step) {
        this.step = step;
    }
    
    public int getStep() {
        return step;
    }
    
    /**
     * The total number of steps in this animation. 
     * @return the number of steps
     */
    public int getMaxSteps() {
        return 100;
    }
    
    /**
     * Indicates if the animation is in progress
     * @return true if in progress
     */
    public abstract boolean isInProgress();
    
    /**
     * Updates the animation state
     */
    protected abstract void updateState();
    
    /**
     * Invoked by the animation manager internally
     */
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
     * Flushes the animation immediately, this will be called if the form is de-initialized
     */
    public void flush() {
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
    
    /**
     * Allows us to create an animation that compounds several separate animations so they appear as a 
     * single animation to the system and process in parallel
     * @param anims the animations
     * @return the compounded animation
     */
    public static ComponentAnimation compoundAnimation(ComponentAnimation... anims) {
        return new CompoundAnimation(anims);
    }

    /**
     * Allows us to create an animation that places several separate animations in a sequence so they appear as a 
     * single animation to the system and process one after the other
     * @param anims the animations
     * @return the sequential animation
     */
    public static ComponentAnimation sequentialAnimation(ComponentAnimation... anims) {
        return new CompoundAnimation(anims, true);
    }
    
    static class CompoundAnimation extends ComponentAnimation {
        private ComponentAnimation[] anims;
        int sequence;
        public CompoundAnimation(ComponentAnimation[] anims) {
            this.anims = anims;
            sequence = -1;
        }

        public CompoundAnimation(ComponentAnimation[] anims, boolean s) {
            this.anims = anims;
            sequence = 0;
        }

        @Override
        public boolean isInProgress() {
            if(sequence > -1) {
                if(anims[sequence].isInProgress()) {
                    return true;
                }
                while(anims.length < sequence) {
                    sequence++;
                    if(anims[sequence].isInProgress()) {
                        return true;
                    }
                }
                return false;
            }
            for(ComponentAnimation a : anims) {
                if(a.isInProgress()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void updateState() {
            if(sequence > -1) {
                anims[sequence].updateState();
                return;
            }
            for(ComponentAnimation a : anims) {
                a.updateState();
            }
        }
        
        @Override
        public void flush() {
            for(ComponentAnimation a : anims) {
                a.flush();
            }
        }
    }
}
