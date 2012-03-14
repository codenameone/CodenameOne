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
package com.codename1.ui.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Painter;
import com.codename1.ui.RGBImage;
import com.codename1.ui.plaf.UIManager;

/**
 * Contains common transition animations including the following:
 * <ol>
 * <li>Slide - the exiting form slides out of the screen while the new form slides in. 
 * <li>Fade - components fade into/out of the screen
 * <li>Timeline - uses an animation image as an alpha mask between the source/destination
 * </ol>
 * <p>Instances of this class are created using factory methods.
 * 
 * @author Shai Almog, Chen Fishbein
 */
public final class CommonTransitions extends Transition {
    private Motion motion, motion2;
    private static final int TYPE_EMPTY = 0;
    private static final int TYPE_SLIDE = 1;
    private static final int TYPE_FADE = 2;
    private static final int TYPE_FAST_SLIDE = 3;
    private static final int TYPE_TIMELINE = 4;
    private static final int TYPE_SLIDE_AND_FADE = 5;
    private static final int TYPE_PULSATE_DIALOG = 6;
    
    /**
     * Slide the transition horizontally
     * @see #createSlide
     */
    public static final int SLIDE_HORIZONTAL = 0;

    /**
     * Slide the transition vertically
     * @see #createSlide
     */
    public static final int SLIDE_VERTICAL = 1;

    private long startTime;
    private int slideType;
    private int speed;
    private int position;
    private int transitionType;
    private Image buffer;
    private Image secondaryBuffer;
    private Image timeline;
    private byte pulseState;
    private static boolean defaultLinearMotion = false;
    private boolean linearMotion = defaultLinearMotion;
    private boolean motionSetManually;
    private int originalWidth, originalHeight, originalX, originalY;

    /**
     * The transition is a special case where we "keep" an allocated buffer
     */
    private RGBImage rgbBuffer;
    private boolean forward;
    private boolean drawDialogMenu;

    private boolean firstFinished;

    private CommonTransitions(int type) {
        transitionType = type;
    }

    /**
     * Creates an empty transition that does nothing. This has the same effect as
     * setting a transition to null.
     * 
     * @return empty transition
     */
    public static CommonTransitions createEmpty() {
        CommonTransitions t = new CommonTransitions(TYPE_EMPTY);
        return t;
    }


    /**
     * Creates a slide transition for the body of the form that fades the title in while sliding
     *
     * @param forward forward is a boolean value, represent the directions of
     * switching forms, for example for a horizontally type, true means
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     */
    public static CommonTransitions createSlideFadeTitle(boolean forward, int duration)  {
        CommonTransitions c = new CommonTransitions(TYPE_SLIDE_AND_FADE);
        c.forward = forward;
        c.speed = duration;
        return c;
    }

    /**
     * Creates a dialog pulsate transition
     */
    public static CommonTransitions createDialogPulsate()  {
        CommonTransitions c = new CommonTransitions(TYPE_PULSATE_DIALOG);
        return c;
    }

    /**
     * Creates a slide transition with the given duration and direction, this differs from the
     * standard slide animation by focusing on speed rather than on minimizing heap usage.
     * This method works by creating two images and sliding them which works much faster for
     * all devices however takes up more ram. Notice that this method of painting doesn't
     * support various basic CodenameOne abilities such as translucent menus/dialogs etc.
     *
     * @param type type can be either vertically or horizontally, which means
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of
     * switching forms, for example for a horizontally type, true means
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @return a transition object
     */
    public static CommonTransitions createFastSlide(int type, boolean forward, int duration) {
        if(Display.getInstance().areMutableImagesFast()) {
            return createFastSlide(type, forward, duration, false);
        }
        return createSlide(type, forward, duration);
    }

    /**
     * Creates a slide transition with the given duration and direction
     * 
     * @param type type can be either vertically or horizontally, which means 
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of 
     * switching forms, for example for a horizontally type, true means 
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @return a transition object
     */
    public static CommonTransitions createSlide(int type, boolean forward, int duration) {
        return createSlide(type, forward, duration, false);
    }

