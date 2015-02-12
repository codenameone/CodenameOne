/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * A Transitions that flips between 2 views
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

    /**
     * Creates  a Flip Transition
     */ 
    public FlipTransition() {
    }

    /**
     * Creates  a Flip Transition
     * 
     * @param bgColor the color to paint in the background when the transition 
     * paints
     */ 
    public FlipTransition(int bgColor) {
        this.bgColor = bgColor;
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
            bgColor = 0;
        }
        
        
        
        //flipState = 0f;
        //motion = Motion.createLinearMotion(0, 180, 500);
        //motion.start();
        
        motion = Motion.createLinearMotion(0, 100, 200);
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
                    motion = Motion.createLinearMotion(0, 180, 500);
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
                        motion = Motion.createLinearMotion(100, 0, 200);
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
    
    private Transform makePerspectiveTransform(){
        int x = getSource().getAbsoluteX();
        int y = getSource().getAbsoluteY();
        int w = getSource().getWidth();
        int h = getSource().getHeight();
        float displayH = Display.getInstance().getDisplayHeight();
        float displayW = Display.getInstance().getDisplayWidth();
        //double midX = (float)x+(float)w/2.0;
        //double midY = (float)y+(float)h/2.0;
        double fovy = 0.25;
        return Transform.makePerspective((float)fovy, (float)displayW/(float)displayH, zNear, zFar);
    }
    
    

    @Override
    public void paint(Graphics g) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        int x = getSource().getAbsoluteX();
        int y = getSource().getAbsoluteY();
        int w = getSource().getWidth();
        int h = getSource().getHeight();
        g.setClip(x, y, w, h);
        
        
        
        if (bgColor >= 0) {
            int c = g.getColor();
            g.setColor(0);
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
            
            
            Transform perspectiveT = makePerspectiveTransform();
            float[] bottomRight = perspectiveT.transformPoint(new float[]{displayW, displayH, zNear});
            
            Transform t = Transform.makeTranslation(0,0, 0);
            
            
            float xfactor = -displayW/bottomRight[0];
            float yfactor = -displayH/bottomRight[1];
            
            
            t.scale(xfactor,yfactor,0f);
            t.translate((x+w/2)/xfactor, (y+h/2)/yfactor, 0);
            
            t.concatenate(perspectiveT);
            
            float cameraZ = -zNear-w/2*zState;
            float cameraX = -x-w/2;
            float cameraY = -y-h/2;
            t.translate(cameraX, cameraY, cameraZ);
            
            if ( transitionState == STATE_FLIP){
                t.translate((float)midX, y, 0);
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

                    t.rotate((float)angle, 0, 1, 0);// rotate about y axis
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
                    t.rotate((float)angle, 0, 1, 0);// rotate about y axis
                }
            }
            if ( transitionState == STATE_FLIP ){
                t.translate(-(float)midX, -y, 0);
                if ( flipState >= 0.5f ){
                    // The rotation will leave the destination image flipped
                    // backwards, so we need to transform it to be the 
                    // mirror image
                    t.scale(-1, 1, 1);
                    t.translate(-2*x-w, 0, 0);
                }
            }
            
            Transform oldTransform = g.getTransform();
            g.setTransform(t);
            g.drawImage(img, x, y, w, h);
            g.setTransform(oldTransform);
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

}
