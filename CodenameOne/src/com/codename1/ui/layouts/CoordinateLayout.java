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
package com.codename1.ui.layouts;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;

/**
 * Allows laying out components based on absolute positions/sizes
 * that are adapted based on available space for the layout.
 * The layout 
 *
 * @author Chen Fishbein
 */
public class CoordinateLayout extends Layout{
    
    private int width;
    private int height;
    
    /**
     * Allows creating a coordinate layouts that disables the scaling feature
     */
    public CoordinateLayout(){
        this.width = -1;
        this.height = -1;
    }

    /**
     * This constructor accepts the relative width and height used to define the
     * aspect ratio of the Container
     * 
     * @param width
     * @param height
     */
    public CoordinateLayout(int width, int height){
        this.width = width;
        this.height = height;
    }

    /**
     * This constructor accepts the relative width and height used to define the
     * aspect ratio of the Container
     * 
     * @param d the width/height
     */
    public CoordinateLayout(Dimension d){
        this(d.getWidth(), d.getHeight());
    }
    
    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container parent) {
        if(width < 0) {
            return;
        }
        int numOfcomponents = parent.getComponentCount();
        int parentW = parent.getWidth();
        int parentH = parent.getHeight();
        
        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            int x = cmp.getX() * parentW /width;
            int y = cmp.getY() * parentH /height;
            cmp.setX(x);
            cmp.setY(y);
            
            cmp.setWidth(cmp.getPreferredW());
            cmp.setHeight(cmp.getPreferredH());
        }
        width = parentW;
        height = parentH;
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize(Container parent) {
        Dimension retVal = new Dimension();
        int numOfcomponents = parent.getComponentCount();
        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            retVal.setWidth(Math.max(retVal.getWidth(), cmp.getX() + cmp.getPreferredW()));
            retVal.setHeight(Math.max(retVal.getHeight(), cmp.getY() + cmp.getPreferredH()));
        }
        
        return retVal;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOverlapSupported() {
        return true;
    }

}