    /**
     * Creates a slide transition with the given duration and direction
     * 
     * @param type type can be either vertically or horizontally, which means 
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of 
     * switching forms, for example for a horizontally type, true means 
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @param drawDialogMenu indicates that the menu (softkey area) of the dialog 
     * should be kept during a slide transition. This is only relevant for 
     * dialog in/out transitions.
     * @return a transition object
     */
    public static CommonTransitions createSlide(int type, boolean forward, int duration, boolean drawDialogMenu) {
        CommonTransitions t = new CommonTransitions(TYPE_SLIDE);
        t.slideType = type;
        t.forward = forward;
        t.speed = duration;
        t.position = 0;
        t.drawDialogMenu = drawDialogMenu;
        return t;
    }

    /**
     * Creates a slide transition with the given duration and direction, this differs from the
     * standard slide animation by focusing on speed rather than on minimizing heap usage
     * This method works by creating two images and sliding them which works much faster for
     * all devices however takes up more ram. Notice that this method of painting doesn't
     * support various basic CodenameOne abilities such as translucent menus/dialogs etc.
     *
     * @param type type can be either vertically or horizontally, which means
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of
     * switching forms, for example for a horizontally type, true means
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @param drawDialogMenu indicates that the menu (softkey area) of the dialog
     * should be kept during a slide transition. This is only relevant for
     * dialog in/out transitions.
     * @return a transition object
     */
    public static CommonTransitions createFastSlide(int type, boolean forward, int duration, boolean drawDialogMenu) {
        CommonTransitions t = new CommonTransitions(TYPE_FAST_SLIDE);
        t.slideType = type;
        t.forward = forward;
        t.speed = duration;
        t.position = 0;
        t.drawDialogMenu = drawDialogMenu;
        return t;
    }

    /**
     * Creates a transition for fading a form in while fading out the original form
     * 
     * @param duration represent the time the transition should take in millisecond
     * @return a transition object
     */
    public static CommonTransitions createFade(int duration) {
        CommonTransitions t = new CommonTransitions(TYPE_FADE);
        t.speed = duration;
        return t;
    }

    /**
     * Creates a transition using an animated image object (e.g. timeline object) as an
     * alpha mask between the source/target
     *
     * @param animation the image object to execute
     * @return a transition object
     */
    public static CommonTransitions createTimeline(Image animation) {
        CommonTransitions t = new CommonTransitions(TYPE_TIMELINE);
        t.timeline = animation;
        t.transitionType = TYPE_TIMELINE;
        return t;
    }

    private Container getDialogParent(Component dlg) {
        return ((Dialog)dlg).getDialogComponent();
    }

