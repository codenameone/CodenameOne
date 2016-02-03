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
import com.codename1.ui.geom.*;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/**
 * <p>Layout manager that places elements in a row (<code>X_AXIS</code>) or column (<code>Y_AXIS</code>) 
 * according to box orientation. Box is a very simple and predictable layout that serves as the "workhorse" of 
 * component lists in Codename One<br>
 * You can create a box layout Y UI using syntax such as this</p>
 * 
 * <script src="https://gist.github.com/codenameone/99f5e43061b4c6413d16.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/box-layout-y.png" alt="Box Layout Y" />
 * 
 * <p>
 * This can also be expressed with more terse syntax e.g. an X axis layout like this:
 * </p>
 * <script src="https://gist.github.com/codenameone/5d2908517241126b5803.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/box-layout-x.png" alt="Box Layout X" />
 *
 * <p>
 * The <code>BoxLayout</code> keeps the preferred size of its destination orientation and scales elements on the other axis. 
 * Specifically <code>X_AXIS</code> will keep the preferred width of the component while growing all
 * the components vertically to match in size. Its <code>Y_AXIS</code> counterpart keeps the preferred height
 * while growing the components horizontally.<br>
 * This behavior is very useful since it allows elements to align as they would all have the same size.
 * </p>
 * <p>
 * In some cases the growing behavior in the X axis is undesired, for these cases we can use the <code>X_AXIS_NO_GROW</code>
 * variant.
 * </p>
 * <img src="https://www.codenameone.com/img/developer-guide/box-layout-x-no-grow.png" alt="Box Layout X No Grow" />
 * 
 * <h4>FlowLayout vs. BoxLayout.X_AXIS/X_AXIS_NO_GROW</h4>
 * <p>
 * There are quite a few differences between {@link FlowLayout} and <code>BoxLayout</code>. When it doesn't
 * matter to you we tend to recommend <code>BoxLayout</code> as it acts more consistently in all situations since
 * its far simpler. Another advantage of <code>BoxLayout</code> is the fact that it grows and thus aligns nicely.
 * </p>
 * 
 * @author Chen Fishbein
 */
public class BoxLayout extends Layout{
    
    /**
     * Horizontal layout where components are arranged from left to right
     */
    public static final int X_AXIS = 1;

    /**
     * Vertical layout where components are arranged from top to bottom
     */
    public static final int Y_AXIS = 2;
    
    /**
     * Horizontal layout where components are arranged from left to right but don't grow vertically beyond their preferred size
     */
    public static final int X_AXIS_NO_GROW = 3;

    private int axis;
    
