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
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Transition;
import com.codename1.util.MathUtil;

/**
 * <p>A Transitions that flips between 2 components/forms using perspective transform where available.<br>
 * Notice that this looks rather different on devices as perspective transform is available there but isn't
 * on the simulator. 
 * </p>
 * <script src="https://gist.github.com/codenameone/47602e679f61712693bd.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/transition-flip.jpg" alt="Flip" />
 * 
 * @author Chen, Steve
 */
public class FlipTransition extends Transition {

    private static final int STATE_MOVE_AWAY=1;
    private static final int STATE_FLIP=2;
    private static final int STATE_MOVE_CLOSER=3;
    
    // Assume supported optimistically
    // will be switched off in paint.
    private boolean perspectiveSupported = true;
    
    private Image sourceBuffer;
    private Image destBuffer;

    // 0 is front, 1.0 is back
    private float flipState = 0f;

    // 0 is at closest point (fills original bounds).
    // 1 is at farthest point (all of flip will be visible).
    private float zState = 0f;
    private int transitionState = STATE_MOVE_AWAY;
    
    private Motion motion;
    private boolean firstFinished = false;
    private boolean started = false;

    private int bgColor = -1;
    
    private float zNear;
    private float zFar;

    private int duration = 200;
    
    private Transform tmpTransform;
    private Transform perspectiveT;
    private Transform currTransform;
    
    /**
     * Creates  a Flip Transition
     */ 
    public FlipTransition() {
    }

    /**
     * Creates  a Flip Transition
     * 
     * @param bgColor the color to paint in the background when the transition 
     * paints, use -1 to not paint a background color
     */ 
    public FlipTransition(int bgColor) {
        this.bgColor = bgColor;
    }

    /**
     * Creates  a Flip Transition
     * 
     * @param bgColor the color to paint in the background when the transition 
     * paints, use -1 to not paint a background color
     * @param duration the duration of the transition
     */ 
    public FlipTransition(int bgColor, int duration) {
        this.bgColor = bgColor;
        this.duration = duration;
    }
    
    @Override
    public void initTransition() {
        flipState = 0f;
        transitionState = STATE_MOVE_AWAY;
        zNear = 1600;
        zFar = zNear+3000;
        
        Component source = getSource();
        Component destination = getDestination();
        int w = source.getWidth();
        int h = source.getHeight();

        // a transition might occur with illegal source or destination values (common with 
        // improper replace() calls, this may still be valid and shouldn't fail
        if (w <= 0 || h <= 0) {
            return;
        }
        sourceBuffer = createMutableImage(source.getWidth(), source.getHeight());
        paint(sourceBuffer.getGraphics(), source, -source.getAbsoluteX(), -source.getAbsoluteY());

        destBuffer = createMutableImage(destination.getWidth(), destination.getHeight());
        paint(destBuffer.getGraphics(), destination, -destination.getAbsoluteX(), -destination.getAbsoluteY());
        

        if (source instanceof Form) {
            setBgColor(0);
        }
        
        motion = Motion.createLinearMotion(0, 100, duration);
        motion.start();

    }

    @Override
    public boolean animate() {
        int val = motion.getValue();
        switch (transitionState){
            case STATE_MOVE_AWAY: {
                zState = ((float)val)/100f;
                if ( motion.isFinished() || !perspectiveSupported){
                    transitionState = STATE_FLIP;
                    motion = Motion.createLinearMotion(0, 180, duration);
                    motion.start();
                }
                return true;
            }
            
            case STATE_FLIP: {
                double valInRadians = Math.PI / 180f * (double) val;
                double projectedPos = Math.cos(valInRadians);

                flipState = (float) ((-projectedPos) / 2.0 + 0.5);
                if (motion.isFinished()) {
                    transitionState = STATE_MOVE_CLOSER;
                    if ( perspectiveSupported ){
                        motion = Motion.createLinearMotion(100, 0, duration);
                        motion.start();
                    } else {
                        return false;
                    }
                }
                return true;
            }
            
            case STATE_MOVE_CLOSER: {
                zState = ((float)val)/100f;
                return !motion.isFinished();
                
            }
            default:
                throw new RuntimeException("Invalid transition state");
                
               
        }
        
    }
    
    private void makePerspectiveTransform(Transform t){
        int x = getSource().getAbsoluteX();
        int y = getSource().getAbsoluteY();
        int w = getSource().getWidth();
        int h = getSource().getHeight();
        float displayH = Display.getInstance().getDisplayHeight();
        float displayW = Display.getInstance().getDisplayWidth();
        //double midX = (float)x+(float)w/2.0;
        //double midY = (float)y+(float)h/2.0;
        double fovy = 0.25;
        
        t.setPerspective((float)fovy, (float)displayW/(float)displayH, zNear, zFar);
    }
    
    

