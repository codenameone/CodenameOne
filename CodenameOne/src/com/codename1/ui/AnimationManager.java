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
    private ArrayList<ArrayList<ComponentAnimation>> anims_queues = new ArrayList<ArrayList<ComponentAnimation>>(); //animations queues. Animations of a same queue would be run in serie while queues run in parrallel
    private ArrayList<ArrayList<Runnable>> postAnimations_queues = new ArrayList<ArrayList<Runnable>>(); //runnables that would run when all animations of a specific queue have finished 
    private ArrayList<Runnable> postAllAnimations =  new ArrayList<Runnable>(); //runnables that would run when all animations, of all queues, have finished 
        
    AnimationManager(Form parentForm) {
        this.parentForm = parentForm;
    }
    
    /**
     * Returns true if an animation is currently in progress in a given queue
     * @param qIndex: the index of the queue
     * @return true if an animation is currently in progress in this queue
     */
    public boolean isAnimating(int qIndex) {
    	if (qIndex >= 0 && qIndex < anims_queues.size()) {
    		ArrayList<ComponentAnimation> anims = anims_queues.get(qIndex);
    		if (anims != null) {
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
    	}
    	return false;
    }
    
    
    public boolean isAnimating() {
    	for (int i=0; i<anims_queues.size(); i++) {
    		if (isAnimating(i)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    
    void updateAnimations() {
    	boolean animated = false;
    	for (int i=0; i<anims_queues.size(); i++) {
    		ArrayList<ComponentAnimation> anims = anims_queues.get(i);
    		if (anims != null) {
	    	    if(anims.size() > 0) {
		            ComponentAnimation c = anims.get(0);
		            if(c.isInProgress()) {
		                c.updateAnimationState();
		            } else {
		                c.updateAnimationState();
		                anims.remove(c);
		            }
		            animated = true;
		        } else {  ////execute postAnimations that should be run when this queue has finished animating
		        	if (i >= 0 && i < postAnimations_queues.size()) { //a queue might not have any associated postAnimations so must ensure that postAnimations_queues.get(i) would not throw an outofbounds exception
		        		ArrayList<Runnable> postAnimations = postAnimations_queues.get(i);
		        		if (postAnimations != null) {
		        			while(postAnimations.size() > 0) {
		        				postAnimations.get(0).run();
		        				postAnimations.remove(0);
		        			}
		        		}
		        	}
		        }
    		}
    	}
    	if (!animated) { //execute postAnimations that should be run when all queues have finished animating
    		while(postAllAnimations.size() > 0) {
    			postAllAnimations.get(0).run();
    			postAllAnimations.remove(0);
    		}
    	}
    }

    void flush() {
    	for (int i=0; i<anims_queues.size(); i++) {
    		ArrayList<ComponentAnimation> anims = anims_queues.get(i);
    		if (anims != null) {
    			while(anims.size() > 0) {
    				anims.get(0).flush();
    				anims.remove(0);
    			}
    		}
    	}
    }
    
    /**
     * Adds the animation to the end of a specific animation queue
     * Be carefull when using more than one queue in an AnimationManager as these
     * queue would run their animations in parallel. 
     * So you must ensure that no animation of one queue can conflict with the animation of another queue
     * @param an: the animation object
     * @param qIndex: the index of the queue
     */
    public void addAnimation(ComponentAnimation an, int qIndex) {
    	if (qIndex >= 0) {
    		ArrayList<ComponentAnimation> anims = null;
    		if (qIndex < anims_queues.size()) {
    			anims = anims_queues.get(qIndex);
    			if (anims == null) {
        			anims = new ArrayList<ComponentAnimation>();
        			anims_queues.set(qIndex, anims);
        		}
    		}
    		else {
    			anims = new ArrayList<ComponentAnimation>();
    			anims_queues.add(qIndex, anims);
    		}
    		anims.add(an);
    		Display.getInstance().notifyDisplay();
    	}
    }
    
    /**
     * Adds the animation to the end of a specific animation queue and blocks the current thread until the animation
     * completes 
     * Be carefull when using more than one queue in an AnimationManager as these
     * queue would run their animations in parallel. 
     * So you must ensure that no animation of one queue can conflict with the animation of another queue
     * @param an: the animation to perform 
     * @param qIndex: the index of the queue
     */
    public void addAnimationAndBlock(final ComponentAnimation an, int qIndex) {
        final Object LOCK = new Object();
        an.setNotifyLock(LOCK);
        addAnimation(an, qIndex);
        final ArrayList<ComponentAnimation> anims = anims_queues.get(qIndex); //necessarily ok here as addAnimation() would have created the queue if it didn't exist
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while(an.isInProgress() && anims.contains(an)) {
                    Util.wait(LOCK, 50);
                }
            }
        });
    }

    
    /**
     * Adds the animation to the end of a specific animation queue
     * Be carefull when using more than one queue in an AnimationManager as these
     * queue would run their animations in parallel. 
     * So you must ensure that no animation of one queue can conflict with the animation of another queue
     * @param an: the animation object
     * @param callback: invoked when the animation completes
     * @param qIndex: the index of the queue
     */
    public void addAnimation(ComponentAnimation an, Runnable callback, int qIndex) {
        an.setOnCompletion(callback);
        addAnimation(an, qIndex);
        Display.getInstance().notifyDisplay();
    }
    
    
    /**
     * Adds the animation to the end of the main animation queue
     * @param an the animation object
     */
    public void addAnimation(ComponentAnimation an) {
    	addAnimation(an, 0);
    }
    
    /**
     * Adds the animation to the end of the main animation queue and blocks the current thread until the animation
     * completes 
     * @param an the animation to perform 
     */
    public void addAnimationAndBlock(final ComponentAnimation an) {
    	addAnimationAndBlock(an, 0);
    }

    
    /**
     * Adds the animation to the end to the main animation queue
     * @param an the animation object
     * @param callback invoked when the animation completes
     */
    public void addAnimation(ComponentAnimation an, Runnable callback) {
    	addAnimation(an, callback, 0);
    }
    
    
    /**
     * Adds an animation that should be run in parallel to others animations
     * So this animation would be put in a new animation queue
     * Be carefull when using multiple animation queues. This is your responsability
     * to ensure that no animation of one queue can conflict with the animation of another queue
     * @param an the animation object
     * @return the index of the queue used for this animation
     */
    public int addParallelAnimation(ComponentAnimation an) {
    	int qi = firstEmptyQueue();
    	addAnimation(an, qi);
    	return qi;
    }
    
    /**
     * Adds an animation that should be run in parallel to others animations
     * and blocks the current thread until the animation completes 
     * So this animation would be put in a new animation queue
     * Be carefull when using multiple animation queues. This is your responsability
     * to ensure that no animation of one queue can conflict with the animation of another queue
     * @param an the animation to perform 
     * @return the index of the queue used for this animation
     */
    public int addParallelAnimationAndBlock(final ComponentAnimation an) {
    	int qi = firstEmptyQueue();
    	addAnimationAndBlock(an, qi);
    	return qi;
    }

    
    /**
     * Adds an animation that should be run in parallel to others animations
     * So this animation would be put in a new animation queue
     * Be carefull when using multiple animation queues. This is your responsability
     * to ensure that no animation of one queue can conflict with the animation of another queue
     * @param an the animation object
     * @param callback invoked when the animation completes
     * @return the index of the queue used for this animation
     */
    public int addParallelAnimation(ComponentAnimation an, Runnable callback) {
    	int qi = firstEmptyQueue();
    	addAnimation(an, callback, qi);
    	return qi;
    }
    
    
    /**
     * @return the index of the first empty queue
     */
    private int firstEmptyQueue() {
    	for (int i=0; i<anims_queues.size(); i++) {
    		ArrayList<ComponentAnimation> aq = anims_queues.get(i);
    		if (aq == null || aq.isEmpty()) {
    			//ensure that there is no postAnimation queue pending associated to that queue
    			ArrayList<Runnable> postAnimations = null;
    			if (i >= 0 && i < postAnimations_queues.size()) { 
    				postAnimations = postAnimations_queues.get(i);
    			}
    			if (postAnimations == null || postAnimations.isEmpty()) {
					return i;
				}
    		}
    	}
    	return anims_queues.size();
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
            boolean recursion = false;
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                if(recursion) {
                    return;
                }
                recursion = true;
                if(scrollY >= 0) {
                    boolean changed = false;
                    for(ComponentAnimation c : cna) {
                        if(scrollY < c.getMaxSteps()) {
                            c.setStep(scrollY);
                            c.updateAnimationState();
                            changed = true;
                        } else {
                            if(c.getStep() < c.getMaxSteps()) {
                                c.setStep(c.getMaxSteps());
                                c.updateAnimationState();
                                changed = true;
                            }
                        }
                    } 
                    if(changed) {
                        parentForm.revalidate();
                    }
                }
                recursion = false;
            }
        });
    }
       
    
    /**
     * Invokes the runnable when all animations of specific queue have completed
     * @param r: the runnable that will be invoked after the animations of the queue
     * @param qIndex: the index of the queue 
     */
    public void flushAnimation(Runnable r, int qIndex) {
    	if(isAnimating(qIndex)) { //qIndex necessarily >= 0
    		ArrayList<Runnable> postAnimations = null;
    		if (qIndex < postAnimations_queues.size()) {
    			postAnimations = postAnimations_queues.get(qIndex);
    			if (postAnimations == null) {
    				postAnimations = new ArrayList<Runnable>();
    				postAnimations_queues.set(qIndex, postAnimations);
    			}
    		}
    		else {
    			postAnimations = new ArrayList<Runnable>();
    			postAnimations_queues.add(qIndex, postAnimations);
    		}
    		postAnimations.add(r);
    	} else {
    		r.run();
    	}
    }
    
    /**
     * Invokes the runnable when all animations of the main queue (i.e queue with index 0) have completed
     * @param r the runnable that will be invoked after the animations of the main queue
     */
    public void flushAnimation(Runnable r) {
    	flushAnimation(r, 0);
    }
    
    
    /**
     * Invokes the runnable when all animations (of all queues) have completed
     * @param r: the runnable that will be invoked after the animations of the queue
     */
    public void flushAnimationAll(Runnable r) {
        if(isAnimating()) {
        	postAllAnimations.add(r);
        } else {
            r.run();
        }
    }
}
