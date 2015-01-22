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
