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

import static com.codename1.ui.MultipleGradientPaint.CycleMethod.REFLECT;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.geom.Shape;
import com.codename1.util.MathUtil;

/**
 * LinearGradientPaint provides a way to fill a {@link Shape} with a linear gradient.  
 * @author shannah
 * @since 7.0
 * @see Graphics#setColor(com.codename1.ui.Paint) 
 */
public class LinearGradientPaint extends MultipleGradientPaint {
    private double startX, startY, endX, endY;
    private Transform t = Transform.makeIdentity(), t2 = Transform.makeIdentity();
    
    /**
     * Creates a LinearGradientPaint with the specified settings.
     * @param startX The startX coordinate of the gradient in user space.
     * @param startY The startY coordinate of the gradient in user space.
     * @param endX The endX coordinate of the gradient in user space.
     * @param endY THe endY coordinate of the gradient in user space.
     * @param fractions Fractional positions of where gradient colors begin.  Each value should be between 0 and 1.
     * @param colors The colors to use in the gradient.  There should be the same number of colors as there are fractions.
     * @param cycleMethod The cycle method to use.
     * @param colorSpace The color space to use.
     * @param gradientTransform Transform to use for the gradient.  Not used right now.
     */
    public LinearGradientPaint(float startX, float startY, float endX, float endY, float[] fractions, int[] colors, MultipleGradientPaint.CycleMethod cycleMethod, MultipleGradientPaint.ColorSpaceType colorSpace, Transform gradientTransform) {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }
    