    /**
     * @inheritDoc
     */
    public void initTransition() {
        firstFinished = false;
        if(transitionType == TYPE_EMPTY) {
            return;
        }

        startTime = System.currentTimeMillis();
        Component source = getSource();
        Component destination = getDestination();
        position = 0;
        int w = source.getWidth();
        int h = source.getHeight();

        // a transition might occur with illegal source or destination values (common with 
        // improper replace() calls, this may still be valid and shouldn't fail
        if(w <= 0 || h <= 0) {
            return;
        }

        // nothing to prepare in advance  for a shift fade transition
        if(transitionType == TYPE_SLIDE_AND_FADE) {
            motion = createMotion(100, 200, speed);
            motion2 = createMotion(0, getDestination().getWidth(), speed);
            motion.start();
            motion2.start();
            return;
        }

        if(transitionType == TYPE_PULSATE_DIALOG) {
            if(getDestination() instanceof Dialog) {
                motion = createMotion(600, 1100, 200);
                motion.start();
                motion2 = createMotion(100, 255, 300);
                motion2.start();
                pulseState = 0;
                Component c = getDialogParent(getDestination());
                originalX = c.getX();
                originalY = c.getY();
                originalWidth = c.getWidth();
                originalHeight = c.getHeight();
                return;
            }
            motion = createMotion(0, 0, 0);
            pulseState = (byte)3;
            return;
        }

        if(Display.getInstance().areMutableImagesFast() || transitionType == TYPE_TIMELINE) {
            if (buffer == null) {
                buffer = createMutableImage(w, h);
            } else {
                // this might happen when screen orientation changes or a MIDlet moves
                // to an external screen
                if(buffer.getWidth() != w || buffer.getHeight() != h) {
                    buffer = createMutableImage(w, h);
                    rgbBuffer = null;

                    // slide motion might need resetting since screen size is different
                    motion = null;
                }
            }
        }

        if(transitionType == TYPE_FADE) {
            motion = createMotion(0, 256, speed);
            motion.start();
            
            if(Display.getInstance().areMutableImagesFast()) {
                Graphics g = buffer.getGraphics();
                g.translate(-source.getAbsoluteX(), -source.getAbsoluteY());

                if(getSource().getParent() != null){
                    getSource().getComponentForm().paintComponent(g);
                }
                getSource().paintBackgrounds(g);
                g.setClip(0, 0, buffer.getWidth()+source.getAbsoluteX(), buffer.getHeight()+source.getAbsoluteY());
                paint(g, getDestination(), 0, 0);
                rgbBuffer = new RGBImage(buffer.getRGBCached(), buffer.getWidth(), buffer.getHeight());

                paint(g, getSource(), 0, 0);
                g.translate(source.getAbsoluteX(), source.getAbsoluteY());
            }
            return;
        }
        

        if(transitionType == TYPE_TIMELINE) {
            Graphics g = buffer.getGraphics();
            g.translate(-source.getAbsoluteX(), -source.getAbsoluteY());

            g.setClip(0, 0, buffer.getWidth()+source.getAbsoluteX(), buffer.getHeight()+source.getAbsoluteY());

            if(timeline.getWidth() != buffer.getWidth() || timeline.getHeight() != buffer.getHeight()) {
                timeline = timeline.scaled(buffer.getWidth(), buffer.getHeight());
            }

            if(timeline instanceof Timeline) {
                ((Timeline)timeline).setTime(0);
                ((Timeline)timeline).setLoop(false);
                ((Timeline)timeline).setAnimationDelay(0);
            }

            paint(g, getDestination(), 0, 0);
            g.translate(source.getAbsoluteX(), source.getAbsoluteY());
            return;
        }

        if (transitionType == TYPE_SLIDE || transitionType == TYPE_FAST_SLIDE) {
            int dest;
            int startOffset = 0;
            boolean direction = forward;
            if ( (source.getUIManager().getLookAndFeel().isRTL())) {
                    direction=!direction;
            }
            if (slideType == SLIDE_HORIZONTAL) {
                dest = w;
                if(destination instanceof Dialog) {
                    startOffset = w - getDialogParent(destination).getWidth();
                    if(direction) {
                        startOffset -= getDialogParent(destination).getStyle().getMargin(destination.isRTL(), Component.LEFT);
                    } else {
                        startOffset -= getDialogParent(destination).getStyle().getMargin(destination.isRTL(), Component.RIGHT);
                    }
                } else {
                    if(source instanceof Dialog) {
                        dest = getDialogParent(source).getWidth();
                        if(direction) {
                            dest += getDialogParent(source).getStyle().getMargin(source.isRTL(), Component.LEFT);
                        } else {
                            dest += getDialogParent(source).getStyle().getMargin(source.isRTL(), Component.RIGHT);
                        }
                    }
                }
            } else {
                dest = h;
                if(destination instanceof Dialog) {
                    startOffset = h - getDialogParent(destination).getHeight() -
                        getDialogTitleHeight((Dialog)destination);
                    if(direction) {
                        startOffset -= getDialogParent(destination).getStyle().getMargin(false, Component.BOTTOM);
                    } else {
                        startOffset -= getDialogParent(destination).getStyle().getMargin(false, Component.TOP);
                        startOffset -= ((Dialog)destination).getTitleStyle().getMargin(false, Component.TOP);
                        if(!drawDialogMenu && ((Dialog)destination).getCommandCount() > 0) {
                            Container p = ((Dialog)destination).getSoftButton(0).getParent();
                            if(p != null) {
                                startOffset -= p.getHeight();
                            }
                        }
                    }
                } else {
                    if(source instanceof Dialog) {
                        dest = getDialogParent(source).getHeight() +
                            getDialogTitleHeight((Dialog)source);
                        if(direction) {
                            dest += getDialogParent(source).getStyle().getMargin(false, Component.BOTTOM);
                        } else {
                            dest += getDialogParent(source).getStyle().getMargin(false, Component.TOP);
                            dest += ((Dialog)source).getTitleStyle().getMargin(false, Component.TOP);
                            if(((Dialog)source).getCommandCount() > 0) {
                                Container p = ((Dialog)source).getSoftButton(0).getParent();
                                if(p != null) {
                                    dest += p.getHeight();
                                }
                            }
                        }
                    }
                }
            }

            motion = createMotion(startOffset, dest, speed);

            if(!Display.getInstance().areMutableImagesFast()) {
                motion.start();
                return;
            }
            
            // make sure the destination is painted fully at least once
            // we must use a full buffer otherwise the clipping will take effect
            Graphics g = buffer.getGraphics();

            // If this is a dialog render the tinted frame once since
            // tinting is expensive
            if(getSource() instanceof Dialog) {
                paint(g, getDestination(), 0, 0);
                if(transitionType == TYPE_FAST_SLIDE && !(destination instanceof Dialog)) {
                    Dialog d = (Dialog)source;
                    secondaryBuffer = createMutableImage(getDialogParent(d).getWidth(),
                            getDialogParent(d).getHeight() +
                            getDialogTitleHeight(d));
                    drawDialogCmp(secondaryBuffer.getGraphics(), d);
                }
            } else {
                if(getDestination() instanceof Dialog) {
                    paint(g, getSource(), 0, 0);
                    if(transitionType == TYPE_FAST_SLIDE && !(source instanceof Dialog)) {
                        Dialog d = (Dialog)destination;
                        secondaryBuffer = createMutableImage(getDialogParent(d).getWidth(),
                                d.getContentPane().getParent().getHeight() +
                                getDialogTitleHeight(d));
                        drawDialogCmp(secondaryBuffer.getGraphics(), d);
                    }
                } else {
                    paint(g, source, -source.getAbsoluteX(), -source.getAbsoluteY());
                    if(transitionType == TYPE_FAST_SLIDE) {
                        secondaryBuffer = createMutableImage(destination.getWidth(), destination.getHeight());
                        paint(secondaryBuffer.getGraphics(), destination, -destination.getAbsoluteX(), -destination.getAbsoluteY());
                    }
                }
            }
            motion.start();
        }
        
    }

