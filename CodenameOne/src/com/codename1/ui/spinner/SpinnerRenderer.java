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
package com.codename1.ui.spinner;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.List;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Effects;
import java.util.HashMap;
import java.util.Map;

/**
 * Spinner renderer that can automatically simulate the iOS perspective transform behavior
 *
 * @author Shai Almog
 */
class SpinnerRenderer<T> extends DefaultListCellRenderer<T>{
    private Map<Character, Image>[] imageCache;
    private static final int PERSPECTIVES = 9;
    private static final int FRONT_ANGLE = 4;
    private static final float[] TOP_SCALE = {0.5f, 0.5f, 0.8f, 0.95f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] BOTTOM_SCALE = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f, 0.8f, 0.5f, 0.5f};
    private static final float[] VERTICAL_SHRINK = {0.5f, 0.5f, 0.80f, 0.90f, 1.0f, 0.90f, 0.80f, 0.5f, 0.5f};
    
    int perspective;
    
    public SpinnerRenderer() {
        super(false);
    }

    @Override
    public Component getCellRendererComponent(Component list, Object model, T value, int index, boolean isSelected) {
        if(iOS7Mode) {
            perspective = -1;
            // calculate perspective
            int idx = ((List)list).getCurrentSelected();
            if(idx == index) {
                perspective = FRONT_ANGLE;
            } else {
                int count = ((List)list).getModel().getSize();
                int directDistance = Math.abs(idx - index);
                int indirect;
                if(index > idx) {
                    indirect = count - index + idx;
                } else {
                    indirect = count - idx + index;
                }
                if(indirect < directDistance) {
                    if(indirect < FRONT_ANGLE) {
                        if(index < idx) {
                            perspective = indirect;
                        } else {
                            perspective = FRONT_ANGLE + 1 + indirect;
                        }
                    }
                } else {
                    if(directDistance < FRONT_ANGLE) {
                        if(index < idx) {
                            perspective = directDistance;
                        } else {
                            perspective = FRONT_ANGLE + 1 + directDistance;
                        }
                    }
                }
            }
        }
        return super.getCellRendererComponent(list, model, value, index, isSelected); 
    }
    
    static boolean iOS7Mode;
    @Override
    public void paint(Graphics g) {
        if(!iOS7Mode || perspective == FRONT_ANGLE) {
            super.paint(g);
        } else {
            if(!isInClippingRegion(g)) {
                return;
            }
            Style s = getStyle();
            drawStringPerspectivePosition(g, getText(), getX() + s.getPadding(LEFT), getY() + s.getPadding(RIGHT));
        }
    }
    
    /**
     * Draws the character with the given perspective effect
     */
    private int drawCharPerspectivePosition(Graphics g, char c, int x, int y) {
        if(imageCache == null) {
            imageCache = new HashMap[PERSPECTIVES];
            for(int iter = 0 ; iter < PERSPECTIVES ; iter++) {
                if(iter != FRONT_ANGLE) {
                    imageCache[iter] = new HashMap<Character, Image>();
                }
            }
        }
        Character chr = new Character(c);
        Image i = imageCache[perspective].get(chr);
        if(i == null) {
            //UIManager.getInstance().getLookAndFeel().setFG(g, this);
            Font f = getStyle().getFont();
            int w = f.charWidth(c);
            int h = f.getHeight();
            i = Image.createImage(w, h, 0);
            g = i.getGraphics();
            UIManager.getInstance().getLookAndFeel().setFG(g, this);
            g.drawChar(c, 0, 0);
            i = Effects.verticalPerspective(i, TOP_SCALE[perspective], BOTTOM_SCALE[perspective], VERTICAL_SHRINK[perspective]);
            imageCache[perspective].put(chr, i);
        }
        g.drawImage(i, x, y);
        return i.getWidth();
    }
    
    private void drawStringPerspectivePosition(Graphics g, String s, int x, int y) {
        if(perspective < 0 || perspective >= PERSPECTIVES) {
            return;
        }
        System.out.println("Drawing " + s + " at " + perspective);
        int l = s.length();
        int position = 0;
        for(int iter = 0 ; iter < l ; iter++) {
            char c = s.charAt(iter);
            position += drawCharPerspectivePosition(g, c, x + position, y);
            position -= 4;
        }
    }
}
