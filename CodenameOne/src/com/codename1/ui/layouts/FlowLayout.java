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
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/**
 * <p>FlowLayout is the default layout manager for Codename One Containers and Forms. It places components
 * in a row one after another based on their preferred size. When it reaches the edge of the container it will break
 * a line and start a new row. </p>
 * <script src="https://gist.github.com/codenameone/124cab8d0c1da82756f1.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/flow-layout.png" alt="Result of FlowLayout code" />
 * 
 * <p>
 * Since flow layout isn't a constraint based layout it has a bunch of very useful enclose methods that can significantly 
 * reduce the code required to create the same UI e.g.:
 * </p>
 * <script src="https://gist.github.com/codenameone/3481c77f93726745ad28.js"></script>
 * 
 * <p>
 * This class works nicely for simple elements, however since Codename One doesn't reflow recursively (for performance)
 * it can't accurately handle complex layouts. As a result when an element of varying size is placed in a flow layout
 * this confuses the line breaking logic and fails in odd ways. That is why this layout should only be used for relatively
 * simple use cases.
 * </p>
 *
 * <p>
 * Flow layout supports aligning the component horizontally and vertically, it defaults to the top left alignment for 
 * LTR languages. E.g. the following alignments are supported thru the usage of <code>setAlign</code> &amp;
 * <code>setValign</code>.
 * </p>
 * <p>E.g. you can align to the center</p>
 * <img src="https://www.codenameone.com/img/developer-guide/flow-layout-center.png" alt="Flow layout align center" />
 * 
 * <p>You can align to the right</p>
 * <img src="https://www.codenameone.com/img/developer-guide/flow-layout-right.png" alt="Flow layout align right" />
 * 
 * <p>You can align to the center and the middle horizontally</p>
 * <img src="https://www.codenameone.com/img/developer-guide/flow-layout-center-middle.png" alt="Flow layout align middle" />
 * 
 * <p>There are quite a few additional combinations that are possible with these API's.</p>
 * 
 * @see BoxLayout see the box layout X which is often a better choice than flow layout.
 * @author Nir Shabi
 */
public class FlowLayout extends Layout{
    private boolean fillRows;

    private int orientation = Component.LEFT;
    private int valign = Component.TOP;
    private boolean vAlignByRow;


    /**
     * Creates a new instance of FlowLayout with left alignment
     */
    public FlowLayout() {
    }

    /**
     * Creates a new instance of FlowLayout with the given orientation one of
     * LEFT, RIGHT or CENTER
     *
     * @param orientation the orientation value
     */
    public FlowLayout(int orientation) {
        this.orientation = orientation;
    }