    private Image createMutableImage(int w, int h) {
        Display d = Display.getInstance();
        return Image.createImage(Math.min(d.getDisplayWidth(), w), Math.min(d.getDisplayHeight(), h));
    }

    /**
     * This method can be overriden by subclasses to create their own motion object on the fly
     *
     * @param startOffset the start offset for the menu
     * @param dest the destination of the motion
     * @param speed the speed of the motion
     * @return a motion instance
     */
    protected Motion createMotion(int startOffset, int dest, int speed) {
        if(motionSetManually) {
            return motion;
        }
        if(linearMotion) {
            return Motion.createLinearMotion(startOffset, dest, speed);
        }

        return Motion.createEaseInOutMotion(startOffset, dest, speed);
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        if(timeline != null) {
            boolean val = timeline.animate();
            return val;
        }
        if(motion == null) {
            return false;
        }
        position = motion.getValue();
        
        // after the motion finished we need to paint one last time otherwise
        // there will be a "bump" in sliding
        if(firstFinished) {
            return false;
        }
        boolean finished = motion.isFinished();
        if(finished) {
            if(transitionType == TYPE_PULSATE_DIALOG) {
                switch(pulseState) {
                    case 0:
                        pulseState = 1;
                        motion = createMotion(1100, 900, 90);
                        motion.start();
                        return true;
                    case 1:
                        pulseState = 2;
                        motion = createMotion(900, 1000, 180);
                        motion.start();
                        return true;
                }
            }
            if(!firstFinished) {
                firstFinished = true;
            }
        }
        return true;
    }
    
    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        try {
            switch (transitionType) {
                case TYPE_SLIDE:
                    // if this is an up or down slide
                    if (slideType == SLIDE_HORIZONTAL) {
                        paintSlideAtPosition(g, position, 0);
                    } else {
                        paintSlideAtPosition(g, 0, position);
                    }
                    return;
                case TYPE_FAST_SLIDE:
                    // if this is an up or down slide
                    if (slideType == SLIDE_HORIZONTAL) {
                        paintFastSlideAtPosition(g, position, 0);
                    } else {
                        paintFastSlideAtPosition(g, 0, position);
                    }
                    return;
                case TYPE_FADE:
                    paintAlpha(g);
                    return;
                case TYPE_TIMELINE:
                    Object mask = timeline.createMask();
                    paint(g, getSource(), 0, 0);
                    g.drawImage(buffer.applyMask(mask), 0, 0);
                    return;
                case TYPE_SLIDE_AND_FADE: {
                    Form sourceForm = (Form)getSource();
                    Form destForm = (Form)getDestination();
                    int alpha = position;
                    int slidePos = motion2.getValue();
                    int clipX = g.getClipX();
                    int clipY = g.getClipY();
                    int clipW = g.getClipWidth();
                    int clipH = g.getClipHeight();
                    g.translate(0, sourceForm.getTitleArea().getHeight());
                    Container sourcePane = ((Form)getSource()).getContentPane();
                    Container destPane = ((Form)getDestination()).getContentPane();
                    if(forward) {
                        g.translate(slidePos, 0);
                        paint(g, sourcePane, -sourcePane.getAbsoluteX() -sourcePane.getScrollX(), -sourcePane.getAbsoluteY() -sourcePane.getScrollY(), true);
                        g.translate(-destPane.getWidth(), 0);
                        paint(g, destPane, -destPane.getAbsoluteX() -destPane.getScrollX(), -destPane.getAbsoluteY() -destPane.getScrollY(), true);
                        g.translate(destPane.getWidth() - slidePos, 0);
                    } else {
                        g.translate(-slidePos, 0);
                        paint(g, sourcePane, -sourcePane.getAbsoluteX() -sourcePane.getScrollX(), -sourcePane.getAbsoluteY() -sourcePane.getScrollY(), true);
                        g.translate(destPane.getWidth(), 0);
                        paint(g, destPane, -destPane.getAbsoluteX() -destPane.getScrollX(), -destPane.getAbsoluteY() -destPane.getScrollY(), true);
                        g.translate(slidePos - destPane.getWidth(), 0);
                    }
                    g.translate(0, -sourceForm.getTitleArea().getHeight());
                    g.setClip(clipX, clipY, clipW, clipH);

                    sourceForm.getTitleArea().paintBackground(g);
                    paintShiftFadeHierarchy(sourceForm.getTitleArea(), 255 - alpha, g, false);
                    paintShiftFadeHierarchy(destForm.getTitleArea(), alpha, g, true);
                    return;
                }
                case TYPE_PULSATE_DIALOG:
                    paint(g, getSource(), 0, 0);
                    int alpha = g.getAlpha();
                    g.setAlpha(motion2.getValue());

                    Component c = getDialogParent(getDestination());
                    float ratio = ((float)position) / 1000.0f;
                    if(g.isAffineSupported()) {
                        g.scale(ratio, ratio);
                        int w = (int)(originalWidth * ratio);
                        int h = (int)(originalHeight * ratio);
                        c.setX(originalX + ((originalWidth - w) / 2));
                        c.setY(originalY + ((originalHeight - h) / 2));
                        paint(g, c, 0, 0);
                        g.resetAffine();
                    } else {
                        c.setWidth((int)(originalWidth * ratio));
                        c.setHeight((int)(originalHeight * ratio));
                        c.setX(originalX + ((originalWidth - c.getWidth()) / 2));
                        c.setY(originalY + ((originalHeight - c.getHeight()) / 2));
                        paint(g, c, 0, 0);
                    }
                    g.setAlpha(alpha);
                    return;
            }
        } catch(Throwable t) {
            System.out.println("An exception occurred during transition paint this might be valid in case of a resize in the middle of a transition");
            t.printStackTrace();
        }
    }

    private void paintShiftFadeHierarchy(Container c, int alpha, Graphics g, boolean incoming) {
        int componentCount = c.getComponentCount();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = c.getComponentAt(iter);
            if(current instanceof Container) {
                paintShiftFadeHierarchy((Container)current, alpha, g, incoming);
                continue;
            }
            g.setAlpha(alpha);
            Motion m = getComponentShiftMotion(current, incoming);
            int tval = m.getValue();
            g.translate(tval, 0);
            current.paintComponent(g, false);
            g.translate(-tval, 0);
            g.setAlpha(255);
        }
    }

    private Motion getComponentShiftMotion(Component c, boolean incoming) {
        Motion m = (Motion)c.getClientProperty("$shm");
        if(m == null) {
            int travelDestination = getDestination().getWidth() - c.getWidth() - c.getAbsoluteX();
            if(getDestination().getWidth() - c.getWidth() < 10) {
                // big component that takes up all the space such as a title that occupies the entire title area
                travelDestination = c.getWidth() / 2 - c.getPreferredW() / 2;
            }
            if(incoming) {
                if(forward) {
                    m = Motion.createEaseInOutMotion(-travelDestination, 0, speed);
                } else {
                    m = Motion.createEaseInOutMotion(travelDestination, 0, speed);
                }
            } else {
                if(forward) {
                    m = Motion.createEaseInOutMotion(0, travelDestination, speed);
                } else {
                    m = Motion.createEaseInOutMotion(0, -travelDestination, speed);
                }
            }
            m.start();
            c.putClientProperty("$shm", m);
        }
        return m;
    }

    private void paintAlpha(Graphics graphics) {
        Component src = getSource();
        int w = src.getWidth();
        int h = src.getHeight();
        int position = this.position;
        if (position > 255) {
            position = 255;
        } else {
            if (position < 0) {
                position = 0;
            }
        }
        // for slow mutable images
        if(buffer == null) {
            paint(graphics, src, 0, 0);
            Component dest = getDestination();
            dest.setX(src.getX());
            dest.setY(src.getY());
            dest.setWidth(src.getWidth());
            dest.setHeight(src.getHeight());
            graphics.setAlpha(position);
            paint(graphics, dest, 0, 0);
            graphics.setAlpha(255);
            return;
        }
        // this will always be invoked on the EDT so there is no race condition risk
        if(rgbBuffer != null || secondaryBuffer != null) {
            if(secondaryBuffer != null) {
                Component dest = getDestination();                
                int x = dest.getAbsoluteX();
                int y = dest.getAbsoluteY();

                graphics.drawImage(buffer, x, y);
                graphics.setAlpha(position);
                graphics.drawImage(secondaryBuffer, x, y);
                graphics.setAlpha(0xff);
            } else {
                int alpha = position << 24;
                int size = w * h;
                int[] bufferArray = rgbBuffer.getRGB();
                for (int iter = 0 ; iter < size ; iter++) {
                    bufferArray[iter] = ((bufferArray[iter] & 0xFFFFFF) | alpha);
                }
                Component dest = getDestination();                
                int x = dest.getAbsoluteX();
                int y = dest.getAbsoluteY();
                graphics.drawImage(buffer, x, y);
                graphics.drawImage(rgbBuffer, x, y);
            }
        } 
    }

    private void removeConstant(Container c) {
        int componentCount = c.getComponentCount();
        c.putClientProperty("$shm", null);
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component cmp = c.getComponentAt(iter);
            cmp.putClientProperty("$shm", null);
            if(cmp instanceof Container) {
                removeConstant((Container)cmp);
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void cleanup() {
        if(transitionType == TYPE_SLIDE_AND_FADE) {
            Component c = getSource();
            if(c instanceof Container){
                removeConstant((Container)c);
            }
            c = getDestination();
            if(c instanceof Container){
                removeConstant((Container)c);
            }
        }
        super.cleanup();
        buffer = null;
        rgbBuffer = null;
        secondaryBuffer = null;
        timeline = null;
    }

    private void paintSlideAtPosition(Graphics g, int slideX, int slideY) {
        Component source = getSource();
        
        // if this is the first form we can't do a slide transition since we have no source form
        if (source == null) { 
            return;           
        }
        
        Component dest = getDestination();                
        int w = source.getWidth();
        int h = source.getHeight();
                    
        if (slideType == SLIDE_HORIZONTAL) {
            h = 0;
        } else {
            w = 0;
        }

        if(forward) {
            w = -w;
            h = -h;
        } else {
            slideX = -slideX;
            slideY = -slideY;
        }
        g.setClip(source.getAbsoluteX()+source.getScrollX(), source.getAbsoluteY()+source.getScrollY(), source.getWidth(), source.getHeight());
            
        // dialog animation is slightly different... 
        if(source instanceof Dialog) {
            if(buffer != null) {
                g.drawImage(buffer, 0, 0);
            } else {
                paint(g, dest, 0, 0);
            }
            paint(g, source, -slideX, -slideY);
            return;
        } 
        
        if(dest instanceof Dialog) {
            if(buffer != null) {
                g.drawImage(buffer, 0, 0);
            } else {
                paint(g, source, 0, 0);
            }
            paint(g, dest, -slideX - w, -slideY - h);
            return;
        } 
        
        if(source.getParent() != null || buffer == null) {
            source.paintBackgrounds(g);
            paint(g, source, slideX , slideY );
        } else {
            g.drawImage(buffer, slideX, slideY);        
        }
        paint(g, dest, slideX + w, slideY + h);
        
    }

    private void paintFastSlideAtPosition(Graphics g, int slideX, int slideY) {
        if(secondaryBuffer != null) {
            Component source = getSource();

            // if this is the first form we can't do a slide transition since we have no source form
            if (source == null) {
                return;
            }

            Component dest = getDestination();

            int w = buffer.getWidth();
            int h = buffer.getHeight();

            if (slideType == SLIDE_HORIZONTAL) {
                h = 0;
            } else {
                w = 0;
            }

            if(forward) {
                w = -w;
                h = -h;
            } else {
                slideX = -slideX;
                slideY = -slideY;
            }
            g.setClip(source.getAbsoluteX()+source.getScrollX(), source.getAbsoluteY()+source.getScrollY(), source.getWidth(), source.getHeight());

            // dialog animation is slightly different...
            if(source instanceof Dialog) {
                g.drawImage(buffer, 0, 0);
                slideX -= getDialogParent(source).getX();
                slideY -= getDialogParent(source).getY();
                g.drawImage(secondaryBuffer, -slideX, -slideY);
                return;
            }

            if(dest instanceof Dialog) {
                g.drawImage(buffer, 0, 0);
                slideY -= getDialogParent(dest).getY();
                slideX -= getDialogParent(dest).getX();
                g.drawImage(secondaryBuffer, -slideX - w, -slideY - h);
                return;
            }

            g.drawImage(buffer, slideX, slideY);
            g.drawImage(secondaryBuffer, slideX + w, slideY + h);

        } else {
            paintSlideAtPosition(g, slideX, slideY);
        }

    }

    private int getDialogTitleHeight(Dialog d) {
        return 0;
    }

    private void drawDialogCmp(Graphics g, Dialog dlg) {
        Painter p = dlg.getStyle().getBgPainter();
        dlg.getStyle().setBgPainter(null);
        g.setClip(0, 0, dlg.getWidth(), dlg.getHeight());
        g.translate(-getDialogParent(dlg).getX(), -getDialogParent(dlg).getY() + getDialogTitleHeight(dlg));
        getDialogParent(dlg).paintComponent(g, false);
        if(drawDialogMenu && dlg.getCommandCount() > 0) {
            Component menuBar = dlg.getSoftButton(0).getParent();
            if(menuBar != null) {
                g.setClip(0, 0, dlg.getWidth(), dlg.getHeight());
                menuBar.paintComponent(g, false);
            }
        }

        dlg.getStyle().setBgPainter(p);
    }

    private void paint(Graphics g, Component cmp, int x, int y) {
        paint(g, cmp, x, y, false);
    }

    private void paint(Graphics g, Component cmp, int x, int y, boolean background) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        if(cmp instanceof Dialog) {
            if(transitionType != TYPE_FADE) {
                if(!(getSource() instanceof Dialog && getDestination() instanceof Dialog && 
                        cmp == getDestination())) {
                    Painter p = cmp.getStyle().getBgPainter();
                    cmp.getStyle().setBgPainter(null);
                    g.translate(x, y);
                    Dialog dlg = (Dialog)cmp;
                    g.setClip(0, 0, cmp.getWidth(), cmp.getHeight());
                    getDialogParent(dlg).paintComponent(g, false);
                    g.translate(-x, -y);
                    if(drawDialogMenu && dlg.getCommandCount() > 0) {
                        Component menuBar = dlg.getSoftButton(0).getParent();
                        if(menuBar != null) {
                            g.setClip(0, 0, cmp.getWidth(), cmp.getHeight());
                            menuBar.paintComponent(g, false);
                        }
                    }

                    g.setClip(cx, cy, cw, ch);
                    cmp.getStyle().setBgPainter(p);
                    return;
                }
            } 
            cmp.paintComponent(g, background);
            return;
        }
        //g.clipRect(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
         g.translate(x, y);
        //g.clipRect(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
        cmp.paintComponent(g, background);
         g.translate(-x, -y);
        
        g.setClip(cx, cy, cw, ch);
    }
    
    /**
     * Motion represents the physical movement within a transition, it can
     * be replaced by the user to provide a more appropriate physical feel
     * 
     * @return the instanceo of the motion class used by this transition
     */
    public Motion getMotion() {
        return motion;
    }

    /**
     * Motion represents the physical movement within a transition, it can
     * be replaced by the user to provide a more appropriate physical feel
     * 
     * @param motion new instance of the motion class that will be used by the transition
     */
    public void setMotion(Motion motion) {
        motionSetManually = true;
        this.motion = motion;
    }
    
    
    /**
     * @inheritDoc
     */
    public Transition copy(boolean reverse){
        CommonTransitions retVal = null;
        switch(transitionType) {
            case TYPE_TIMELINE:
                retVal = CommonTransitions.createTimeline(timeline);
                break;
            case TYPE_FADE:
                retVal = CommonTransitions.createFade(speed);
                break;
            case TYPE_SLIDE: {
                boolean fwd=forward;

                if(reverse) {
                    retVal = CommonTransitions.createSlide(slideType, !fwd, speed, drawDialogMenu);
                } else {
                    retVal = CommonTransitions.createSlide(slideType, fwd, speed, drawDialogMenu);
                }
                break;
            }
            case TYPE_SLIDE_AND_FADE: {
                boolean fwd=forward;
                if(reverse) {
                    retVal = CommonTransitions.createSlideFadeTitle(!fwd, speed);
                } else {
                    retVal = CommonTransitions.createSlideFadeTitle(fwd, speed);
                }
                break;
            }
            case TYPE_FAST_SLIDE: {
                boolean fwd=forward;

                if(reverse) {
                    retVal = CommonTransitions.createFastSlide(slideType, !fwd, speed, drawDialogMenu);
                } else {
                    retVal = CommonTransitions.createFastSlide(slideType, fwd, speed, drawDialogMenu);
                }
                break;
            }
            case TYPE_EMPTY:
                retVal = CommonTransitions.createEmpty();
                break;
            case TYPE_PULSATE_DIALOG:
                retVal = createDialogPulsate();
                break;
        }
        retVal.linearMotion = linearMotion;
        return retVal;
    }

    /**
     * Indicates whether the motion associated with this transition is linear or spline motion
     *
     * @return the linearMotion
     */
    public boolean isLinearMotion() {
        return linearMotion;
    }

    /**
     * Indicates whether the motion associated with this transition is linear or spline motion
     *
     * @param linearMotion the linearMotion to set
     */
    public void setLinearMotion(boolean linearMotion) {
        this.linearMotion = linearMotion;
    }

    /**
     * Indicates whether the motion associated with these transitions by default is linear or spline motion
     *
     * @return the defaultLinearMotion
     */
    public static boolean isDefaultLinearMotion() {
        return defaultLinearMotion;
    }

    /**
     * Indicates whether the motion associated with these transitions by default is linear or spline motion
     *
     * @param aDefaultLinearMotion the defaultLinearMotion to set
     */
    public static void setDefaultLinearMotion(boolean aDefaultLinearMotion) {
        defaultLinearMotion = aDefaultLinearMotion;
    }
}
