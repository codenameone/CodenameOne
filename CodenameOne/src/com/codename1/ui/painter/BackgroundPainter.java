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
package com.codename1.ui.painter;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;

/**
 * A painter that draws the background of a component based on its style
 *
 * @author Shai Almog
 */
public class BackgroundPainter implements Painter {
    private Component parent;
    
    /**
     * Construct a background painter for the given component
     * 
     * @param parent the parent component
     */
    public BackgroundPainter(Component parent) {
        this.parent = parent;
    }
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g, Rectangle rect) {
        Style s = parent.getStyle();
        int x = rect.getX();
        int y = rect.getY();
        int width = rect.getSize().getWidth();
        int height = rect.getSize().getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Image bgImage = s.getBgImage();
        if (bgImage == null) {
            g.setColor(s.getBgColor());
            g.fillRect(x, y, width, height, s.getBgTransparency());
        } else {
            if (s.getBackgroundType() == Style.BACKGROUND_IMAGE_SCALED) {
                if (bgImage.getWidth() != width || bgImage.getHeight() != height) {
                    bgImage = bgImage.scaled(width, height);
                    s.setBgImage(bgImage, true);
                }
            } else {
                int iW = bgImage.getWidth();
                int iH = bgImage.getHeight();
                for(int xPos = 0 ; xPos < width ; xPos += iW) { 
                    for(int yPos = 0 ; yPos < height ; yPos += iH) { 
                        g.drawImage(s.getBgImage(), x + xPos, y + yPos);
                    }
                }
                return;
            }
            g.drawImage(s.getBgImage(), x, y);
        }
    }
}
