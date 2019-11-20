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

import com.codename1.ui.AnimationManager;
import com.codename1.ui.Container;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Parent class representing an animation object within the AnimationManager queue.
 *
 * @author Shai Almog
 */
public abstract class ComponentAnimation {
    private Object notifyLock;
    private Runnable onCompletion;
    private int step = -1;
    private ArrayList<Runnable> post;
    private boolean completed = false;

    /**
     * Invokes the runnable just as the animation finishes thus allowing cleanup of the UI for the upcoming 
     * animations, this is useful when running a complex sequence
     * @param r the runnable to call when the animation is done
     */
    public void addOnCompleteCall(Runnable r) {
        if(post == null) {
            post = new ArrayList<Runnable>();
        }
        post.add(r);        
    }
    
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
        	if (!completed) {
        		completed = true;
        		if(notifyLock != null) {
        			synchronized(notifyLock) {
        				notifyLock.notify();
        			}
        		}
        		if(onCompletion != null) {
        			onCompletion.run();
        		}
        		if(post != null) {
        			for(Runnable p : post) {
        				p.run();
        			}
        		}
        	}
        }
        else { //ensure completed would be set to false if animation has been restarted
        	 completed = false; 
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
        ComponentAnimation[] anims;
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
            if(sequence > -1 && sequence < anims.length) {
                if(anims[sequence].isInProgress()) {
                    return true;
                }
                while(anims.length > sequence) {
                    if(anims[sequence].isInProgress()) {
                        return true;
                    }
                    sequence++;
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
                anims[Math.min(sequence, anims.length - 1)].updateState();
                return;
            }
            for(ComponentAnimation a : anims) {
                a.updateAnimationState();
            }
        }
        
        @Override
        public void flush() {
            for(ComponentAnimation a : anims) {
                a.flush();
            }
        }

        @Override
        public int getMaxSteps() {
            if (sequence > -1) {
                int out = 0;
                for (ComponentAnimation a : anims) {
                    out += a.getMaxSteps();
                }
                return out;
            } else {
                int out = 0;
                for (ComponentAnimation a : anims) {
                    out = Math.max(a.getMaxSteps(), out);
                }
                return out;
            }
            
        }

        @Override
        public void setStep(int step) {
            super.setStep(step);
            if (sequence > -1) {
                int animIdx = 0;
                int len = anims.length;
                while (animIdx < anims.length && anims[animIdx].getMaxSteps() <= step) {
                    ComponentAnimation anim = anims[animIdx];
                    anim.setStep(anim.getMaxSteps());
                    step -= anim.getMaxSteps();
                    animIdx++;
                }
                while (animIdx < len) {
                    anims[animIdx].setStep(0);
                    animIdx++;
                }
                
            } else {
                for (ComponentAnimation anim : anims) {
                    anim.setStep(Math.min(anim.getMaxSteps(), step));
                }
            }
        }
        
        
        
    }
    
    /**
     * A special kind of ComponentAnimation that encapsulates a mutation of the 
     * user interface.  This class used internally to allow compatible UI mutation
     * animations to run concurrently.  Two UI mutations are compatible if the containers
     * that they mutate reside in separate branches of the UI tree.  I.e. As long as neither
     * container contains the other, their mutations are compatible.
     * 
     * @since 7.0
     * @see AnimationManager#addUIMutation(com.codename1.ui.Container, com.codename1.ui.animations.ComponentAnimation) 
     * @see AnimationManager#addUIMutation(com.codename1.ui.Container, com.codename1.ui.animations.ComponentAnimation, java.lang.Runnable) 
     */
    public static class UIMutation extends CompoundAnimation {
        
        /**
         * Containers that are being mutated as a part of this animation.
         */
        private Set<Container> containers = new HashSet<Container>();
        
        /**
         * A flag that is set the first time updateState() is called.  Once this 
         * flag is set, the UIMutation will not accept any more mutations.
         */
        private boolean isStarted;
        
        /**
         * Creates a new UIMutation which mutates the given container with the provided
         * animation.
         * @param cnt The container that is being mutated.
         * @param anim The animation.
         */
        public UIMutation(Container cnt, ComponentAnimation anim) {
            super(new ComponentAnimation[]{anim});
        }

        /**
         * Tries to add another mutation to this UIMutation.
         * 
         * @param cnt The container that is being mutated.
         * @param anim The animation
         * @return True if it was successfully added.  False otherwise.  This will return false if
         * {@link #isLocked() } returns true (i.e. the animation has already stared), or if the mutation
         * is incompatible with any of the existing mutations in this mutation.
         */
        public boolean add(Container cnt, ComponentAnimation anim) {
            if (isStarted) {
                return false;
            }
            for (Container existing : containers) {
                if (cnt == existing || existing.contains(cnt) || cnt.contains(existing)) {
                    return false;
                }
            }
            
            ComponentAnimation[] newAnims = new ComponentAnimation[anims.length+1];
            System.arraycopy(anims, 0, newAnims, 0, anims.length);
            newAnims[anims.length] = anim;
            anims = newAnims;
            containers.add(cnt);
            return true;
        }
        
        /**
         * Checks if this mutation is locked.  Once a mutation animation has started,
         * it becomes locked, and cannot have any further mutations added to it.
         * @return 
         */
        public boolean isLocked() {
            return isStarted;
        }
        
        
        @Override
        protected void updateState() {
            isStarted = true;
            super.updateState();
        }
        
        
    }
}
