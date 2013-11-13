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
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/**
 * Base class for spinners
 *
 * @author Shai Almog
 */
public abstract class BaseSpinner extends Container {
    private Style overlayStyle;
    
    /**
     * Default constructor
     */
    public BaseSpinner() {
        super(new BoxLayout(BoxLayout.X_AXIS));
        setUIID("SpinnerWrapper");
        overlayStyle = getUIManager().getComponentStyle("SpinnerOverlay");
        installDefaultPainter(overlayStyle);
    }
    
    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize() {
        if(!isInitialized()) {
            initSpinner();
        }
        Dimension d = super.calcPreferredSize();
        if(overlayStyle.getBorder() != null) {
            d.setWidth(Math.max(overlayStyle.getBorder().getMinimumWidth(), d.getWidth()));
            d.setHeight(Math.max(overlayStyle.getBorder().getMinimumHeight(), d.getHeight()));
        }
        return d;
    }
    
    /**
     * Default constructor
     */
    protected void initComponent() {
        super.initComponent();
        initSpinner();
    }


    void initSpinner() {
    }
    
    /**
     * @inheritDoc
     */
    public void setUIID(String id) {
        super.setUIID(id);
    }

    /**
     * @inheritDoc
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        overlayStyle = getUIManager().getComponentStyle("SpinnerOverlay");
        installDefaultPainter(overlayStyle);
    }

    Component createSeparator() {
        Label l = new Label(" ") {

            public void repaint() {
                getParent().repaint();
            }
        };
        l.setUIID("SpinnerSeparator");
        return l;
    }
    
    /**
     * @inheritDoc
     */
    protected void paintGlass(Graphics g) {
        super.paintGlass(g);
        paintOverlay(g);
    }   
    
    private void paintOverlay(Graphics g) {
        int x = getParent().getAbsoluteX();
        int y = getParent().getAbsoluteY();
        g.translate(x, y);
        if(overlayStyle.getBorder() != null) {
            overlayStyle.getBorder().paintBorderBackground(g, this);
            overlayStyle.getBorder().paint(g, this);
        } else {
            overlayStyle.getBgPainter().paint(g, getBounds());
        }
        g.translate(-x, -y);        
    }
}
