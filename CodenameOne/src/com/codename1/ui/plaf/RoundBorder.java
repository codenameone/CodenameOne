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
package com.codename1.ui.plaf;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

/**
 * A simple circular border for a component that can have an optional emboss and shadow
 *
 * @author Shai Almog
 */
class RoundBorder extends Border {
    private final int mm;
    
    /**
     * Default constructor 
     */
    public RoundBorder() {
        mm = Display.getInstance().convertToPixels(1, true);
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public boolean isBackgroundPainter() {
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void paint(Graphics g, Component c) {
    }

    /**
     * @inheritDoc
     */
    @Override
    public void paintBorderBackground(Graphics g, Component c) {
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        int alpha = g.getAlpha();
        g.setColor(0);
        g.setAlpha(80);
        int size = Math.min(c.getWidth(), c.getHeight()) - mm;
        g.fillArc(c.getX(), c.getY() + mm, size, size, 0, 360);
        g.setAlpha(alpha);
        g.setColor(c.getStyle().getBgColor());
        g.fillArc(c.getX(), c.getY(), size, size, 0, 360);
        
        g.lighterColor(4);
        g.drawArc(c.getX(), c.getY(), size, size, -45, 90);
        g.setAntiAliased(aa);
    }

    
}
