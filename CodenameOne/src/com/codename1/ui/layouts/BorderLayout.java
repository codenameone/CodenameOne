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
import com.codename1.ui.Display;
import com.codename1.ui.geom.*;
import com.codename1.ui.plaf.Style;
import java.util.HashMap;

/**
 * <p>A border layout lays out a container, arranging and resizing its 
 * components to fit in five regions: north, south, east, west, and center. 
 * Each region may contain no more than one component, and is identified by a 
 * corresponding constant: NORTH, SOUTH, EAST, WEST, and CENTER. 
 * When adding a component to a container with a border layout, use one of 
 * these five constants.</p>
 * <p>
 * The border layout scales all of the components within it to match the available 
 * constraints. The NORTH &amp; SOUTH components use their preferred height but
 * are stretched to take up the full width available. The EAST &amp; WEST do the same
 * for the reverse axis however they leave room for the NORTH/SOUTH entries if they
 * are defined.<br>
 * The CENTER constraint will take up the rest of the available space regardless of its preferred
 * size. This is normally very useful, however in some cases we would prefer that the center
 * component will actually position itself in the middle of the available space. For this we have
 * the <code>setCenterBehavior</code> method.
 * </p>
 * <p>
 * Because of its scaling behavior scrolling a border layout makes no sense. However it is a 
 * common mistake to apply a border layout to a scrollable container or trying to make a border
 * layout scrollable. That is why the {@link com.codename1.ui.Container} class explicitly blocks
 * scrolling on a BorderLayout.<br>
 * Typical usage of this class:
 * </p>
 * <script src="https://gist.github.com/codenameone/23e642b1a749e2f37e68.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/border-layout.png" alt="Border Layout" />
 *
 * <p>
 * When defining the center behavior we can get very different results:
 * </p>
 *<script src="https://gist.github.com/codenameone/108aa105386ed7c340ad.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/border-layout-center.png" alt="Border Layout Center" />
 * 
 * <p>Notice that in the case of RTL (right to left language also known as bidi) the
 * EAST and WEST values are implicitly reversed as shown in this image:
 * </p>
 * <img src="https://www.codenameone.com/img/developer-guide/border-layout-RTL.png" alt="Border Layout bidi/RTL" />
 * 
 * <p>
 * You can read further in the <a href="https://www.codenameone.com/manual/basics.html#_border_layout">BorderLayout section in the developer guide</a>.
 * </p>
 * 
 * @author Nir Shabi, Shai Almog
 */
public class BorderLayout extends Layout {
    private boolean scaleEdges = true;
    
    /**
     * Defines the behavior of the component placed in the center position of the layout, by default it is scaled to the available space
     */
    public static final int CENTER_BEHAVIOR_SCALE = 0;

    /**
     * Defines the behavior of the component placed in the center position of the layout, places the component in the center of
     * the space available to the center component.
     */
    public static final int CENTER_BEHAVIOR_CENTER = 1;


    /**
     * Defines the behavior of the component placed in the center position of the layout, places the component in the center of
     * the surrounding container
     */
    public static final int CENTER_BEHAVIOR_CENTER_ABSOLUTE = 2;

    /**
     * Deprecated due to spelling mistake, use CENTER_BEHAVIOR_TOTAL_BELOW
     * The center component takes up the entire screens and the sides are automatically placed on top of it thus creating
     * a layered effect
     * @deprecated Deprecated due to spelling mistake, use CENTER_BEHAVIOR_TOTAL_BELOW
     */
    public static final int CENTER_BEHAVIOR_TOTAL_BELLOW = 3;

    /**
     * The center component takes up the entire screens and the sides are automatically placed on top of it thus creating
     * a layered effect
     */
    public static final int CENTER_BEHAVIOR_TOTAL_BELOW = 3;
    
    private Component portraitNorth;
    private Component portraitSouth;
    private Component portraitCenter;
    private Component portraitWest;
    private Component portraitEast;

    private HashMap<String, String> landscapeSwap;

    /**
     * Defines the behavior of the center component to one of the constants defined in this class
     */
    private int centerBehavior;

    /**
     * The north layout constraint (top of container).
     */
    public static final String NORTH = "North";
    /**
     * The south layout constraint (bottom of container).
     */
    public static final String SOUTH = "South";
    /**
     * The center layout constraint (middle of container)
     */
    public static final String CENTER = "Center";
    /**
     * The west layout constraint (left of container).
     */
    public static final String WEST = "West";
    /**
     * The east layout constraint (right of container).
     */
    public static final String EAST = "East";