    /**
     * Creates a LinearGradientPaint with the specified settings.
     * @param startX The startX coordinate of the gradient in user space.
     * @param startY The startY coordinate of the gradient in user space.
     * @param endX The endX coordinate of the gradient in user space.
     * @param endY THe endY coordinate of the gradient in user space.
     * @param fractions Fractional positions of where gradient colors begin.  Each value should be between 0 and 1.
     * @param colors The colors to use in the gradient.  There should be the same number of colors as there are fractions.
     * @param cycleMethod The cycle method to use.
     * @param colorSpace The color space to use.
     * @param gradientTransform Transform to use for the gradient.  Not used right now.
     */
    public LinearGradientPaint(double startX, double startY, double endX, double endY, float[] fractions, int[] colors, MultipleGradientPaint.CycleMethod cycleMethod, MultipleGradientPaint.ColorSpaceType colorSpace, Transform gradientTransform) {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    /**
     * Paints linear gradient in the given bounds.
     * @param g
     * @param bounds 
     */
    @Override
    public final void paint(Graphics g, Rectangle2D bounds) {
        paint(g, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    
    private double insetStartLength(double x, double y, double w, double h) {
        
        
        return Math.max(w, h);
    }
    
    private double insetEndLength(double x, double y, double w, double h) {
        return Math.max(w, h);
    }
    
    private double length() {
        double x = endX-startX;
        double y = endY-startY;
        return Math.sqrt(x*x+y*y);
    }
    
    private double theta() {
        if (length() == 0) {
            return 0;
        }
        return thetaDirection() * MathUtil.acos((endX-startX)/length());
    }
    
    private int thetaDirection() {
        if (endY >= startY) {
            return 1;
        } else {
            return -1;
        }
    }
    
    private int[] reverseColors() {
        int[] colors = getColors();
        int len = colors.length;
        int[] out = new int[len];
        for (int i=0; i<len; i++) {
            out[i] = colors[len-i-1];
        }
        return out;
    }
    
    private float[] reverseFractions() {
        float[] fractions = getFractions();
        int len = fractions.length;
        float[] out = new float[len];
        for (int i=0; i<len; i++) {
            out[i] = 1f-fractions[len-i-1];
        }
        return out;
    }
    
    /**
     * Painds the linear gradient in the given bounds.
     * @param g
     * @param x
     * @param y
     * @param w
     * @param h 
     */
    @Override
    public void paint(Graphics g, double x, double y, double w, double h) {
        paint(g, x, y, w, h, true);
    }
    private void paint(Graphics g, double x, double y, double w, double h, boolean processCycles) {
        Paint p = g.getPaint();
        int[] colors = getColors();
        float[] fractions = getFractions();
        
        double theta = theta();
        double px = 0;
        double py = 0;
        double pw = length();
        double ph = Math.max(w, h) * 2;

        
        double pEndX = (endX-x) * pw/w;
        //System.out.println("px="+px+", "+py+", "+pw+", "+ph+" theta="+theta);
        
        g.getTransform(t);
        t2.setTransform(t);
        if (getTransform() != null) {
            t2.concatenate(getTransform());
        }
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();
        g.translate(-tx, -ty);
        
        t2.translate((float)(startX+tx), (float)(startY+ty));
        t2.rotate((float)theta, 0, 0);
        t2.translate(0, -(float)ph/2);
        
        g.setTransform(t2);
        int len = Math.min(colors.length, fractions.length);
        int alpha = g.getAlpha();
        int gradientTrans = getTransparency();
        if (getTransparency() < 0xff) {
            g.setAlpha((int)(alpha * gradientTrans/255.0));
        }
        /*
        if (pStartX > 0) {
            g.setColor(colors[0]);
            g.fillRect(0, 0, (int)Math.round(pStartX), (int)Math.round(ph));
        }
        */
        if (processCycles) {
            switch (getCycleMethod()) {
                case NO_CYCLE: {

                    g.setColor(colors[0]);
                    g.fillRect((int)Math.floor(-insetStartLength(x, y, w, h)), 0, (int)Math.ceil(insetStartLength(x, y, w, h))+1, (int)Math.round(ph));
                    break;
                }
                case REPEAT:
                case REFLECT: {
                    int currPos = 0;
                    int endPos = (int)Math.floor(-insetStartLength(x, y, w, h));
                    int iter = 0;

                    while (currPos > endPos) {
                        float sx = (float)startX;
                        float sy = (float)startY;
                        float ex = (float)endX;
                        float ey = (float)endY;
                        int[] cols = getColors();
                        float[] fracs = getFractions();
                        if (iter % 2 == 0 && getCycleMethod() == REFLECT) {
                            sx = (float)endX;
                            sy = (float)endY;
                            ex = (float)startX;
                            ey = (float)startY;
                            cols = reverseColors();
                            fracs = reverseFractions();
                        }
                        for (int i=0; i<len-1; i++) {
                            int x1 = (int)Math.round(currPos -fracs[i]*pw);
                            int x2 = (int)Math.round(currPos - fracs[i+1]*pw);
                            g.fillLinearGradient(cols[i], cols[i+1], x1, 0, Math.abs(x2-x1), (int)Math.round(ph), true);
                        }
                        currPos -= pw;
                        iter++;
                    }
                }

            }
        }
        for (int i=0; i<len-1; i++) {
            int x1 = (int)Math.round(fractions[i]*pw);
            int x2 = (int)Math.round(fractions[i+1]*pw);
            g.fillLinearGradient(colors[i], colors[i+1], x1, 0, x2-x1, (int)Math.round(ph), true);
        }
        if (processCycles) {
            switch (getCycleMethod()) {
                case NO_CYCLE: {

                    g.setColor(colors[len-1]);
                    g.fillRect((int)Math.floor(pw)-1, 0, (int)Math.ceil(insetEndLength(x, y, w, h)), (int)Math.round(ph));
                    break;
                }
                case REPEAT:
                case REFLECT: {
                    int currPos = 0;
                    int endPos = (int)Math.ceil(insetEndLength(x, y, w, h));
                    int iter = 0;

                    while (currPos < endPos) {
                        float sx = (float)startX;
                        float sy = (float)startY;
                        float ex = (float)endX;
                        float ey = (float)endY;
                        int[] cols = getColors();
                        float[] fracs = getFractions();
                        if (iter % 2 == 0 && getCycleMethod() == REFLECT) {
                            sx = (float)endX;
                            sy = (float)endY;
                            ex = (float)startX;
                            ey = (float)startY;
                            cols = reverseColors();
                            fracs = reverseFractions();
                        }
                        for (int i=0; i<len-1; i++) {
                            int x1 = (int)Math.round(currPos +fracs[i]*pw);
                            int x2 = (int)Math.round(currPos + fracs[i+1]*pw);
                            g.fillLinearGradient(cols[i], cols[i+1], x1, 0, Math.abs(x2-x1), (int)Math.round(ph), true);
                        }
                        currPos += pw;
                        iter++;
                    }
                }
                
            }
        }
        /*
        if (pEndX < pw) {
            g.setColor(colors[len-1]);
            g.fillRect((int)Math.round(pEndX), 0, (int)Math.round(pw-pEndX), (int)Math.round(ph));
        }*/
        g.setAlpha(alpha);
        g.setTransform(t);
        g.translate(tx, ty);
        if (p != null) {
            g.setColor(p);
        }
        
        
    }
    
    private static double findAngle(double x1, double y1, double x2, double y2) {
        return MathUtil.atan((y2-y1)/(x2-x1));
    }
    
    private static double scaleX(double theta, double x) {
        return x/Math.cos(theta);
    }
    
    private static double scaleY(double theta, double y) {
        return y/Math.sin(theta);
    }
    
    
    
    

}
