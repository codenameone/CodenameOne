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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.plaf.Style;

/**
 * <p>A Transitions that animates the destination component as a growing window
 * until the destination is displayed in place.
 * The Bubble window can be round on supported platforms
 * </p>
 * 
 * @author Chen
 */
public class BubbleTransition extends Transition {

    private Component origin;
    private Image destBuffer;
    private Motion clipMotion;
    private Motion locMotionX;
    private Motion locMotionY;
    private int duration = 200;
    private int clipSize;
    private int x;
    private int y;
    private String componentName;
    private boolean roundBubble = true;
    
    
    /**
     * Creates a Bubble Transition
     */ 
    public BubbleTransition() {
    }

    /**
     * Creates a Bubble Transition
     * @param duration the duration of the transition
     */ 
    public BubbleTransition(int duration) {
        this.duration = duration;
    }
    
    /**
     * Creates a Bubble Transition
     * 
     * @param duration the duration of the transition
     * @param componentName the name of the component from the source Form that 
     * this transition should start from.
     */ 
    public BubbleTransition(int duration, String componentName) {
        this(duration);
        this.componentName = componentName;
    }

    /**
     * the name of the component from the source Form that 
     * this transition should start from.
     * @param componentName name of the component to start the transition from
     */ 
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
    
    @Override
    public void initTransition() {

        Component source = getSource();
        Component destination = getDestination();
        int w = source.getWidth();
        int h = source.getHeight();

        // a transition might occur with illegal source or destination values (common with 
        // improper replace() calls, this may still be valid and shouldn't fail
        if (w <= 0 || h <= 0) {
            return;
        }
        
        Display d = Display.getInstance();
        if (getDestination() instanceof Dialog) {
            Dialog dlg = (Dialog) destination;
            // transparent image!
            destBuffer = Image.createImage(Math.min(d.getDisplayWidth(), getDialogParent(dlg).getWidth()),
                    Math.min(d.getDisplayHeight(), dlg.getContentPane().getParent().getHeight()
                    ), 0);
            Graphics g = destBuffer.getGraphics();
            Style stl = dlg.getDialogComponent().getStyle();
            byte bgt = stl.getBgTransparency();
            stl.setBgTransparency(0xff);
            drawDialogCmp(destBuffer.getGraphics(), dlg);
            stl.setBgTransparency(bgt & 0xff, true);
            
        } else if (getSource() instanceof Dialog) {
            Dialog dlg = (Dialog) source;
            // transparent image!
            destBuffer = Image.createImage(Math.min(d.getDisplayWidth(), getDialogParent(dlg).getWidth()),
                    Math.min(d.getDisplayHeight(), dlg.getContentPane().getParent().getHeight()
                    ), 0);
            Graphics g = destBuffer.getGraphics();
            Style stl = dlg.getDialogComponent().getStyle();
            byte bgt = stl.getBgTransparency();
            stl.setBgTransparency(0xff);
            drawDialogCmp(destBuffer.getGraphics(), dlg);
            stl.setBgTransparency(bgt & 0xff, true);

        } else {
            destBuffer = createMutableImage(destination.getWidth(), destination.getHeight());
            paint(destBuffer.getGraphics(), destination, -destination.getAbsoluteX(), -destination.getAbsoluteY());
        }
        Component dest = getDestination();
        if (dest instanceof Dialog) {
            dest = getDialogParent(dest);
        }
        Component src = getSource();
        if (src instanceof Dialog) {
            src = getDialogParent(src);
        }

        Form sourceForm = source.getComponentForm();
        origin = findByName(sourceForm, componentName);
        
        if(origin != null){
            locMotionX = Motion.createLinearMotion(origin.getAbsoluteX() + origin.getWidth()/2 - dest.getWidth()/2, dest.getAbsoluteX(), duration);
            locMotionX.start();
            locMotionY = Motion.createLinearMotion(origin.getAbsoluteY() + origin.getHeight()/2 - dest.getHeight()/2, dest.getAbsoluteY(), duration);
            locMotionY.start();
            clipMotion = Motion.createLinearMotion(Math.min(origin.getWidth(), origin.getHeight()), Math.max(dest.getWidth(), dest.getHeight())*3/2, duration);
        }else{
            Form destForm = dest.getComponentForm();
            origin = findByName(destForm, componentName);
            if (origin != null) {
                locMotionX = Motion.createLinearMotion(src.getAbsoluteX(), origin.getAbsoluteX() + origin.getWidth() / 2 - src.getWidth() / 2, duration);
                locMotionX.start();
                locMotionY = Motion.createLinearMotion(src.getAbsoluteY(), origin.getAbsoluteY() + origin.getHeight() / 2 - src.getHeight() / 2, duration);
                locMotionY.start();
                clipMotion = Motion.createLinearMotion(Math.max(src.getWidth(), src.getHeight()) * 3 / 2, Math.min(origin.getWidth(), origin.getHeight()), duration);
            } else {
                clipMotion = Motion.createLinearMotion(0, Math.max(dest.getWidth(), dest.getHeight()) * 3 / 2, duration);
            }
        }
        clipMotion.start();

    }