    /** 
     * Creates a new instance of BorderLayout 
     */
    public BorderLayout() {
    }

    /** 
     * Creates a new instance of BorderLayout  with absolute behavior
     * @param  behavior identical value as the setCenterBehavior method
     */
    public BorderLayout(int behavior) {
        setCenterBehavior(behavior);
    }

    /**
     * {@inheritDoc}
     */
    public void addLayoutComponent(Object name, Component comp, Container c) {
        // helper check for a common mistake...
        if (name == null) {
            throw new IllegalArgumentException("Cannot add component to BorderLayout Container without constraint parameter");
        }

        Component previous = null;

        /* Assign the component to one of the known regions of the layout.
         */
        if (CENTER.equals(name)) {
            previous = portraitCenter;
            portraitCenter = comp;
        } else if (NORTH.equals(name)) {
            previous = portraitNorth;
            portraitNorth = comp;
        } else if (SOUTH.equals(name)) {
            previous = portraitSouth;
            portraitSouth = comp;
        } else if (EAST.equals(name)) {
            previous = portraitEast;
            portraitEast = comp;
        } else if (WEST.equals(name)) {
            previous = portraitWest;
            portraitWest = comp;
        } else {
            throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
        }
        if (previous != null && previous != comp) {
            c.removeComponent(previous);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeLayoutComponent(Component comp) {
        if (comp == portraitCenter) {
            portraitCenter = null;
        } else if (comp == portraitNorth) {
            portraitNorth = null;
        } else if (comp == portraitSouth) {
            portraitSouth = null;
        } else if (comp == portraitEast) {
            portraitEast = null;
        } else if (comp == portraitWest) {
            portraitWest = null;
        }
    }

    /**
     * Returns the component constraint
     * 
     * @param comp the component whose constraint is queried
     * @return one of the constraints defined in this class
     */
    public Object getComponentConstraint(Component comp) {
        if (comp == portraitCenter) {
            return CENTER;
        } else if (comp == portraitNorth) {
            return NORTH;
        } else if (comp == portraitSouth) {
            return SOUTH;
        } else if (comp == portraitEast) {
            return EAST;
        } else {
            if(comp == portraitWest) {
                return WEST;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container target) {
        Style s = target.getStyle();
        int top = s.getPadding(false, Component.TOP);
        int bottom = target.getLayoutHeight() - target.getBottomGap() - s.getPadding(false, Component.BOTTOM);
        int left = s.getPadding(target.isRTL(), Component.LEFT);
        int right = target.getLayoutWidth() - target.getSideGap() - s.getPadding(target.isRTL(), Component.RIGHT);
        int targetWidth = target.getWidth();
        int targetHeight = target.getHeight();

        boolean rtl = target.isRTL();
        if (rtl) {
        	left+=target.getSideGap();
        }

        Component east = getEast();
        Component west = getWest();
        Component south = getSouth();
        Component north = getNorth();
        Component center = getCenter();
        if (north != null) {
            Component c = north;
            positionTopBottom(target, c, right, left, targetHeight);
            c.setY(top + c.getStyle().getMargin(false, Component.TOP));
            top += (c.getHeight() + c.getStyle().getMargin(false, Component.TOP) + c.getStyle().getMargin(false, Component.BOTTOM));
        }
        if (south != null) {
            Component c = south;
            positionTopBottom(target, c, right, left, targetHeight);
            c.setY(bottom - c.getHeight() - c.getStyle().getMargin(false, Component.BOTTOM));
            bottom -= (c.getHeight() + c.getStyle().getMargin(false, Component.TOP) + c.getStyle().getMargin(false, Component.BOTTOM));
        }

        Component realEast = east;
        Component realWest = west;

        if (rtl) {
        	realEast = west;
        	realWest = east;
        }

        if (realEast != null) {
            Component c = realEast;
            positionLeftRight(realEast, targetWidth, bottom, top);
            c.setX(right - c.getWidth() - c.getStyle().getMargin(target.isRTL(), Component.RIGHT));
            right -= (c.getWidth() + c.getStyle().getMargin(false, Component.LEFT) + c.getStyle().getMargin(false, Component.RIGHT));
        }
        if (realWest != null) {
            Component c = realWest;
            positionLeftRight(realWest, targetWidth, bottom, top);
            c.setX(left + c.getStyle().getMargin(target.isRTL(), Component.LEFT));
            left += (c.getWidth() + c.getStyle().getMargin(false, Component.LEFT) + c.getStyle().getMargin(false, Component.RIGHT));
        }
        if (center != null) {
            Component c = center;
            int w = right - left - c.getStyle().getMargin(false, Component.LEFT) - c.getStyle().getMargin(false, Component.RIGHT);
            int h = bottom - top - c.getStyle().getMargin(false, Component.TOP) - c.getStyle().getMargin(false, Component.BOTTOM);
            int x = left + c.getStyle().getMargin(target.isRTL(), Component.LEFT);
            int y = top + c.getStyle().getMargin(false, Component.TOP);
            switch(centerBehavior) {
                case CENTER_BEHAVIOR_CENTER_ABSOLUTE: {
                    Dimension d = c.getPreferredSize();
                    if(d.getWidth() < w) {
                        int newX = s.getPadding(target.isRTL(), Component.LEFT) + targetWidth / 2 - d.getWidth() / 2;
                        if(newX > x) {
                            x = newX;
                        }
                        w = d.getWidth();
                    }
                    int append = 0;
                    int th = targetHeight;
                    if(north != null) {
                        append = north.getHeight();
                        th -= append;
                    }
                    if(south != null) {
                        th -= south.getHeight();
                        append += south.getHeight();
                    }
                    if(d.getHeight() < h) {
                        int newY = (s.getPadding(false, Component.TOP) + th) / 2 - d.getHeight() / 2 + append;
                        if(newY > y) {
                            y = newY;
                        }
                        h = d.getHeight();
                    }
                    break;
                }
                case CENTER_BEHAVIOR_CENTER: {
                    Dimension d = c.getPreferredSize();
                    if(d.getWidth() < w) {
                        x += w / 2 - d.getWidth() / 2;
                        w = d.getWidth();
                    }
                    if(d.getHeight() < h) {
                        y += h / 2 - d.getHeight() / 2;
                        h = d.getHeight();
                    }
                    break;
                }
                case CENTER_BEHAVIOR_TOTAL_BELOW: {
                    w = targetWidth;
                    h = targetHeight;
                    x = s.getPadding(target.isRTL(), Component.LEFT);
                    y = s.getPadding(false, Component.TOP);;
                }
            } 
            c.setWidth(w);
            c.setHeight(h);
            c.setX(x);
            c.setY(y);
        }
    }

    /**
     * Position the east/west component variables
     */
    private void positionLeftRight(Component c, int targetWidth, int bottom, int top) {
        int y = top + c.getStyle().getMargin(false, Component.TOP);
        int h = bottom - top - c.getStyle().getMargin(false, Component.TOP) - c.getStyle().getMargin(false, Component.BOTTOM);
        if(scaleEdges) {
            c.setY(y);
            c.setHeight(h); 
        } else { 
            int ph = c.getPreferredH();
            if(ph < h) {
                c.setHeight(ph);
                c.setY(y + (h - ph) / 2);
            } else {
                c.setY(y);
                c.setHeight(h); 
            }
        }
        c.setWidth(Math.min(targetWidth, c.getPreferredW()));
    }
    
    private void positionTopBottom(Component target, Component c, int right, int left, int targetHeight) {
        int w = right - left - c.getStyle().getMargin(false, Component.LEFT) - c.getStyle().getMargin(false, Component.RIGHT);
        int x = left + c.getStyle().getMargin(target.isRTL(), Component.LEFT);
        if(scaleEdges) {
            c.setWidth(w);
            c.setX(x);
        } else {
            int pw = c.getPreferredW();
            if(pw < w) {
                c.setWidth(pw);
                c.setX(x + (w - pw) / 2);
            } else {
                c.setWidth(w);
                c.setX(x);
            }
        }
        c.setHeight(Math.min(targetHeight, c.getPreferredH())); //verify I want to use tge prefered size
    }

    private Dimension dim = new Dimension(0, 0);
    
    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize(Container parent) {
        dim.setWidth(0);
        dim.setHeight(0);
        Component east = getEast();
        Component west = getWest();
        Component south = getSouth();
        Component north = getNorth();
        Component center = getCenter();

        if (east != null) {
            dim.setWidth(east.getPreferredW() + east.getStyle().getMargin(false, Component.LEFT) + east.getStyle().getMargin(false, Component.RIGHT));
            dim.setHeight(Math.max(east.getPreferredH() + east.getStyle().getMargin(false, Component.TOP) + east.getStyle().getMargin(false, Component.BOTTOM), dim.getHeight()));
        }
        if (west != null) {
            dim.setWidth(dim.getWidth() + west.getPreferredW() + west.getStyle().getMargin(false, Component.LEFT) + west.getStyle().getMargin(false, Component.RIGHT));
            dim.setHeight(Math.max(west.getPreferredH() + west.getStyle().getMargin(false, Component.TOP) + west.getStyle().getMargin(false, Component.BOTTOM), dim.getHeight()));
        }
        if (center != null) {
            dim.setWidth(dim.getWidth() + center.getPreferredW() + center.getStyle().getMargin(false, Component.LEFT) + center.getStyle().getMargin(false, Component.RIGHT));
            dim.setHeight(Math.max(center.getPreferredH() + center.getStyle().getMargin(false, Component.TOP) + center.getStyle().getMargin(false, Component.BOTTOM), dim.getHeight()));
        }
        if (north != null) {
            dim.setWidth(Math.max(north.getPreferredW() + north.getStyle().getMargin(false, Component.LEFT) + north.getStyle().getMargin(false, Component.RIGHT), dim.getWidth()));
            dim.setHeight(dim.getHeight() + north.getPreferredH() + north.getStyle().getMargin(false, Component.TOP) + north.getStyle().getMargin(false, Component.BOTTOM));
        }

        if (south != null) {
            dim.setWidth(Math.max(south.getPreferredW() + south.getStyle().getMargin(false, Component.LEFT) + south.getStyle().getMargin(false, Component.RIGHT), dim.getWidth()));
            dim.setHeight(dim.getHeight() + south.getPreferredH() + south.getStyle().getMargin(false, Component.TOP) + south.getStyle().getMargin(false, Component.BOTTOM));
        }

        dim.setWidth(dim.getWidth() + parent.getStyle().getPadding(false, Component.LEFT) + parent.getStyle().getPadding(false, Component.RIGHT));
        dim.setHeight(dim.getHeight() + parent.getStyle().getPadding(false, Component.TOP) + parent.getStyle().getPadding(false, Component.BOTTOM));
        return dim;
    }

    private boolean isLandscape() {
        Display d = Display.getInstance();
        return d.getDisplayWidth() > d.getDisplayHeight();
    }

    /**
     * Returns the component at the given constraint
     */
    private Component getComponentAtIgnoreLandscape(String constraint) {
        if(constraint != null) {
            if(constraint.equals(NORTH)) {
                return portraitNorth;
            }
            if(constraint.equals(SOUTH)) {
                return portraitSouth;
            }
            if(constraint.equals(EAST)) {
                return portraitEast;
            }
            if(constraint.equals(WEST)) {
                return portraitWest;
            }
            if(constraint.equals(CENTER)) {
                return portraitCenter;
            }
        }
        return null;
    }

    private Component getComponentImpl(Component noLandscape, String orientation) {
        if(landscapeSwap != null && isLandscape()) {
            String s = (String)landscapeSwap.get(orientation);
            if(s != null) {
                return getComponentAtIgnoreLandscape(s);
            }
        }
        return noLandscape;
    }

    /**
     * Returns the component in the south location
     * 
     * @return the component in the constraint
     */
    public Component getSouth() {
        return getComponentImpl(portraitSouth, SOUTH);
    }

    /**
     * Returns the component in the center location
     * 
     * @return the component in the constraint
     */
    public Component getCenter() {
        return getComponentImpl(portraitCenter, CENTER);
    }

    /**
     * Returns the component in the north location
     * 
     * @return the component in the constraint
     */
    public Component getNorth() {
        return getComponentImpl(portraitNorth, NORTH);
    }

    /**
     * Returns the component in the east location
     * 
     * @return the component in the constraint
     */
    public Component getEast() {
        return getComponentImpl(portraitEast, EAST);
    }

    /**
     * Returns the component in the west location
     * 
     * @return the component in the constraint
     */
    public Component getWest() {
        return getComponentImpl(portraitWest, WEST);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "BorderLayout";
    }

    /**
     * This method allows swapping positions within the border layout when the layout
     * orientation changes to landscape or if the layout starts off as landscape.
     *
     * @param portraitPosition the position for the component when in portrait (this position
     * should always be used when adding a component to the layout). One of NORTH/SOUTH/EAST/WEST/CENTER.
     * @param landscapePosition the destination position to use in landscape
     */
    public void defineLandscapeSwap(String portraitPosition, String landscapePosition) {
        if(landscapeSwap == null) {
            landscapeSwap = new HashMap<String, String>();
        }
        landscapeSwap.put(portraitPosition, landscapePosition);
        landscapeSwap.put(landscapePosition, portraitPosition);
    }

    /**
     * Returns the landscape swap destination for the given border layout element if such
     * a destination is defined.
     *
     * @param portraitPosition the constraint used when placing the component
     * @return the constraint to use when in landscape or null if undefined
     */
    public String getLandscapeSwap(String portraitPosition) {
        if(landscapeSwap == null) {
            return null;
        }
        return (String)landscapeSwap.get(portraitPosition);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if(super.equals(o) && centerBehavior == ((BorderLayout)o).centerBehavior) {
            if(landscapeSwap == ((BorderLayout)o).landscapeSwap) {
                return true;
            }
            if(landscapeSwap != null) {
                return landscapeSwap.equals(((BorderLayout)o).landscapeSwap);
            }
        }
        return false;
    }

    /**
     * Indicates that the center shouldn't grow and should be placed exactly in the center of the layout
     * 
     * @return the absoluteCenter
     * @deprecated use center behavior instead
     */
    public boolean isAbsoluteCenter() {
        return centerBehavior == CENTER_BEHAVIOR_CENTER;
    }

    /**
     * Indicates that the center shouldn't grow and should be placed exactly in the center of the layout
     *
     * @param absoluteCenter the absoluteCenter to set
     * @deprecated use center behavior instead
     */
    public void setAbsoluteCenter(boolean absoluteCenter) {
        if(absoluteCenter) {
            setCenterBehavior(CENTER_BEHAVIOR_CENTER);
        } else {
            setCenterBehavior(CENTER_BEHAVIOR_SCALE);
        }
    }

    /**
     * Defines the behavior of the center component to one of the constants defined in this class
     *
     * @return the centerBehavior
     */
    public int getCenterBehavior() {
        return centerBehavior;
    }

    /**
     * Defines the behavior of the center component to one of the constants defined in this class
     *
     * @param centerBehavior the centerBehavior to set
     */
    public void setCenterBehavior(int centerBehavior) {
        this.centerBehavior = centerBehavior;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOverlapSupported(){
        return centerBehavior == CENTER_BEHAVIOR_TOTAL_BELOW;
    }

    /**
     * Stretches the edge components (NORTH/EAST/WEST/SOUTH)
     * @return the scaleEdges
     */
    public boolean isScaleEdges() {
        return scaleEdges;
    }

    /**
     * Stretches the edge components (NORTH/EAST/WEST/SOUTH)
     * @param scaleEdges the scaleEdges to set
     */
    public void setScaleEdges(boolean scaleEdges) {
        this.scaleEdges = scaleEdges;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isConstraintTracking() {
        return false;
    }    
    
    /**
     * {@inheritDoc}
     */
    public boolean obscuresPotential(Container parent) {
        return getCenter() != null;
    }
    
    /**
     * Convenience method that creates a border layout container and places the given component in the center
     * @param center the center component
     * @return the created component
     */
    public static Container center(Component center) {
        return Container.encloseIn(new BorderLayout(), center, BorderLayout.CENTER);
    }

    /**
     * Convenience method that creates a border layout container and places the given component in the center
     * with the {@link  #CENTER_BEHAVIOR_CENTER} constraint applied
     * @param center the center component
     * @return the created component
     */
    public static Container centerCenter(Component center) {
        return Container.encloseIn(new BorderLayout(CENTER_BEHAVIOR_CENTER), center, BorderLayout.CENTER);
    }

    /**
     * Convenience method that creates a border layout container and places the given component in the center
     * with the {@link  #CENTER_BEHAVIOR_CENTER_ABSOLUTE} constraint applied
     * @param center the center component
     * @return the created component
     */
    public static Container centerAbsolute(Component center) {
        return Container.encloseIn(new BorderLayout(CENTER_BEHAVIOR_CENTER_ABSOLUTE), center, BorderLayout.CENTER);
    }

    /**
     * Convenience method that creates a border layout container and places the given component in the north
     * @param north the north component
     * @return the created component
     */
    public static Container north(Component north) {
        return Container.encloseIn(new BorderLayout(), north, BorderLayout.NORTH);
    }

    /**
     * Convenience method that creates a border layout container and places the given component in the south
     * @param south the south component
     * @return the created component
     */
    public static Container south(Component south) {
        return Container.encloseIn(new BorderLayout(), south, BorderLayout.SOUTH);
    }

    /**
     * Convenience method that creates a border layout container and places the given component in the east
     * @param east the east component
     * @return the created component
     */
    public static Container east(Component east) {
        return Container.encloseIn(new BorderLayout(), east, BorderLayout.EAST);
    }


    /**
     * Convenience method that creates a border layout container and places the given component in the west
     * @param west the west component
     * @return the created component
     */
    public static Container west(Component west) {
        return Container.encloseIn(new BorderLayout(), west, BorderLayout.WEST);
    }
}
