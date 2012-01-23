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
import com.codename1.ui.plaf.UIManager;
import java.util.Hashtable;

/**
 * A border layout lays out a container, arranging and resizing its 
 * components to fit in five regions: north, south, east, west, and center. 
 * Each region may contain no more than one component, and is identified by a 
 * corresponding constant: NORTH, SOUTH, EAST, WEST, and CENTER. 
 * When adding a component to a container with a border layout, use one of 
 * these five constants.
 *
 * @author Nir Shabi
 */
public class BorderLayout extends Layout {
    /**
     * Defines the behavior of the component placed in the center position of the layout, by default it is scaled to the available space
     */
    public static final int CENTER_BEHAVIOR_SCALE = 0;

    /**
     * Defines the behavior of the component placed in the center position of the layout, places the component in the center of
     * the space availble to the center component.
     */
    public static final int CENTER_BEHAVIOR_CENTER = 1;


    /**
     * Defines the behavior of the component placed in the center position of the layout, places the component in the center of
     * the surrounding container
     */
    public static final int CENTER_BEHAVIOR_CENTER_ABSOLUTE = 2;

    private Component portaraitNorth;
    private Component portaraitSouth;
    private Component portaraitCenter;
    private Component portaraitWest;
    private Component portaraitEast;

    private Hashtable landscapeSwap;

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
     * @inheritDoc
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
            previous = portaraitCenter;
            portaraitCenter = comp;
        } else if (NORTH.equals(name)) {
            previous = portaraitNorth;
            portaraitNorth = comp;
        } else if (SOUTH.equals(name)) {
            previous = portaraitSouth;
            portaraitSouth = comp;
        } else if (EAST.equals(name)) {
            previous = portaraitEast;
            portaraitEast = comp;
        } else if (WEST.equals(name)) {
            previous = portaraitWest;
            portaraitWest = comp;
        } else {
            throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
        }
        if (previous != null && previous != comp) {
            c.removeComponent(previous);
        }
    }

    /**
     * @inheritDoc
     */
    public void removeLayoutComponent(Component comp) {
        if (comp == portaraitCenter) {
            portaraitCenter = null;
        } else if (comp == portaraitNorth) {
            portaraitNorth = null;
        } else if (comp == portaraitSouth) {
            portaraitSouth = null;
        } else if (comp == portaraitEast) {
            portaraitEast = null;
        } else if (comp == portaraitWest) {
            portaraitWest = null;
        }
    }

    /**
     * Returns the component constraint
     * 
     * @param comp the component whose constraint is queried
     * @return one of the constraints defined in this class
     */
    public Object getComponentConstraint(Component comp) {
        if (comp == portaraitCenter) {
            return CENTER;
        } else if (comp == portaraitNorth) {
            return NORTH;
        } else if (comp == portaraitSouth) {
            return SOUTH;
        } else if (comp == portaraitEast) {
            return EAST;
        } else {
            if(comp == portaraitWest) {
                return WEST;
            }
        }
        return null;
    }

    /**
     * @inheritDoc
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
                        int newX = targetWidth / 2 - d.getWidth() / 2;
                        if(newX > x) {
                            x = newX;
                        }
                        w = d.getWidth();
                    }
                    if(d.getHeight() < h) {
                        int newY = targetHeight / 2 - d.getHeight() / 2;
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
        c.setWidth(Math.min(targetWidth, c.getPreferredW()));
        c.setHeight(bottom - top - c.getStyle().getMargin(false, Component.TOP) - c.getStyle().getMargin(false, Component.BOTTOM)); //verify I want to use tge prefered size
        c.setY(top + c.getStyle().getMargin(false, Component.TOP));
    }
    
    private void positionTopBottom(Component target, Component c, int right, int left, int targetHeight) {
        c.setWidth(right - left - c.getStyle().getMargin(false, Component.LEFT) - c.getStyle().getMargin(false, Component.RIGHT));
        c.setHeight(Math.min(targetHeight, c.getPreferredH())); //verify I want to use tge prefered size
        c.setX(left + c.getStyle().getMargin(target.isRTL(), Component.LEFT));
    }
    
    /**
     * @inheritDoc
     */
    public Dimension getPreferredSize(Container parent) {
        Dimension dim = new Dimension(0, 0);

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
                return portaraitNorth;
            }
            if(constraint.equals(SOUTH)) {
                return portaraitSouth;
            }
            if(constraint.equals(EAST)) {
                return portaraitEast;
            }
            if(constraint.equals(WEST)) {
                return portaraitWest;
            }
            if(constraint.equals(CENTER)) {
                return portaraitCenter;
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
        return getComponentImpl(portaraitSouth, SOUTH);
    }

    /**
     * Returns the component in the center location
     * 
     * @return the component in the constraint
     */
    public Component getCenter() {
        return getComponentImpl(portaraitCenter, CENTER);
    }

    /**
     * Returns the component in the north location
     * 
     * @return the component in the constraint
     */
    public Component getNorth() {
        return getComponentImpl(portaraitNorth, NORTH);
    }

    /**
     * Returns the component in the east location
     * 
     * @return the component in the constraint
     */
    public Component getEast() {
        return getComponentImpl(portaraitEast, EAST);
    }

    /**
     * Returns the component in the west location
     * 
     * @return the component in the constraint
     */
    public Component getWest() {
        return getComponentImpl(portaraitWest, WEST);
    }

    /**
     * @inheritDoc
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
            landscapeSwap = new Hashtable();
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
     * @inheritDoc
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
}
