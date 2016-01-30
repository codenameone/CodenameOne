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
package com.codename1.ui.util;

import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.Vector;

/**
 * A Glass Tutorial appears on top of the UI especially on a touch device
 * but could be on any device and points to/circles components within the UI
 * coupled with explanation of what they do and a tint of the screen.
 * It is implemented as a GlassPane on top of a Form which is automatically
 * removed when a user touches the screen or presses a button.<br>
 * To position elements within the glass tutorial the elements must be 
 * associated with a component instance of the underlying UI and positioned 
 * relatively to said component.<br>
 * The GlassTutorial uses the "GlassTutorial" UIID to paint itself it then paints
 * the hint components in their proper places.
 * 
 * @author Shai Almog
 */
public class GlassTutorial implements Painter {
    private static final String DEST = "$$GLSDESTHINT$$";
    private static final String POS = "$$GLSDESTPOS$$";
    private Vector vec = new Vector(); 
    private Component internal;
    
    /**
     * Places a hint within the glass in a position relative to the destinationComponent, the position
     * is indicated with border layout attributes. Notice you can place multiple components on a single 
     * element and they will be rendered in order e.g. a component with a border can be used to "circle"
     * the destination by placing it in the CENTER position and another arrow with text can be places in 
     * the south position bellow.
     * 
     * @param hintComponent The component that would be renderered in the given position
     * @param destinationComponent the "hinted" component over which the hint will show
     * @param position the position relative to the destinationComponent in BorderLayout values e.g. to place the hint
     * above the component just place it in BorderLayout.NORTH. The center will stretch the component but the
     * other sides will give the component its exact preferred size.
     */
    public void addHint(Component hintComponent, Component destinationComponent, String position) {
        hintComponent.putClientProperty(POS, position);
        hintComponent.putClientProperty(DEST, destinationComponent);
        vec.addElement(hintComponent);
    }
        
    /**
     * Install the glass tutorial on a form and seamlessly dismiss it when no longer necessary
     * @param f the form
     */
    public void showOn(Form f) {
        Painter oldPane = f.getGlassPane();
        f.setGlassPane(this);
        Dialog dummy = new Dialog() {
            public void keyReleased(int i) {
                dispose();
            }
        };
        int oldTint = f.getTintColor();
        f.setTintColor(0);
        
        dummy.getDialogStyle().setBgTransparency(0);
        dummy.setDisposeWhenPointerOutOfBounds(true);
        dummy.show(0, Display.getInstance().getDisplayHeight() - 2, 0, Display.getInstance().getDisplayWidth() - 2, true, true);
        
        f.setTintColor(oldTint);
        f.setGlassPane(oldPane);
    }
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g, Rectangle rect) {
        if(internal == null) {
            internal = new Label(" ");
            internal.setUIID("GlassTutorial");
        }
        internal.setSize(rect.getSize());
        internal.paintComponent(g);
        
        int componentCount = vec.size();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = (Component)vec.elementAt(iter);
            String pos = (String)current.getClientProperty(POS);
            Component dest = (Component)current.getClientProperty(DEST);
            int xpos = dest.getAbsoluteX();
            int ypos = dest.getAbsoluteY();
            int w = dest.getWidth();
            int h = dest.getHeight();
            if(pos.equals(BorderLayout.CENTER)) {
                current.setX(xpos);
                current.setY(ypos);
                current.setWidth(w);
                current.setHeight(h);
                current.paintComponent(g);
                continue;
            }
            Dimension d = current.getPreferredSize();
            current.setWidth(d.getWidth());
            current.setHeight(d.getHeight());
            if(pos.equals(BorderLayout.SOUTH)) {
                current.setX(xpos + w / 2 - d.getWidth() / 2);
                current.setY(ypos + h);
                current.paintComponent(g);
                continue;
            }
            if(pos.equals(BorderLayout.NORTH)) {
                current.setX(xpos + w / 2 - d.getWidth() / 2);
                current.setY(ypos - d.getHeight());
                current.paintComponent(g);
                continue;
            }
            if(pos.equals(BorderLayout.EAST)) {
                current.setX(xpos + w);
                current.setY(ypos + h / 2 - d.getHeight() / 2);
                current.paintComponent(g);
                continue;
            }
            if(pos.equals(BorderLayout.WEST)) {
                current.setX(xpos - d.getWidth());
                current.setY(ypos + h / 2 - d.getHeight() / 2);
                current.paintComponent(g);
                continue;
            }
        }
    }
    
}