    @Override
    public void paint(Graphics g) {
        // this can happen if a transition is cut short
        if(destBuffer == null) {
            return;
        }
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        int x = getSource().getAbsoluteX();
        int y = getSource().getAbsoluteY();
        int w = getSource().getWidth();
        int h = getSource().getHeight();
        g.setClip(x, y, w, h);
        
        
        
        if (getBgColor() >= 0) {
            int c = g.getColor();
            g.setColor(getBgColor());
            g.fillRect(x, y, w, h);
            g.setColor(c);
        } else {
            getSource().paintBackgrounds(g);
        }

        if ( g.isPerspectiveTransformSupported()){
            float displayH = Display.getInstance().getDisplayHeight();
            float displayW = Display.getInstance().getDisplayWidth();
            double midX = (float)x+(float)w/2.0;
            //double midY = (float)y+(float)h/2.0;
            
            if (perspectiveT == null) {
                perspectiveT = Transform.makeIdentity();
            }
            makePerspectiveTransform(perspectiveT);
            float[] bottomRight = perspectiveT.transformPoint(new float[]{displayW, displayH, zNear});
            
            if (currTransform == null) {
                currTransform = Transform.makeTranslation(0,0, 0);
            } else {
                currTransform.setIdentity();
            }
            
            
            float xfactor = -displayW/bottomRight[0];
            float yfactor = -displayH/bottomRight[1];
            
            
            currTransform.scale(xfactor,yfactor,0f);
            currTransform.translate((x+w/2)/xfactor, (y+h/2)/yfactor, 0);
            
            currTransform.concatenate(perspectiveT);
            
            float cameraZ = -zNear-w/2*zState;
            float cameraX = -x-w/2;
            float cameraY = -y-h/2;
            currTransform.translate(cameraX, cameraY, cameraZ);
            
            if ( transitionState == STATE_FLIP){
                currTransform.translate((float)midX, y, 0);
            }
            
            Image img = null;
            if ( flipState < 0.5 ){
                img = sourceBuffer;
                if ( transitionState == STATE_FLIP){
                    // We are showing the front image
                    // We will rotate it up to 90 degrees
                    // 0 -> 0 degrees
                    // 0.5 -> 90 degress
                    double sin = flipState * 2.0;
                    double angle = MathUtil.asin(sin);

                    currTransform.rotate((float)angle, 0, 1, 0);// rotate about y axis
                }
            } else {
                img = destBuffer;
                if ( transitionState == STATE_FLIP){
                    // We are showing the back image
                    // We are showing the back of the image
                    //  We will rotate it from 90 degrees back to 0
                    // 0.5 -> 90 degrees
                    // 1.0 -> 0 degrees
                    double sin = (1.0-flipState)*2.0;
                    double angle = Math.PI-MathUtil.asin(sin);
                    currTransform.rotate((float)angle, 0, 1, 0);// rotate about y axis
                }
            }
            if ( transitionState == STATE_FLIP ){
                currTransform.translate(-(float)midX, -y, 0);
                if ( flipState >= 0.5f ){
                    // The rotation will leave the destination image flipped
                    // backwards, so we need to transform it to be the 
                    // mirror image
                    currTransform.scale(-1, 1, 1);
                    currTransform.translate(-2*x-w, 0, 0);
                }
            }
            if (tmpTransform == null) {
                tmpTransform = Transform.makeIdentity();
            }
            g.getTransform(tmpTransform);
            g.setTransform(currTransform);
            g.drawImage(img, x, y, w, h);
            g.setTransform(tmpTransform);
        } else {
            perspectiveSupported = false;
            if (flipState < 0.5) {
                int frontX = x + (int) (flipState * (float) w);
                int frontWidth = (int) ((float) w * (1.0 - flipState * 2.0));
                g.drawImage(sourceBuffer, frontX, y, frontWidth, h);
            } else {
                double backState = 1.0 - flipState;
                int backX = x + (int) (backState * (float) w);
                int backWidth = (int) ((float) w * (1.0 - backState * 2.0));
                g.drawImage(destBuffer, backX, y, backWidth, h);
            }
        }
        g.setClip(cx, cy, cw, ch);
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
        cmp.paintComponent(g, background);
        g.translate(-x, -y);

        g.setClip(cx, cy, cw, ch);
    }

    public void cleanup() {
        sourceBuffer = null;
        destBuffer = null;
        
    }

    /**
     * The duration for the flip transition
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * The duration for the flip transition
     * @param duration the duration to set
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * The background color that is painted behind the flipping effect or -1 to use the paintBackgrounds method instead
     * @return the bgColor
     */
    public int getBgColor() {
        return bgColor;
    }

    /**
     * The background color that is painted behind the flipping effect or -1 to use the paintBackgrounds method instead
     * @param bgColor the bgColor to set
     */
    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
    }

    /**
     * {@inheritDoc}
     * @param reverse {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Transition copy(boolean reverse) {
        return new FlipTransition(bgColor, duration);
    }

}
