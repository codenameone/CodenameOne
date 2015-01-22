/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.codename1.charts.compat;

import com.codename1.ui.geom.Rectangle;

/**
 *
 * @author shannah
 */
public class GradientDrawable {

    Orientation orientation;
    int[] colors;
    Rectangle bounds = new Rectangle();
    
    public GradientDrawable(Orientation orientation, int[] colors) {
        this.orientation = orientation;
        this.colors = colors;
    }
    
    public static enum Orientation {
        BL_TR,
        BOTTOM_TOP,
        BR_TL,
        LEFT_RIGHT,
        RIGHT_LEFT,
        TL_BR,
        TOP_BOTTOM,
        TR_BL
    }

    public void setBounds(int left, int top, int right, int bottom) {
        bounds.setBounds(left, top, right-left, bottom-top);
    }

    public void draw(Canvas canvas) {
        canvas.drawGradient(this);
    }
    
}
