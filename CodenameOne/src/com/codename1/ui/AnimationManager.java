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

package com.codename1.ui;

import com.codename1.io.Util;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.events.ScrollListener;
import java.util.ArrayList;

/**
 * Animation manager concentrates all of the animations for a given form into a single place that allows us
 * to manage all mutations to a Form in a way the prevents collisions between mutations. The one type of
 * animation that isn't handled by this class is the form level transition, replace transitions are handled by this class.
 *
 * @author Shai Almog
 */
public final class AnimationManager {
    private final Form parentForm;
    private ArrayList<ComponentAnimation> anims = new ArrayList<ComponentAnimation>();
    private ArrayList<Runnable> postAnimations = new ArrayList<Runnable>();
    
    AnimationManager(Form parentForm) {
        this.parentForm = parentForm;
    }
    
    /**
     * Returns true if an animation is currently in progress
     * @return true if an animation is currently in progress
     */
    public boolean isAnimating() {
        int size = anims.size();
        if(size == 0) {
            return false;
        }
        if(size > 1) {
            return true;
        }
        // special case where an animation finished but wasn't removed from the queue just yet...
        return anims.get(0).isInProgress();
    }
    
    void updateAnimations() {
        if(anims.size() > 0) {
            ComponentAnimation c = anims.get(0);
            if(c.isInProgress()) {
                c.updateAnimationState();
            } else {
                c.updateAnimationState();
                anims.remove(c);
            }
        } else {
            while(postAnimations.size() > 0) {
                postAnimations.get(0).run();
                postAnimations.remove(0);
            }
        }
    }

    void flush() {
        while(anims.size() > 0) {
            anims.get(0).flush();
            anims.remove(0);
        }
    }
    
    /**
     * Adds the animation to the end to the animation queue
     * @param an the animation object
     */
    public void addAnimation(ComponentAnimation an) {
        anims.add(an);
        Display.getInstance().notifyDisplay();
    }
    
    /**
     * Adds the animation to the end of the animation queue and blocks the current thread until the animation
     * completes 
     * @param an the animation to perform 
     */
    public void addAnimationAndBlock(final ComponentAnimation an) {
        final Object LOCK = new Object();
        an.setNotifyLock(LOCK);
        addAnimation(an);
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while(an.isInProgress()) {
                    Util.wait(LOCK, 50);
                }
            }
        });
    }

    
    /**
     * Adds the animation to the end to the animation queue
     * @param an the animation object
     * @param callback invoked when the animation completes
     */
    public void addAnimation(ComponentAnimation an, Runnable callback) {
        an.setOnCompletion(callback);
        addAnimation(an);
        Display.getInstance().notifyDisplay();
    }
    
    /**
     * Performs a step animation as the user scrolls down/up the page e.g. slowly converting a title UIID from
     * a big visual representation to a smaller title for easier navigation then back again when scrolling up
     * @param cna the animation to bind to the scroll event
     */
    public void onTitleScrollAnimation(final ComponentAnimation... cna) {
        onTitleScrollAnimation(parentForm.getContentPane(), cna);
    }

    /**
     * Performs a step animation as the user scrolls down/up the page e.g. slowly converting a title UIID from
     * a big visual representation to a smaller title for easier navigation then back again when scrolling up
     * @param content the scrollable container representing the body
     * @param cna the animation to bind to the scroll event
     */
    public void onTitleScrollAnimation(Container content, final ComponentAnimation... cna) {
        content.addScrollListener(new ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                if(scrollY >= 0) {
                    boolean changed = false;
                    for(ComponentAnimation c : cna) {
                        if(scrollY < c.getMaxSteps()) {
                            c.setStep(scrollY);
                            c.updateAnimationState();
                            changed = true;
                        }
                    } 
                    if(changed) {
                        parentForm.revalidate();
                    }
                }
            }
        });
    }
    
    /**
     * Invokes the runnable when all animations have completed
     * @param r the runnable that will be invoked after the animations
     */
    public void flushAnimation(Runnable r) {
        if(isAnimating()) {
            postAnimations.add(r);
        } else {
            r.run();
        }
    }
}
