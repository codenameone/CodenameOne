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
import com.codename1.ui.Form;
import com.codename1.ui.geom.*;
import com.codename1.ui.plaf.Style;

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

    /**
     * Same as Y_AXIS with a special case for the last component. The last 
     * component is glued to the end of the available space
     */
    public static final int Y_AXIS_BOTTOM_LAST = 4;
    
    /**
     * @since 7.0
     */
    private int align=Component.TOP;
    
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
     * Shorthand for {@code new BoxLayout(BoxLayout.Y_AXIS)}
     * @return a new Y axis {@code BoxLayout}
     */
    public static BoxLayout y() {
        return new BoxLayout(BoxLayout.Y_AXIS);
    }

    /**
     * Shorthand for {@code new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST)}
     * @return a new Y bottom last axis {@code BoxLayout}
     */
    public static BoxLayout yLast() {
        return new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST);
    }
    
    /**
     * Creates a new layout with {@link #Y_AXIS}, and align center.
     * @return BoxLayout with center alignment on Y_AXIS.
     * @since 7.0
     */
    public static BoxLayout yCenter() {
        BoxLayout out = new BoxLayout(BoxLayout.Y_AXIS);
        out.setAlign(Component.CENTER);
        return out;
    }
    
    /**
     * Creates a new layout with {@link #Y_AXIS}, and align bottom.
     * @return BoxLayout with bottom alignment on Y_AXIS.
     * @since 7.0
     */
    public static BoxLayout yBottom() {
        BoxLayout out = new BoxLayout(BoxLayout.Y_AXIS);
        out.setAlign(Component.BOTTOM);
        return out;
    }
    
    /**
     * Shorthand for {@code new BoxLayout(BoxLayout.X_AXIS)}
     * @return a new X axis {@code BoxLayout}
     */
    public static BoxLayout x() {
        return new BoxLayout(BoxLayout.X_AXIS);
    }
    
    /**
     * Creates a new layout with {@link #X_AXIS}, and align center.
     * @return BoxLayout with center alignment on X_AXIS.
     * @since 7.0
     */
    public static BoxLayout xCenter() {
        BoxLayout out = new BoxLayout(BoxLayout.X_AXIS);
        out.setAlign(Component.CENTER);
        return out;
    }
    
    /**
     * Creates a new layout with {@link #X_AXIS}, and align right.
     * @return BoxLayout with right alignment on X_AXIS.
     * @since 7.0
     */
    public static BoxLayout xRight() {
        BoxLayout out = new BoxLayout(BoxLayout.X_AXIS);
        out.setAlign(Component.RIGHT);
        return out;
    }
    
    /**
     * Sets the alignment of this layout. By default Y_AXIS aligns top, and X_AXIS aligns left (RTL-aware).  You can specify an align value of {@link Component#CENTER} to align items vertically centered (for Y_AXIS), and horizontally centered (for X_AXIS),
     * of {@link Component#BOTTOM} to align vertically bottom (Y_AXIS), and {@link Component#RIGHT} to align right (RTL-aware), for X_AXIS.
     * @param align One of {@link Component#CENTER}, {@link Component#BOTTOM}, {@link Component#RIGHT}, to adjust the alignment of children.
     * @since 7.0
     */
    public void setAlign(int align) {
        this.align = align;
    }
    
    /**
     * Gets the alignment of this layout.  By default Y_AXIS aligns top, and X_AXIS aligns left (RTL-aware).  You can specify an align value of {@link Component#CENTER} to align items vertically centered (for Y_AXIS), and horizontally centered (for X_AXIS),
     * of {@link Component#BOTTOM} to align vertically bottom (Y_AXIS), and {@link Component#RIGHT} to align right (RTL-aware), for X_AXIS.
     * @return The alignment.
     * @since 7.0
     */
    public int getAlign() {
        return this.align;
    }

    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container parent) {        
        Style ps = parent.getStyle();
        int width = parent.getLayoutWidth() - parent.getSideGap() - ps.getHorizontalPadding();
        int height = parent.getLayoutHeight() - parent.getBottomGap() - ps.getVerticalPadding();
        int x = ps.getPaddingLeft(parent.isRTL());
        int y = ps.getPaddingTop();
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
                case Y_AXIS_BOTTOM_LAST: 
                    int cmpBottom = height;
                    cmp.setWidth(width - stl.getHorizontalMargins());
                    int cmpH = cmp.getPreferredH();

                    y += stl.getMarginTop();

                    if(y - ps.getPaddingTop() >= cmpBottom && !parent.isScrollableY()){
                        cmpH = 0;
                    }else if(y + cmpH - ps.getPaddingTop() > cmpBottom){
                        if(!parent.isScrollableY()) {
                            cmpH = cmpBottom - y - stl.getMarginBottom();
                        }
                    }
                    cmp.setHeight(cmpH);
                    cmp.setX(x + stl.getMarginLeft(parent.isRTL()));
                    cmp.setY(y);
                    y += cmp.getHeight() + stl.getMarginBottom();
                    break;
                case X_AXIS_NO_GROW: {
                    int cmpRight = width;
                    height = Math.min(getPreferredSize(parent).getHeight(), height);
                    int cmpW = cmp.getPreferredW();

                    x += stl.getMarginLeftNoRTL();

                    if(x >= cmpRight && !parent.isScrollableX()){
                        cmpW = 0;
                    } else {
                        if(x + cmpW - ps.getPaddingLeftNoRTL() > cmpRight){
                            cmpW = cmpRight - x - stl.getMarginRightNoRTL();
                        }
                    }
                    cmp.setWidth(cmpW);
                    cmp.setHeight(height- stl.getMarginTop() - stl.getMarginBottom());
                    if(rtl) {
                            cmp.setX(width + initX - (x - initX) - cmpW);
                    } else {
                            cmp.setX(x);
                    }
                    cmp.setY(y + stl.getMarginTop());
                    x += cmp.getWidth() + stl.getMarginRightNoRTL();
                    break;
                }
                default:
                    int cmpRight = width;
                    int cmpW = cmp.getPreferredW();

                    x += stl.getMarginLeftNoRTL();

                    if(x >= cmpRight && !parent.isScrollableX()){
                        cmpW = 0;
                    } else {
                        if(x + cmpW - ps.getPaddingLeftNoRTL()> cmpRight){
                            cmpW = cmpRight - x - stl.getMarginRightNoRTL();
                        }
                    }
                    cmp.setWidth(cmpW);
                    cmp.setHeight(height- stl.getVerticalMargins());
                    if(rtl) {
                            cmp.setX(width + initX - (x - initX) - cmpW);
                    } else {
                            cmp.setX(x);
                    }
                    cmp.setY(y + stl.getMarginTop());
                    x += cmp.getWidth() + stl.getMarginRightNoRTL();
                    break;
            }
        }
        
        if(axis == Y_AXIS_BOTTOM_LAST && numOfcomponents > 0) {
            if(parent instanceof Form) {
                parent = ((Form)parent).getContentPane();
            }
            Component cmp = parent.getComponentAt(numOfcomponents - 1);
            if(cmp.getY() + cmp.getHeight() < height) {
                cmp.setY(height - cmp.getHeight());
            }
        }
        if (axis == Y_AXIS) {
            
            if (numOfcomponents > 0) {
                int containerBottomInner = parent.getLayoutHeight() - parent.getStyle().getPaddingBottom();
                Component lastCmp = parent.getComponentAt(numOfcomponents-1);
                int lastCmpBottomOuter = lastCmp.getY() + lastCmp.getHeight() + lastCmp.getStyle().getMarginBottom();
                int dy = 0;
                switch (align) {
                    case Component.CENTER: {
                        dy = (containerBottomInner - lastCmpBottomOuter) / 2;
                        break;
                    }
                    case Component.BOTTOM: {
                        dy = (containerBottomInner - lastCmpBottomOuter);
                        break;
                    }
                }
                if (dy > 0) {
                    for (int i=0; i<numOfcomponents; i++) {
                        Component cmp = parent.getComponentAt(i);
                        cmp.setY(cmp.getY() + dy);
                    }
                }
            }
            
        } else if (axis == X_AXIS) {
            if (numOfcomponents > 0) {
                if (rtl) {
                    int containerLeftInner = parent.getStyle().getPaddingLeftNoRTL();
                    Component lastCmp = parent.getComponentAt(numOfcomponents-1);
                    int lastCmpLeftOuter = lastCmp.getX() - lastCmp.getStyle().getMarginLeftNoRTL();
                    int dx = 0;
                    switch (align) {
                        case Component.CENTER: {
                            dx = (lastCmpLeftOuter - containerLeftInner) / 2;
                            break;
                        }
                        case Component.RIGHT: {
                            dx = (lastCmpLeftOuter - containerLeftInner);
                            break;
                        }
                    }
                    if (dx > 0) {
                        for (int i=0; i<numOfcomponents; i++) {
                            Component cmp = parent.getComponentAt(i);
                            cmp.setX(cmp.getX() - dx);
                        }
                    }
                } else {
                    int containerRightInner = parent.getLayoutWidth() - parent.getStyle().getPaddingRightNoRTL();
                    Component lastCmp = parent.getComponentAt(numOfcomponents-1);
                    int lastCmpRightOuter = lastCmp.getX() + lastCmp.getWidth() + lastCmp.getStyle().getMarginRightNoRTL();
                    int dx = 0;
                    switch (align) {
                        case Component.CENTER: {
                            dx = (containerRightInner - lastCmpRightOuter) / 2;
                            break;
                        }
                        case Component.RIGHT: {
                            dx = (containerRightInner - lastCmpRightOuter);
                            break;
                        }
                    }
                    if (dx > 0) {
                        for (int i=0; i<numOfcomponents; i++) {
                            Component cmp = parent.getComponentAt(i);
                            cmp.setX(cmp.getX() + dx);
                        }
                    }
                }
                
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
            
            if(axis == Y_AXIS || axis == Y_AXIS_BOTTOM_LAST){
                int cmpH = cmp.getPreferredH() + stl.getVerticalMargins();
                height += cmpH;
                width = Math.max(width , cmp.getPreferredW()+ stl.getHorizontalMargins());
            }else{
                int cmpW = cmp.getPreferredW() + stl.getHorizontalMargins();
                width += cmpW;
                height = Math.max(height, cmp.getPreferredH() + stl.getVerticalMargins());
            }
        }
        Style s = parent.getStyle();
        dim.setWidth(width + s.getHorizontalPadding());
        dim.setHeight(height + s.getVerticalPadding());
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
     * The equivalent of Container.enclose() with a box layout Y, with center alignment.
     * @param cmps the set of components
     * @return the newly created container
     * @since 7.0
     */
    public static Container encloseYCenter(Component... cmps) {
        return Container.encloseIn(yCenter(), cmps);
    }
    
    /**
     * The equivalent of Container.enclose() with a box layout Y, with bottom alignment.
     * @param cmps the set of components
     * @return the newly created container
     * @since 7.0
     */
    public static Container encloseYBottom(Component... cmps) {
        return Container.encloseIn(yBottom(), cmps);
    }
    
    /**
     * The equivalent of Container.enclose() with a box layout Y in bottom 
     * last mode
     * @param cmps the set of components
     * @return the newly created container
     */
    public static Container encloseYBottomLast(Component... cmps) {
        return Container.encloseIn(new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST), cmps);
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
    
    /**
     * The equivalent of Container.enclose() with a box layout X, with center alignment.
     * @param cmps the set of components
     * @return the newly created container
     * @since 7.0
     */
    public static Container encloseXCenter(Component... cmps) {
        return Container.encloseIn(xCenter(), cmps);
    }
    
    /**
     * The equivalent of Container.enclose() with a box layout X, with right alignment.
     * @param cmps the set of components
     * @return the newly created container
     * @since 7.0
     */
    public static Container encloseXRight(Component... cmps) {
        return Container.encloseIn(xRight(), cmps);
    }
}