    /**
     * Creates a new instance of FlowLayout with the given orientation one of
     * LEFT, RIGHT or CENTER and the vertical orientation
     *
     * @param orientation the orientation value
     * @param valign the vertical orientation one of Component.TOP/BOTTOM/CENTER
     */
    public FlowLayout(int orientation, int valign) {
        this.orientation = orientation;
        this.valign = valign;
    }

    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container parent) {
        int x = parent.getStyle().getPadding(parent.isRTL(), Component.LEFT);
        int width = parent.getLayoutWidth() - parent.getSideGap() - parent.getStyle().getPadding(parent.isRTL(), Component.RIGHT) - x;
        
        boolean rtl = parent.isRTL();
        if(rtl) {
        	x += parent.getSideGap();
        }
        int initX = x;

        int y = parent.getStyle().getPadding(false, Component.TOP);
        int rowH=0;
        int start=0;
        int rowBaseline=0;

        int maxComponentWidth = width;

        int numOfcomponents = parent.getComponentCount();
        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            Style style = cmp.getStyle();
            int marginX = style.getMargin(false, Component.LEFT) + style.getMargin(false, Component.RIGHT);
            cmp.setWidth(Math.min(maxComponentWidth - marginX, cmp.getPreferredW()));
            cmp.setHeight(cmp.getPreferredH());

            if((x == parent.getStyle().getPadding(rtl, Component.LEFT)) || ( x+ cmp.getPreferredW() <= width) ) {
                // We take the actual LEFT since drawing is done in reverse
                x += cmp.getStyle().getMargin(false, Component.LEFT);
            	if(rtl) {
                	cmp.setX(Math.max(width + initX - (x - initX) - cmp.getPreferredW(), style.getMargin(false, Component.LEFT)));
            	} else {
            		cmp.setX(x);
            	}

                cmp.setY(y + cmp.getStyle().getMargin(cmp.isRTL(), Component.TOP));

                x += cmp.getWidth() + cmp.getStyle().getMargin(false, Component.RIGHT);
                rowH = Math.max(rowH, cmp.getHeight() + cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM));
                if ( valign == Component.BASELINE ){
                    int cmpPrefH = cmp.getPreferredH();
                    int cmpBaseline = cmp.getBaseline(cmp.getPreferredW(), cmpPrefH);
                    
                    rowBaseline = Math.max(rowBaseline, cmpBaseline + cmp.getStyle().getMargin(false, Component.TOP));
                    rowH = Math.max(rowH, rowBaseline + cmp.getStyle().getMargin(false, Component.BOTTOM) + cmpPrefH-cmpBaseline);
                }
            } else {
                moveComponents(parent, parent.getStyle().getPadding(rtl, Component.LEFT), y, width - parent.getStyle().getPadding(rtl, Component.LEFT) - x, rowH, start, i, rowBaseline);
                if(fillRows) {
                    fillRow(parent, width, start, i);
                }
                x = initX+cmp.getStyle().getMargin(false, Component.LEFT);
                y += rowH;
                rowBaseline = 0;

                if(rtl) {
                	cmp.setX(Math.max(width + initX - (x - initX) - cmp.getPreferredW(), style.getMargin(false, Component.LEFT)));
                } else {
                	cmp.setX(x);
                }

                cmp.setY(y + cmp.getStyle().getMargin(false, Component.TOP));
                rowH = cmp.getPreferredH()+ cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM);
                if ( valign == Component.BASELINE ){
                    int cmpPrefH = cmp.getPreferredH();
                    int cmpBaseline = cmp.getBaseline(cmp.getPreferredW(), cmpPrefH);
                    
                    rowBaseline = Math.max(rowBaseline, cmpBaseline + cmp.getStyle().getMargin(false, Component.TOP));
                    rowH = Math.max(rowH, rowBaseline + cmp.getStyle().getMargin(false, Component.BOTTOM) + cmpPrefH-cmpBaseline);
                }
                x += cmp.getPreferredW()+ cmp.getStyle().getMargin(false, Component.RIGHT);
                start = i;

            }
        }
        moveComponents(parent, parent.getStyle().getPadding(rtl, Component.LEFT), y, width - parent.getStyle().getPadding(rtl, Component.LEFT) - x, rowH, start, numOfcomponents, rowBaseline);
        if(fillRows) {
            fillRow(parent, width, start, numOfcomponents);
        }
    }

    /**
     * This method tries to fill up the available space in a row.
     * This method is called if isFillRows() returns true.
     * 
     * @param target the parent container
     * @param width the width of the row to fill
     * @param start the index of the first component in this row
     * @param end the index of the last component in this row
     */ 
    protected void fillRow(Container target, int width, int start, int end) {
        int available = width;
        for(int iter = start ; iter < end ; iter++) {
            Component c = target.getComponentAt(iter);
            available -= (c.getWidth() + c.getStyle().getMargin(false, Component.RIGHT) + 
                    c.getStyle().getMargin(false, Component.LEFT));
        }
        if(available > 0 && end - start > 0) {
            int perComponent = available / (end - start);
            int lastComponent = perComponent + available % (end - start);
            if(perComponent > 0) {
                int addOffset = 0;
                boolean rtl = target.isRTL();
                for(int iter = start ; iter < end - 1 ; iter++) {
                    Component c = target.getComponentAt(iter);
                    c.setWidth(c.getWidth() + perComponent);
                    if(rtl) {
                        addOffset += perComponent;
                        c.setX(c.getX() - addOffset);
                    } else {
                        c.setX(c.getX() + addOffset);
                        addOffset += perComponent;
                    }
                }
                Component c = target.getComponentAt(end - 1);
                if(rtl) {
                    addOffset += lastComponent;
                    c.setX(c.getX() - addOffset);
                } else {
                    c.setX(c.getX() + addOffset);
                }
                c.setWidth(c.getWidth() + lastComponent);
            } else {
                Component c = target.getComponentAt(end - 1);
                c.setWidth(c.getWidth() + lastComponent);
            }
        }
        
    }

    private void moveComponents(Container target, int x, int y, int width, int height, int rowStart, int rowEnd){
        moveComponents(target, x, y, width, height, rowStart, rowEnd, -1);
    }
            
    private void moveComponents(Container target, int x, int y, int width, int height, int rowStart, int rowEnd, int baseline ) {
        switch (orientation) {
            case Component.CENTER:
                // this will remove half of last gap
                if (target.isRTL()) {
                	x -= (width) / 2;
                } else {
                	x += (width) / 2;
                }
                break;
            case Component.RIGHT:
                x+=width;  // this will remove the last gap
                break;
        }
        Style parentStyle = target.getStyle();
        int parentPadding = parentStyle.getPadding(Component.LEFT) + parentStyle.getPadding(Component.RIGHT);


        for (int i = rowStart ; i < rowEnd ; i++) {
            Component m = target.getComponentAt(i);
            Style style = m.getStyle();
            int marginX = style.getMargin(false, Component.LEFT) + style.getMargin(false, Component.RIGHT);
            if(m.getWidth() + marginX < target.getWidth() - parentPadding){
                m.setX(m.getX()+ x);
            }
            int marginTop = m.getStyle().getMargin(false, Component.TOP);
            switch(valign) {
                case Component.BOTTOM:
                    if (vAlignByRow) {
                        m.setY(y + Math.max(marginTop, height - m.getHeight()));
                    } else {
                        m.setY(y + Math.max(marginTop, target.getHeight() - m.getHeight()));
                    }
                    break;
                case Component.CENTER:
                    if (vAlignByRow) {
                        m.setY(y + Math.max(marginTop, (height - m.getHeight()) / 2));                    
                    } else {
                        m.setY(y + Math.max(marginTop, (target.getHeight() - m.getHeight()) / 2));
                    }
                    break;
                case Component.BASELINE:
                    m.setY(y + Math.max(marginTop, baseline - m.getBaseline(m.getWidth(), m.getHeight())));
                    
                    break;
                default:
                    m.setY(y + marginTop);
                    break;
            }
        }
    }

    private Dimension dim = new Dimension(0, 0);

    /**
     * {@inheritDoc}
     */
    public  Dimension getPreferredSize(Container parent) {
        int parentWidth = parent.getWidth();
        if(parentWidth == 0){
            parent.invalidate();
        }
        int width = 0;
        int height = 0;
        int w = 0;
        int numOfcomponents = parent.getComponentCount();
        Style parentStyle = parent.getStyle();
        int parentPadding = parentStyle.getPadding(Component.LEFT) + parentStyle.getPadding(Component.RIGHT);

        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            height = Math.max(height, cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM));
            int prefW = cmp.getPreferredW()+ cmp.getStyle().getMargin(false, Component.RIGHT)+ cmp.getStyle().getMargin(false, Component.LEFT);
            w += prefW;
            //we need to break a line
            if (parentWidth > parentPadding && w > parentWidth && i > 0) {
                height += cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP) + cmp.getStyle().getMargin(false, Component.BOTTOM);
                width = Math.max(w, width);
                w = prefW;
            }
        }

        width = Math.max(w, width);

        dim.setWidth(width + parent.getStyle().getPadding(false, Component.LEFT)+ parent.getStyle().getPadding(false, Component.RIGHT));
        dim.setHeight(height + parent.getStyle().getPadding(false, Component.TOP)+ parent.getStyle().getPadding(false, Component.BOTTOM));
        return dim;
    }


    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "FlowLayout";
    }

    /**
     * Indicates whether the layout manager should try to fill up the available space
     * in the row
     *
     * @return the fillRows
     */
    public boolean isFillRows() {
        return fillRows;
    }

    /**
     * Indicates whether the layout manager should try to fill up the available space
     * in the row
     *
     * @param fillRows the fillRows to set
     */
    public void setFillRows(boolean fillRows) {
        this.fillRows = fillRows;
    }

    /**
     * Indicates vertical alignment within the flow layout
     *
     * @return Component.TOP/BOTTOM/CENTER
     */
    public int getValign() {
        return valign;
    }

    /**
     * Indicates vertical alignment within the flow layout
     *
     * @param valign one of Component.TOP/BOTTOM/CENTER
     */
    public void setValign(int valign) {
        this.valign = valign;
    }
    
    /**
     * When set to true vertical alignment will be performed by row (components within the container will be aligned vertically to each other in the same row)
     * When set to false (which is default) vertical alignment relates to the alignment of this container in regards to external components
     * 
     * @param internal true for internal, false otherwise
     */
    public void setValignByRow(boolean internal) {
        vAlignByRow=internal;
    }
    
    /**
     * Returns whether vertical alignment is done internally or externally 
     * 
     * @return whether vertical alignment is done internally or externally 
     */
    public boolean isValignByRow() {
        return vAlignByRow;
    }
    

    /**
     * Alignment of the flow layout, defaults to LEFT
     *
     * @return the orientation
     */
    public int getAlign() {
        return orientation;
    }

    /**
     * Alignment of the flow layout, defaults to LEFT
     *
     * @param orientation the orientation to set
     */
    public void setAlign(int orientation) {
        this.orientation = orientation;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return super.equals(o) && ((FlowLayout)o).orientation == orientation &&
                ((FlowLayout)o).valign == valign &&
                ((FlowLayout)o).fillRows == fillRows;
    }
    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(), cmps); see:
     * <script src="https://gist.github.com/codenameone/3481c77f93726745ad28.js"></script>
     * 
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseIn(Component... cmps) {
        return Container.encloseIn(new FlowLayout(), cmps);
    }
    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.CENTER), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseCenter(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.CENTER), cmps);
    }

    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.RIGHT), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseRight(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.RIGHT), cmps);
    }

    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.LEFT, Component.CENTER), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseMiddle(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.LEFT, Component.CENTER), cmps);
    }
    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.CENTER, Component.CENTER), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseCenterMiddle(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.CENTER, Component.CENTER), cmps);
    }

    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.RIGHT, Component.CENTER), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseRightMiddle(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.RIGHT, Component.CENTER), cmps);
    }

    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.LEFT, Component.BOTTOM), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseBottom(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.LEFT, Component.BOTTOM), cmps);
    }
    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.CENTER, Component.BOTTOM), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseCenterBottom(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.CENTER, Component.BOTTOM), cmps);
    }

    
    /**
     * Shorthand for Container.encloseIn(new FlowLayout(Component.RIGHT, Component.BOTTOM), cmps);
     * @param cmps the components to enclose in a new container
     * @return the new container
     */
    public static Container encloseRightBottom(Component... cmps) {
        return Container.encloseIn(new FlowLayout(Component.RIGHT, Component.BOTTOM), cmps);
    }
}