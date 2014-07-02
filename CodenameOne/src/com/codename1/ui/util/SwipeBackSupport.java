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
import com.codename1.ui.Painter;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.util.LazyValue;

/**
 * Allows binding a swipe listener to the form that enables the user to swipe back to the previous
 * form.
 *
 * @author Shai Almog
 */
public class SwipeBackSupport {
    boolean sideSwipePotential;
    int initialDragY;
    int initialDragX;
    boolean dragActivated;
    boolean transitionRunning;
    int currentX;
    ActionListener pointerDragged;
    ActionListener pointerPressed;
    ActionListener pointerReleased;
    Form destinationForm;
    
    /**
     * Binds support for swiping to the given forms
     * 
     * @param currentForm the current form
     * @param destination the destination form which can be created lazily
     */
    public static void bindBack(Form currentForm, LazyValue<Form> destination) {
        new SwipeBackSupport().bind(currentForm, destination);
    } 
    
    /**
     * Binds support for swiping to the current form
     * 
     * @param destination the destination form which can be created lazily
     */
    public static void bindBack(LazyValue<Form> destination) {
        new SwipeBackSupport().bind(Display.getInstance().getCurrent(), destination);
    } 
    
    
    /**
     * Binds support for swiping to the given forms
     * 
     * @param currentForm the current form
     * @param destination the destination form which can be created lazily
     */
    protected void bind(final Form currentForm, final LazyValue<Form> destination) {
        pointerDragged = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (sideSwipePotential) {
                    final int x = evt.getX();
                    final int y = evt.getY();
                    if (Math.abs(y - initialDragY) > x - initialDragX) {
                        sideSwipePotential = false;
                        return;
                    }
                    evt.consume();
                    if(dragActivated) {
                        currentX = x;
                        Display.getInstance().getCurrent().repaint();
                    } else {
                        if (x - initialDragX > Display.getInstance().convertToPixels(currentForm.getUIManager().getThemeConstant("backGestureThresholdInt", 5), true)) {
                            dragActivated = true;
                            destinationForm = destination.get();
                            startBackTransition(currentForm, destinationForm);
                        }
                    }
                }
            }
        };
        pointerReleased = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(dragActivated) {
                    int destNumberX = Display.getInstance().getDisplayWidth();
                    int incrementsX = Display.getInstance().convertToPixels(3, true);
                    if(currentX < destNumberX / 2) {
                        destinationForm = currentForm;
                        destNumberX = 0;
                        incrementsX *= -1;
                    }
                    final int destNumber = destNumberX;
                    final int increments = incrementsX;
                    Display.getInstance().getCurrent().registerAnimated(new Animation() {
                        public boolean animate() {
                            currentX += increments;
                            if(currentX > 0 && currentX >= destNumber || currentX < 0 && currentX <= destNumber) {
                                currentX = destNumber;
                                Transition t = destinationForm.getTransitionInAnimator();
                                destinationForm.setTransitionInAnimator(CommonTransitions.createEmpty());
                                destinationForm.show();
                                destinationForm.setTransitionInAnimator(t);
                                destinationForm = null;
                                dragActivated = false;
                                return false;
                            }
                            return true;
                        }

                        public void paint(Graphics g) {
                        }
                    });
                }
            }
        };
        pointerPressed = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                sideSwipePotential = false;
                int displayWidth = Display.getInstance().getDisplayWidth();
                sideSwipePotential = !transitionRunning && evt.getX() < displayWidth / currentForm.getUIManager().getThemeConstant("sideSwipeSensitiveInt", 10);
                initialDragX = evt.getX();
                initialDragY = evt.getY();
                /*if (sideSwipePotential) {
                    Component c = Display.getInstance().getCurrent().getComponentAt(initialDragX, initialDragY);
                    if (c != null && c.shouldBlockSideSwipe()) {
                        sideSwipePotential = false;
                    }
                }*/
            }
        };
        currentForm.addPointerDraggedListener(pointerDragged);
        currentForm.addPointerReleasedListener(pointerReleased);
        currentForm.addPointerPressedListener(pointerPressed);
    } 
    
    void startBackTransition(final Form currentForm, Form destination) {
        final Transition t = destination.getTransitionOutAnimator().copy(true);
        if(t instanceof CommonTransitions) {
            Transition originalTransition = currentForm.getTransitionOutAnimator();
            currentForm.setTransitionOutAnimator(CommonTransitions.createEmpty());
            Form blank = new Form() {
                protected boolean shouldSendPointerReleaseToOtherForm() {
                    return true;
                }
            };
            blank.addPointerDraggedListener(pointerDragged);
            blank.addPointerReleasedListener(pointerReleased);
            blank.addPointerPressedListener(pointerPressed);
            blank.setTransitionInAnimator(CommonTransitions.createEmpty());
            blank.setTransitionOutAnimator(CommonTransitions.createEmpty());
            blank.show();
            currentForm.setTransitionOutAnimator(originalTransition);
            ((CommonTransitions)t).setMotion(new LazyValue<Motion>() {
                public Motion get(Object... args) {
                    return new ManualMotion(((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), ((Integer)args[2]).intValue());
                }
            });
            t.init(currentForm, destination);
            t.initTransition();
            blank.setGlassPane(new Painter() {
                public void paint(Graphics g, Rectangle rect) {
                    t.animate();
                    t.paint(g);
                }
            });
        }
    }
    
    class ManualMotion extends Motion {
        protected ManualMotion(int sourceValue, int destinationValue, int duration) {
            super(sourceValue, destinationValue, duration);
        }

        public int getValue() {
            int destinationValue = getDestinationValue();
            int sourceValue = getSourceValue();
            float ratio = ((float)currentX) / ((float)Display.getInstance().getDisplayWidth());
            int dis = destinationValue - sourceValue;
            int val = (int)(sourceValue + (ratio * dis));

            if(destinationValue < sourceValue) {
                return Math.max(destinationValue, val);
            } else {
                return Math.min(destinationValue, val);
            }
        }
        
        public boolean isFinished() {
            return false;
        }
    }
}