    /** 
     * Creates a new instance of BoxLayout
     * 
     * @param axis the axis to lay out components along. 
     * Can be: BoxLayout.X_AXIS or BoxLayout.Y_AXIS
     */
    public BoxLayout(int axis) {
        this.axis = axis;
    }
    
    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container parent) {        
        int width = parent.getLayoutWidth() - parent.getSideGap() - parent.getStyle().getPadding(false, Component.RIGHT) - parent.getStyle().getPadding(false, Component.LEFT);
        int height = parent.getLayoutHeight() - parent.getBottomGap() - parent.getStyle().getPadding(false, Component.BOTTOM) - parent.getStyle().getPadding(false, Component.TOP);
        int x = parent.getStyle().getPadding(parent.isRTL(), Component.LEFT);
        int y = parent.getStyle().getPadding(false, Component.TOP);
        int numOfcomponents = parent.getComponentCount();
        
        boolean rtl = parent.isRTL();
        if(rtl) {
        	x += parent.getSideGap();
        }
        int initX = x;

        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            Style stl = cmp.getStyle();
            
            switch(axis) {
                case Y_AXIS:
                    int cmpBottom = height;
                    cmp.setWidth(width - stl.getMargin(parent.isRTL(), Component.LEFT) - stl.getMargin(parent.isRTL(), Component.RIGHT));
                    int cmpH = cmp.getPreferredH();

                    y += stl.getMargin(false, Component.TOP);

                    if(y >= cmpBottom && !parent.isScrollableY()){
                        cmpH = 0;
                    }else if(y + cmpH - parent.getStyle().getPadding(false, Component.TOP) > cmpBottom){
                        if(!parent.isScrollableY()) {
                            cmpH = cmpBottom - y - stl.getMargin(false, Component.BOTTOM);
                        }
                    }
                    cmp.setHeight(cmpH);
                    cmp.setX(x + stl.getMargin(parent.isRTL(), Component.LEFT));
                    cmp.setY(y);
                    y += cmp.getHeight() + stl.getMargin(false, Component.BOTTOM);
                    break;
                case X_AXIS_NO_GROW: {
                    int cmpRight = width;
                    height = Math.min(getPreferredSize(parent).getHeight(), height);
                    int cmpW = cmp.getPreferredW();

                    x += stl.getMargin(false, Component.LEFT);

                    if(x >= cmpRight && !parent.isScrollableX()){
                        cmpW = 0;
                    } else {
                        if(x + cmpW - parent.getStyle().getPadding(false, Component.LEFT) > cmpRight){
                            cmpW = cmpRight - x - stl.getMargin(false, Component.RIGHT);
                        }
                    }
                    cmp.setWidth(cmpW);
                    cmp.setHeight(height- stl.getMargin(false, Component.TOP) - stl.getMargin(false, Component.BOTTOM));
                    if(rtl) {
                            cmp.setX(width + initX - (x - initX) - cmpW);
                    } else {
                            cmp.setX(x);
                    }
                    cmp.setY(y + stl.getMargin(false, Component.TOP));
                    x += cmp.getWidth() + stl.getMargin(false, Component.RIGHT);
                    break;
                }
                default:
                    int cmpRight = width;
                    int cmpW = cmp.getPreferredW();

                    x += stl.getMargin(false, Component.LEFT);

                    if(x >= cmpRight && !parent.isScrollableX()){
                        cmpW = 0;
                    } else {
                        if(x + cmpW - parent.getStyle().getPadding(false, Component.LEFT) > cmpRight){
                            cmpW = cmpRight - x - stl.getMargin(false, Component.RIGHT);
                        }
                    }
                    cmp.setWidth(cmpW);
                    cmp.setHeight(height- stl.getMargin(false, Component.TOP) - stl.getMargin(false, Component.BOTTOM));
                    if(rtl) {
                            cmp.setX(width + initX - (x - initX) - cmpW);
                    } else {
                            cmp.setX(x);
                    }
                    cmp.setY(y + stl.getMargin(false, Component.TOP));
                    x += cmp.getWidth() + stl.getMargin(false, Component.RIGHT);
                    break;
            }
        }
    }
    
    private Dimension dim = new Dimension(0, 0);


    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize(Container parent) {
        int width = 0;
        int height = 0;

        int numOfcomponents = parent.getComponentCount();
        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            Style stl = cmp.getStyle();
            
            if(axis == Y_AXIS){
                int cmpH = cmp.getPreferredH() + stl.getMargin(false, Component.TOP) + stl.getMargin(false, Component.BOTTOM);
                height += cmpH;
                width = Math.max(width , cmp.getPreferredW()+ stl.getMargin(false, Component.LEFT) + stl.getMargin(false, Component.RIGHT));
            }else{
                int cmpW = cmp.getPreferredW() + stl.getMargin(false, Component.LEFT) + stl.getMargin(false, Component.RIGHT);
                width += cmpW;
                height = Math.max(height, cmp.getPreferredH() + stl.getMargin(false, Component.TOP) + stl.getMargin(false, Component.BOTTOM));
            }
        }
        dim.setWidth(width + parent.getStyle().getPadding(false, Component.LEFT)+ parent.getStyle().getPadding(false, Component.RIGHT));
        dim.setHeight(height + parent.getStyle().getPadding(false, Component.TOP)+ parent.getStyle().getPadding(false, Component.BOTTOM));
        return dim;
    }  

    /**
     * Returns the layout axis x/y
     * 
     * @return the layout axis
     */
    public int getAxis() {
        return axis;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if(axis == X_AXIS) {
            return "BoxLayout X";
        }
        return "BoxLayout Y";
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return super.equals(o) && axis == ((BoxLayout)o).axis;
    }
    
    /**
     * The equivalent of Container.enclose() with a box layout Y
     * <img src="https://www.codenameone.com/img/developer-guide/box-layout-x.png" alt="Box Layout X" />
     * @param cmps the set of components
     * @return the newly created container
     */
    public static Container encloseY(Component... cmps) {
        return Container.encloseIn(new BoxLayout(BoxLayout.Y_AXIS), cmps);
    }

    
    /**
     * The equivalent of Container.enclose() with a box layout X
     * <img src="https://www.codenameone.com/img/developer-guide/box-layout-x.png" alt="Box Layout X" />
     * @param cmps the set of components
     * @return the newly created container
     */
    public static Container encloseX(Component... cmps) {
        return Container.encloseIn(new BoxLayout(BoxLayout.X_AXIS), cmps);
    }

    /**
     * The equivalent of Container.enclose() with a box layout X no grow option
     * <img src="https://www.codenameone.com/img/developer-guide/box-layout-x.png" alt="Box Layout X" />
     * @param cmps the set of components
     * @return the newly created container
     */
    public static Container encloseXNoGrow(Component... cmps) {
        return Container.encloseIn(new BoxLayout(BoxLayout.X_AXIS_NO_GROW), cmps);
    }
}