    @Override
    public boolean animate() {
        clipSize = clipMotion.getValue();
        if(origin != null){
            x = locMotionX.getValue();
            y = locMotionY.getValue();
        }
        return !clipMotion.isFinished();
    }

    @Override
    public void paint(Graphics g) {
        Component source = getSource();
        Component dest = getDestination();
        
        Component srcCmp = source;
        Component destCmp = dest;
        
        if (source instanceof Dialog && dest instanceof Form) {
            srcCmp = dest;
            destCmp = source;
        }
        
        
        paint(g, srcCmp, -srcCmp.getAbsoluteX(), -srcCmp.getAbsoluteY(), true);
        
        int [] clip = g.getClip();

        if(destCmp instanceof Dialog){
            destCmp = getDialogParent(destCmp);
        }
        if(roundBubble && g.isShapeClipSupported()){
            GeneralPath p = new GeneralPath();
            p.arc(x + destCmp.getWidth()/2 - clipSize/2, y + destCmp.getHeight()/2 - clipSize/2, clipSize, clipSize, 0, Math.toRadians(360));
            g.setClip(p);
        }else{
            g.setClip(x + destCmp.getWidth()/2 - clipSize/2, y + destCmp.getHeight()/2 - clipSize/2, clipSize, clipSize);
        }
        g.drawImage(destBuffer, x, y);        
        g.setClip(clip);
    }

    /**
     * Determines if the Bubble is a round circle or a rectangle.
     * Round bubble apply to platforms who supports shaped clipping.
     * See Graphics.isShapeClipSupported().
     * 
     * @param roundBubble true if the bubble should be round
     */ 
    public void setRoundBubble(boolean roundBubble) {
        this.roundBubble = roundBubble;
    }
    
    

    private Image createMutableImage(int w, int h) {
        Display d = Display.getInstance();
        return Image.createImage(Math.min(d.getDisplayWidth(), w), Math.min(d.getDisplayHeight(), h));
    }

    private void paint(Graphics g, Component cmp, int x, int y) {
        paint(g, cmp, x, y, false);
    }

    private void paint(Graphics g, Component cmp, int x, int y, boolean background) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        g.translate(x, y);
        if (cmp instanceof Dialog) {
            ((Dialog) cmp).getDialogComponent().paintComponent(g);
        } else {
            cmp.paintComponent(g, background);
        }
        g.translate(-x, -y);

        g.setClip(cx, cy, cw, ch);
    }

    @Override
    public void cleanup() {
        destBuffer = null;
        origin = null;
    }

    /**
     * The duration for the transition
     *
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * The duration for the transition
     *
     * @param duration the duration to set
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    private void drawDialogCmp(Graphics g, Dialog dlg) {
        Painter p = dlg.getStyle().getBgPainter();
        dlg.getStyle().setBgPainter(null);
        g.setClip(0, 0, dlg.getWidth(), dlg.getHeight());
        g.translate(-getDialogParent(dlg).getX(), -getDialogParent(dlg).getY());
        getDialogParent(dlg).paintComponent(g, false);
        if (dlg.getCommandCount() > 0) {
            Component menuBar = dlg.getSoftButton(0).getParent();
            if (menuBar != null) {
                g.setClip(0, 0, dlg.getWidth(), dlg.getHeight());
                menuBar.paintComponent(g, false);
            }
        }

        dlg.getStyle().setBgPainter(p);
    }

    private Container getDialogParent(Component dlg) {
        return ((Dialog) dlg).getDialogComponent();
    }
    
    
    private static Component findByName(Container root, String componentName) {
        int count = root.getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            Component c = root.getComponentAt(iter);
            String n = c.getName();
            if(n != null && n.equals(componentName)) {
                return c;
            }
            if(c instanceof Container) {
                c = findByName((Container)c, componentName);
                if(c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @param reverse {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Transition copy(boolean reverse) {
        return new BubbleTransition(duration, componentName);
    }
}
