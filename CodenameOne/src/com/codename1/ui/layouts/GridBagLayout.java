/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.codename1.ui.layouts;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Codename One port of the infamous GridBagLayout based on the Apache Harmony code
 */
public class GridBagLayout extends Layout {
    protected static final int MAXGRIDSIZE = 512;

    protected static final int MINSIZE = 1;

    protected static final int PREFERREDSIZE = 2;

    // Direct modification is forbidden
    protected volatile Hashtable<Component, GridBagConstraints> comptable;

    protected volatile GridBagConstraints defaultConstraints;

    protected volatile GridBagLayoutInfo layoutInfo;

    public volatile double columnWeights[];

    public volatile double rowWeights[];

    public volatile int columnWidths[];

    public volatile int rowHeights[];

    private ParentInfo lastParentInfo;

    public GridBagLayout() {
        comptable = new Hashtable<Component, GridBagConstraints>();
        defaultConstraints = new GridBagConstraints();
        columnWeights = rowWeights = null;
        columnWidths = rowHeights = null;
        layoutInfo = null;
        lastParentInfo = null;
    }

 
    public void addLayoutComponent(Object constraints, Component comp, Container c) {
        GridBagConstraints cons;
        if (constraints != null) {
            if (!(constraints instanceof GridBagConstraints)) {
                throw new IllegalArgumentException("AddLayoutComponent: constraint object must be GridBagConstraints"); //$NON-NLS-1$
            }
            cons = (GridBagConstraints) constraints;
        } else {
            if (comptable.containsKey(comp)) {
                //  don't replace constraints with default ones
                return;
            }
            cons = defaultConstraints;
        }
        try {
            //cons.verify();
        } catch (IllegalArgumentException e) {
            // awt.81=AddLayoutComponent: {0}
            throw new IllegalArgumentException("AddLayoutComponent: " + e.getMessage()); //$NON-NLS-1$
        }
        GridBagConstraints consClone = (GridBagConstraints) cons.clone();
        comptable.put(comp, consClone);
        Container parent = comp.getParent();
        updateParentInfo(parent, consClone);
    }

    public void removeLayoutComponent(Component comp) {
        Container parent = comp.getParent();
        if (parent != null) {
            getParentInfo(parent).consTable.remove(comptable.get(comp));
        }
        comptable.remove(comp);
    }

    public Object getComponentConstraint(Component comp) {
        GridBagConstraints cons = comptable.get(comp);
        if (cons == null) {
            cons = defaultConstraints;
            comptable.put(comp, (GridBagConstraints) cons.clone());
        }
        return (GridBagConstraints) cons.clone();
    }

    public void setConstraints(Component comp, GridBagConstraints constraints) {
        GridBagConstraints consClone = (GridBagConstraints) constraints.clone();
        try {
            //                consClone.verify();
        } catch (IllegalArgumentException e) {
            // awt.85=SetConstraints: {0}
            throw new IllegalArgumentException("SetConstraints: " + e.getMessage()); //$NON-NLS-1$
        }
        ParentInfo info = getParentInfo(comp.getParent());
        if (info != null) {
            GridBagConstraints cons = comptable.get(comp);
            info.allConstraints.remove(info.consTable.get(cons)); //?
            info.consTable.remove(cons);
        }
        // add component if it's not there yet
        comptable.put(comp, consClone);
        if (info != null) {
            MixedConstraints mixCons = new MixedConstraints(consClone);
            info.consTable.put(consClone, mixCons);
            info.allConstraints.add(mixCons);
        }
    }

    public void invalidateLayout(Container target) {
        if (target == null) {
            return;
        }
        getParentInfo(target).valid = false;
    }

    public Dimension getPreferredSize(Container parent) {
        Style s = parent.getStyle();
        ParentInfo info = lastParentInfo = getParentInfo(parent);
        if (getComponentsNumber(parent) == 0) {
            return new Dimension(s.getPadding(Component.LEFT) + s.getPadding(Component.RIGHT), s.getPadding(Component.TOP) + s.getPadding(Component.BOTTOM));
        }
        try {
            validate(parent, info);
        } catch (RuntimeException e) {
            // awt.87=PreferredLayoutSize: {0}
            throw new IllegalArgumentException("PreferredLayoutSize: " + e.getMessage()); //$NON-NLS-1$
        }
        Dimension d = info.grid.preferredSize();
        d.setWidth(d.getWidth() + s.getPadding(Component.LEFT) + s.getPadding(Component.RIGHT));
        d.setHeight(d.getHeight() + s.getPadding(Component.TOP) + s.getPadding(Component.BOTTOM));
        return d;
        //return addInsets(grid.preferredSize(), parent);
    }

    public void layoutContainer(Container parent) {
        ParentInfo info = lastParentInfo = getParentInfo(parent);
        if (getComponentsNumber(parent) == 0) {
            return;
        }
        try {
            arrangeGridImpl(parent, info);
        } catch (RuntimeException e) {
            // awt.88=LayoutContainer: {0}
            throw new IllegalArgumentException("LayoutContainer: " + e.getMessage()); //$NON-NLS-1$
        }
        setComponentsBounds(info);
    }

    public int[][] getLayoutDimensions() {
        if (lastParentInfo == null) {
            return new int[][] { new int[0], new int[0] };
        }
        return new int[][] { lastParentInfo.grid.getWidths(),
                lastParentInfo.grid.getHeights() };
    }

    public double[][] getLayoutWeights() {
        if (lastParentInfo == null) {
            return new double[][] { new double[0], new double[0] };
        }
        return lastParentInfo.grid.getWeights();
    }

    /*public Point location(int x, int y) {
        toolkit.lockAWT();
        try {
            if (lastParentInfo == null) {
                return new Point();
            }
            return lastParentInfo.grid.location(x, y, lastParentInfo.orientation
                    .isLeftToRight());
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected Dimension GetMinSize(Container parent, GridBagLayoutInfo info) {
        toolkit.lockAWT();
        try {
            int w = 0;
            int h = 0;
            for (int i = 0; i < MAXGRIDSIZE; i++) {
                w += info.widths[i];
                h += info.heights[i];
            }
            return new Dimension(w, h);
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected GridBagLayoutInfo GetLayoutInfo(Container parent, int sizeflag) {
        toolkit.lockAWT();
        try {
            ParentInfo parentInfo = getParentInfo(parent);
            if (sizeflag == PREFERREDSIZE) {
                return new GridBagLayoutInfo(parentInfo.grid.lookupPrefWidths(),
                        parentInfo.grid.lookupPrefHeights());
            }
            // MINSIZE
            return new GridBagLayoutInfo(parentInfo.grid.lookupMinWidths(), parentInfo.grid
                    .lookupMinHeights());
        } finally {
            toolkit.unlockAWT();
        }
    }*/

    protected void ArrangeGrid(Container parent) {
        ParentInfo info = lastParentInfo = getParentInfo(parent);
        if (getComponentsNumber(parent) == 0) {
            return;
        }
        try {
            arrangeGridImpl(parent, info);
        } catch (RuntimeException e) {
            // awt.86=MinimumLayoutSize: {0}
            throw new IllegalArgumentException("MinimumLayoutSize: " + e.getMessage()); //$NON-NLS-1$
        }
    }

    protected GridBagConstraints lookupConstraints(Component comp) {
        GridBagConstraints cons = comptable.get(comp);
        if (cons == null) {
            // if comp is not in the layout, return a copy of default constraints
            cons = (GridBagConstraints) defaultConstraints.clone();
        }
        return cons;
    }

    protected void adjustForGravity(GridBagConstraints constraints, Rectangle r) {
        AdjustForGravity(constraints, r);
    }

    protected void arrangeGrid(Container parent) {
        ArrangeGrid(parent);
    }

    /*protected GridBagLayoutInfo getLayoutInfo(Container parent, int sizeflag) {
        return GetLayoutInfo(parent, sizeflag);
    }

    protected Dimension getMinSize(Container parent, GridBagLayoutInfo info) {
        toolkit.lockAWT();
        try {
            return GetMinSize(parent, info);
        } finally {
            toolkit.unlockAWT();
        }
    }*/

    protected void AdjustForGravity(GridBagConstraints constraints, Rectangle r) {
        try {
            //                ((GridBagConstraints) constraints).verify();
        } catch (IllegalArgumentException e) {
            // awt.8C={0}
            throw new IllegalArgumentException("AdjustForGravity: " + e.getMessage()); //$NON-NLS-1$
        }
        //Don't get parent as param, so have to use older info if exists
        if (layoutInfo == null) {
            r.setBounds(0, 0, 0, 0);
            return;
        }
        GridBagConstraints consClone = (GridBagConstraints) constraints.clone();
        consClone.fill = GridBagConstraints.BOTH;
        ComponentSide horSide = new ComponentSide();
        ComponentSide vertSide = new ComponentSide();
        Dimension dummySize = new Dimension(0, 0);
        initHorCompSide(horSide, consClone, dummySize, dummySize, lastParentInfo);
        initVertCompSide(vertSide, consClone, dummySize, dummySize, lastParentInfo);
        calculateComponentBounds(horSide, vertSide, r, lastParentInfo.grid);
    }

    private ParentInfo getParentInfo(Container parent) {
        if (parent == null) {
            return null;
        }
        ParentInfo pi = (ParentInfo)parent.getClientProperty("cn1$gridBagLayoutData");
        if(pi == null) {
            pi = new ParentInfo();
            parent.putClientProperty("cn1$gridBagLayoutData", pi);
        }
        return pi;
    }

    private void arrangeGridImpl(Container parent, ParentInfo info) {
        validate(parent, info);
        // Do not check clientRect for emptiness. Grid must be updated anyway
        Rectangle clientRect = new Rectangle(0, 0, parent.getWidth(), parent.getHeight());
        info.grid.fit2Client(clientRect);
    }

    private void setComponentsBounds(ParentInfo info) {
        for (int i = 0; i < info.components.length; i++) {
            Rectangle r = new Rectangle();
            calculateComponentBounds(info.horCompSides[i], info.vertCompSides[i], r, info.grid);
            info.components[i].setX(r.getX());
            info.components[i].setY(r.getY());
            info.components[i].setWidth(r.getSize().getWidth());
            info.components[i].setHeight(r.getSize().getHeight());
        }
    }

    private void calculateComponentBounds(ComponentSide horSide, ComponentSide vertSide,
            Rectangle r, Grid grid) {
        Rectangle dispArea = grid.componentDisplayArea(horSide, vertSide);
        r.setWidth(fillDisplaySide(dispArea.getWidth(), horSide));
        r.setHeight(fillDisplaySide(dispArea.getHeight(), vertSide));
        r.setX(anchorComponentSide(dispArea.getX(), dispArea.getWidth(), horSide, r.getWidth()));
        r.setY(anchorComponentSide(dispArea.getY(), dispArea.getHeight(), vertSide, r.getHeight()));
    }

    private int fillDisplaySide(int dispLength, ComponentSide compSide) {
        int l = Math.max(dispLength - compSide.start_inset - compSide.end_inset, 0);
        if (l < compSide.minLength) {
            l = Math.min(compSide.minLength, dispLength);
        } else if (!compSide.stretch) {
            l = Math.min(l, compSide.prefLength);
        }
        return l;
    }

    private int anchorComponentSide(int dispStart, int dispLength, ComponentSide compSide,
            int compLength) {
        //        if (compLength == 0) {
        //            return 0;
        //        }
        int insDispLength = dispLength - compSide.start_inset - compSide.end_inset;
        if (compLength <= insDispLength) {
            int s = dispStart + compSide.start_inset;
            switch (compSide.position) {
                case ComponentSide.POS_START:
                    break;
                case ComponentSide.POS_CENTER:
                    s += (insDispLength - compLength) / 2;
                    break;
                case ComponentSide.POS_END:
                    s += insDispLength - compLength;
                    break;
            }
            return s;
        }
        float insetFactor = (float) (dispLength - compLength)
                / (float) (compSide.start_inset + compSide.end_inset);
        return (dispStart + (int) (compSide.start_inset * insetFactor));
    }

    private void initHorCompSide(ComponentSide side, GridBagConstraints cons,
            Dimension minSize, Dimension prefSize, ParentInfo info) {
        MixedConstraints mixCons = info.consTable.get(cons);
        side.gridStart = mixCons.mapped.getX();
        side.gridLength = mixCons.mapped.getSize().getWidth();
        side.weight = cons.weightx;
        side.start_inset = cons.insets.left;
        side.end_inset = cons.insets.right;
        int anchor = translateRelativeAnchor(cons.anchor, !info.orientation);
        switch (anchor) {
            case GridBagConstraints.NORTHWEST:
            case GridBagConstraints.WEST:
            case GridBagConstraints.SOUTHWEST:
                side.position = ComponentSide.POS_START;
                break;
            case GridBagConstraints.NORTH:
            case GridBagConstraints.CENTER:
            case GridBagConstraints.SOUTH:
                side.position = ComponentSide.POS_CENTER;
                break;
            default:
                side.position = ComponentSide.POS_END;
        }
        if ((cons.fill == GridBagConstraints.BOTH)
                || (cons.fill == GridBagConstraints.HORIZONTAL)) {
            side.stretch = true;
        } else {
            side.stretch = false;
        }
        side.minLength = minSize.getWidth() + cons.ipadx;
        side.prefLength = prefSize.getWidth() + cons.ipadx;
    }

    private void initVertCompSide(ComponentSide side, GridBagConstraints cons,
            Dimension minSize, Dimension prefSize, ParentInfo info) {
        MixedConstraints mixCons = info.consTable.get(cons);
        side.gridStart = mixCons.mapped.getY();
        side.gridLength = mixCons.mapped.getSize().getHeight();
        side.weight = cons.weighty;
        side.start_inset = cons.insets.top;
        side.end_inset = cons.insets.bottom;
        int anchor = translateRelativeAnchor(cons.anchor, !info.orientation);
        switch (anchor) {
            case GridBagConstraints.NORTHWEST:
            case GridBagConstraints.NORTH:
            case GridBagConstraints.NORTHEAST:
                side.position = ComponentSide.POS_START;
                break;
            case GridBagConstraints.WEST:
            case GridBagConstraints.CENTER:
            case GridBagConstraints.EAST:
                side.position = ComponentSide.POS_CENTER;
                break;
            default:
                side.position = ComponentSide.POS_END;
        }
        if ((cons.fill == GridBagConstraints.BOTH)
                || (cons.fill == GridBagConstraints.VERTICAL)) {
            side.stretch = true;
        } else {
            side.stretch = false;
        }
        side.minLength = minSize.getHeight() + cons.ipady;
        side.prefLength = prefSize.getHeight() + cons.ipady;
    }

    private int translateRelativeAnchor(int relAnchor, boolean l2r) {
        int absAnchor = relAnchor;
        switch (relAnchor) {
            case GridBagConstraints.PAGE_START:
                absAnchor = GridBagConstraints.NORTH;
                break;
            case GridBagConstraints.PAGE_END:
                absAnchor = GridBagConstraints.SOUTH;
                break;
            case GridBagConstraints.LINE_START:
                absAnchor = l2r ? GridBagConstraints.WEST : GridBagConstraints.EAST;
                break;
            case GridBagConstraints.LINE_END:
                absAnchor = l2r ? GridBagConstraints.EAST : GridBagConstraints.WEST;
                break;
            case GridBagConstraints.FIRST_LINE_START:
                absAnchor = l2r ? GridBagConstraints.NORTHWEST : GridBagConstraints.NORTHEAST;
                break;
            case GridBagConstraints.FIRST_LINE_END:
                absAnchor = l2r ? GridBagConstraints.NORTHEAST : GridBagConstraints.NORTHWEST;
                break;
            case GridBagConstraints.LAST_LINE_START:
                absAnchor = l2r ? GridBagConstraints.SOUTHWEST : GridBagConstraints.SOUTHEAST;
                break;
            case GridBagConstraints.LAST_LINE_END:
                absAnchor = l2r ? GridBagConstraints.SOUTHEAST : GridBagConstraints.SOUTHWEST;
                break;
        }
        return absAnchor;
    }

    private void validate(Container parent, ParentInfo info) {
        if (info.valid) {
            return;
        }
        info.valid = true;
        resetCache(parent, info);
        info.orientation = parent.isRTL();
        Dimension maxSize = initCompsArray(parent, info.components);
        new RelativeTranslator(maxSize.getWidth(), maxSize.getHeight()).translate(info);
        initCompSides(info);
        info.grid.validate(info);
        if (layoutInfo == null) {
            layoutInfo = new GridBagLayoutInfo(info.grid.lookupWidths(), info.grid
                    .lookupHeights());
        } else {
            layoutInfo.update(info.grid.lookupWidths(), info.grid.lookupHeights());
        }
    }

    private void initCompSides(ParentInfo info) {
        for (int i = 0; i < info.components.length; i++) {
            Component comp = info.components[i];
            info.horCompSides[i] = new ComponentSide();
            initHorCompSide(info.horCompSides[i], comptable.get(comp), comp.getPreferredSize(), //comp.getMinimumSize(),
                    comp.getPreferredSize(), info);
            info.vertCompSides[i] = new ComponentSide();
            initVertCompSide(info.vertCompSides[i], comptable.get(comp), comp.getPreferredSize(), //comp.getMinimumSize(),
                    comp.getPreferredSize(), info);
        }
    }

    private Dimension initCompsArray(Container parent, Component[] components) {
        int maxW = 0;
        int maxH = 0;
        int i = 0;
        for (Component comp : comptable.keySet()) {
            GridBagConstraints cons = comptable.get(comp);
            if ((comp.getParent() == parent) && comp.isVisible()) {
                components[i++] = comp;
            }
            if ((cons.gridx != GridBagConstraints.RELATIVE)
                    && (cons.gridy != GridBagConstraints.RELATIVE)) {
                maxW = Math.max(maxW, cons.gridx + cons.gridwidth);
                maxH = Math.max(maxH, cons.gridy + cons.gridheight);
            }
        }
        return new Dimension(maxW, maxH);
    }

    private int getComponentsNumber(Container parent) {
        int componentsNumber = 0;
        for (Component comp : comptable.keySet()) {
            if ((comp.getParent() == parent) && comp.isVisible()) {
                componentsNumber++;
            }
        }
        return componentsNumber;
    }

    private void resetCache(Container parent, ParentInfo info) {
        int componentsNumber = getComponentsNumber(parent);
        info.components = new Component[componentsNumber];
        info.horCompSides = new ComponentSide[componentsNumber];
        info.vertCompSides = new ComponentSide[componentsNumber];
        updateParentInfo(parent);
    }

    private void updateParentInfo(Container parent, GridBagConstraints gbc) {
        if (parent == null) {
            return;
        }
        ParentInfo info = getParentInfo(parent);
        if (!info.consTable.containsKey(gbc)) {
            MixedConstraints mixCons = new MixedConstraints(gbc);
            info.consTable.put(gbc, mixCons);
            info.allConstraints.add(mixCons);
        }
    }

    private void updateParentInfo(Container parent) {
        int count = parent.getComponentCount();
        for (int iter = 0 ; iter < count ; iter++) {
            Component element = parent.getComponentAt(iter);
            GridBagConstraints gbc = comptable.get(element);
            updateParentInfo(parent, gbc);
        }
    }

    private class RelativeTranslator {
        private int curY; //Left-to-right (or vice versa)

        private final int curX[]; // up-to-down

        private int maxW; //Common for relative

        private int maxH; // and absolute components

        private boolean relWComp;

        private boolean relHComp;

        private int relEndY;

        private int relEndX;

        public RelativeTranslator(int maxW, int maxH) {
            this.maxW = maxW;
            this.maxH = maxH;
            curY = 0;
            curX = new int[MAXGRIDSIZE]; //All = 0, hope so
            relWComp = false;
            relHComp = false;
            relEndY = 0;
            relEndX = 0;
        }

        public void translate(ParentInfo info) {
            spreadComponents(info.allConstraints);
            recalculateRemainders(info.allConstraints);
            applyOrientation(info);
        }

        private void spreadComponents(ArrayList<MixedConstraints> allConstraints) {
            for (int i = 0; i < allConstraints.size(); i++) {
                MixedConstraints mixCons = allConstraints.get(i);
                // awt.8D=
                //assert !((relWComp && (mixCons.initial.getWidth() != GridBagConstraints.REMAINDER)) || (relHComp && (mixCons.initial.getSize().getHeight() != GridBagConstraints.REMAINDER))) : "REMINDER component expected after RELATIVE one"; //$NON-NLS-1$
                if (curY == MAXGRIDSIZE) {
                    // awt.8E=
                    throw new RuntimeException("component is out of grid's range"); //$NON-NLS-1$
                }
                translateHor(mixCons, translateVert(mixCons, i, allConstraints));
            }
            // awt.8D=
            //assert !(relWComp || relHComp) : "REMINDER component expected after RELATIVE one"; //$NON-NLS-1$
        }

        private void applyOrientation(ParentInfo info) {
            if (info.orientation) {
                for (int i = 0; i < info.allConstraints.size(); i++) {
                    MixedConstraints mixCons = info.allConstraints.get(i);
                    if (mixCons.relative) {
                        mixCons.mapped.setX(maxW - mixCons.mapped.getX() - mixCons.mapped.getWidth());
                    }
                }
            }
        }

        private int translateVert(MixedConstraints mixCons, int i,
                ArrayList<MixedConstraints> allConstraints) {
            int endY;
            if (mixCons.initial.getY() != GridBagConstraints.RELATIVE) {
                curY = mixCons.initial.getY();
            }
            mixCons.mapped.setY(curY);
            mixCons.mapped.setHeight(Math.max(mixCons.initial.getHeight(), 1));
            if (mixCons.initial.getHeight() == GridBagConstraints.REMAINDER) {
                if (relHComp) {
                    mixCons.mapped.setY(allConstraints.get(i - 1).mapped.getY() + 1);
                    relHComp = false;
                }
                endY = MAXGRIDSIZE;
            } else if (mixCons.initial.getHeight() == GridBagConstraints.RELATIVE) {
                relHComp = true;
                if (mixCons.initial.getWidth() != GridBagConstraints.REMAINDER) {
                    relEndX = curX[curY] + mixCons.initial.getWidth();
                } else {
                    relEndX = MAXGRIDSIZE;
                }
                endY = mixCons.mapped.getY() + 1;
            } else {
                endY = mixCons.mapped.getY() + mixCons.mapped.getHeight();
            }
            if (endY > MAXGRIDSIZE) {
                // awt.8E=
                throw new RuntimeException("component is out of grid's range"); //$NON-NLS-1$
            }
            maxH = Math.max(maxH, mixCons.mapped.getY() + mixCons.mapped.getHeight());
            return endY;
        }

        private void translateHor(MixedConstraints mixCons, int endY) {
            int trueCurY = curY;
            if (mixCons.initial.getX() != GridBagConstraints.RELATIVE) {
                for (;; trueCurY++) {
                    if (trueCurY == MAXGRIDSIZE) {
                        // awt.8E=
                        throw new RuntimeException("component is out of grid's range"); //$NON-NLS-1$
                    }
                    if (curX[trueCurY] <= mixCons.initial.getX()) {
                        break;
                    }
                }
                mixCons.mapped.setY(trueCurY);
                mixCons.mapped.setX(mixCons.initial.getX());
                endY += trueCurY - curY;
            } else {
                mixCons.mapped.setX(curX[trueCurY]);
            }
            mixCons.mapped.setWidth(Math.max(mixCons.initial.getWidth(), 1));
            if (mixCons.initial.getWidth() == GridBagConstraints.REMAINDER) {
                if (relWComp) {
                    endY = Math.max(endY, relEndY);
                    relWComp = false;
                    relEndY = 0;
                }
                curY = endY;
            } else if (mixCons.initial.getWidth() == GridBagConstraints.RELATIVE) {
                relWComp = true;
                relEndY = endY;
                curX[curY]++;
            } else {
                if (!relHComp) {
                    int endX = Math.max(curX[trueCurY] + mixCons.mapped.getWidth(), relEndX);
                    for (int j = trueCurY; j < endY; j++) {
                        curX[j] = endX;
                    }
                    relEndX = 0;
                }
            }
            if ((mixCons.mapped.getX() + mixCons.mapped.getWidth()) > MAXGRIDSIZE) {
                // awt.8E=
                throw new RuntimeException("component is out of grid's range"); //$NON-NLS-1$
            }
            maxW = Math.max(maxW, mixCons.mapped.getX() + mixCons.mapped.getWidth());
            //            if (curYBackup >= 0) { //FIX
            //                curY = curYBackup;
            //                curYBackup = -1;
            //            }
        }

        private void recalculateRemainders(ArrayList<MixedConstraints> allConstraints) {
            for (int i = 0; i < allConstraints.size(); i++) {
                MixedConstraints mixCons = allConstraints.get(i);
                if (mixCons.initial.getWidth() == GridBagConstraints.REMAINDER) {
                    mixCons.mapped.setWidth(maxW - mixCons.mapped.getX());
                } else if (mixCons.initial.getWidth() == GridBagConstraints.RELATIVE) {
                    int reserve = maxW - mixCons.mapped.getX() - 2;
                    if (reserve > 0) {
                        mixCons.mapped.setWidth(mixCons.mapped.getWidth() + reserve);
                        if ((i + 1) < allConstraints.size()) {
                            Rectangle r = allConstraints.get(i + 1).mapped;
                            r.setX(r.getX() + reserve);
                        }
                    }
                }
                if (mixCons.initial.getHeight() == GridBagConstraints.REMAINDER) {
                    mixCons.mapped.setHeight(maxH - mixCons.mapped.getY());
                } else if (mixCons.initial.getHeight() == GridBagConstraints.RELATIVE) {
                    int reserve = maxH - mixCons.mapped.getX() - 2;
                    mixCons.mapped.setHeight(mixCons.mapped.getHeight() + reserve);
                    if ((i + 1) < allConstraints.size()) {
                        Rectangle r = allConstraints.get(i + 1).mapped;
                        r.setY(r.getY() + reserve);
                    }
                }
            }
        }
    }

    private class ComponentSide {
        public static final int POS_START = 1;

        public static final int POS_CENTER = 2;

        public static final int POS_END = 3;

        public int gridStart;

        public int gridLength;

        public int start_inset;

        public int end_inset;

        public int position;

        public int minLength;

        public int prefLength;

        public double weight;

        public boolean stretch;
    }

    private class Grid {
        private final GridSide cols = new GridSide();

        private final GridSide rows = new GridSide();

        public void validate(ParentInfo info) {
            cols.validate(info.horCompSides, columnWidths, columnWeights);
            rows.validate(info.vertCompSides, rowHeights, rowWeights);
        }

        public Dimension minimumSize() {
            return new Dimension(cols.getMinLength(), rows.getMinLength());
        }

        public Dimension preferredSize() {
            return new Dimension(cols.getPrefLength(), rows.getPrefLength());
        }

        public Rectangle componentDisplayArea(ComponentSide horSide, ComponentSide vertSide) {
            Segment hor = cols.componentDisplaySide(horSide.gridStart, horSide.gridLength);
            Segment vert = rows.componentDisplaySide(vertSide.gridStart, vertSide.gridLength);
            return new Rectangle(hor.start, vert.start, hor.length, vert.length);
        }

        public void fit2Client(Rectangle clientRect) {
            Segment horSeg = new Segment(clientRect.getX(), clientRect.getWidth());
            cols.fit2Client(horSeg);
            clientRect.setX(horSeg.start);
            clientRect.setWidth(horSeg.length);
            Segment vertSeg = new Segment(clientRect.getY(), clientRect.getHeight());
            rows.fit2Client(vertSeg);
            clientRect.setY(vertSeg.start);
            clientRect.setHeight(vertSeg.length);
        }

        public int[] getWidths() {
            return cols.getLengths();
        }

        public int[] getHeights() {
            return rows.getLengths();
        }

        public int[] lookupWidths() {
            return cols.lookupLengths();
        }

        public int[] lookupHeights() {
            return rows.lookupLengths();
        }

        public int[] lookupMinWidths() {
            return cols.lookupMinLengths();
        }

        public int[] lookupMinHeights() {
            return rows.lookupMinLengths();
        }

        public int[] lookupPrefWidths() {
            return cols.lookupPrefLengths();
        }

        public int[] lookupPrefHeights() {
            return rows.lookupPrefLengths();
        }

        public double[][] getWeights() {
            return new double[][] { cols.getWeights(), rows.getWeights() };
        }

        /*public Point getOrigin() {
            return new Point(cols.getOrigin(), rows.getOrigin());
        }

        public Point location(int x, int y, boolean l2r) {
            int col = cols.location(x);
            int row = Math.max(Math.min(rows.location(y), MAXGRIDSIZE - 1), 0);
            if (col == MAXGRIDSIZE) {
                col = l2r ? MAXGRIDSIZE - 1 : 0;
            } else if (col == -1) {
                col = l2r ? 0 : MAXGRIDSIZE - 1;
            }
            return new Point(col, row);
        }*/

        private class GridSide {
            private final int coordinates[] = new int[MAXGRIDSIZE];

            private final int lengths[] = new int[MAXGRIDSIZE];

            /*Cashed data. Validation controlled by parent class*/
            private final int minLengths[] = new int[MAXGRIDSIZE];

            private int minLength = 0;

            private final int prefLengths[] = new int[MAXGRIDSIZE];

            private int prefLength = 0;

            private final double weights[] = new double[MAXGRIDSIZE];

            private double weight = 0.;

            private int weightlessPrefLength = 0;

            private int weightlessMinLength = 0;

            private int weightyPartsNum = 0;

            public void validate(ComponentSide compSides[], int lengthsOverride[],
                    double weightsOverride[]) {
                resetCache();
                spreadComponents(compSides);
                applyOverrides(lengthsOverride, weightsOverride);
                calculateIntegrals();
            }

            public int getMinLength() {
                return minLength;
            }

            public int getPrefLength() {
                return prefLength;
            }

            public Segment componentDisplaySide(int startPart, int partsNum) {
                int l = 0;
                for (int part = startPart; part < (startPart + partsNum); part++) {
                    l += lengths[part];
                }
                return new Segment(coordinates[startPart], l);
            }

            public void fit2Client(Segment clientSide) {
                int start = clientSide.start;
                if (clientSide.length > weightlessPrefLength) {
                    if (weight > 0.) {
                        if (clientSide.length >= prefLength) {
                            divideExtraWeightyLength(clientSide);
                        } else {
                            //divideExtraLength(clientSide);
                            divideInsufWeightyLength(clientSide);
                        }
                    } else {
                        start = centerSide(clientSide);
                    }
                } else if (weightlessMinLength > clientSide.length) {
                    divideInsufficientLength(clientSide);
                } else {
                    //                    divideSufficientLength(clientSide);
                    divideInsufWeightyLength(clientSide);
                }
                calculateCoordinates(start);
            }

            public int[] getLengths() {
                int res[] = new int[MAXGRIDSIZE];
                for (int i = 0; i < MAXGRIDSIZE; i++) {
                    res[i] = lengths[i];
                }
                return res;
            }

            public int[] lookupLengths() {
                return lengths;
            }

            public int[] lookupMinLengths() {
                return minLengths;
            }

            public int[] lookupPrefLengths() {
                return prefLengths;
            }

            public double[] getWeights() {
                double res[] = new double[MAXGRIDSIZE];
                for (int i = 0; i < MAXGRIDSIZE; i++) {
                    res[i] = weights[i];
                }
                return res;
            }

            public int getOrigin() {
                return coordinates[0];
            }

            public int location(int p) {
                if (p < coordinates[0]) {
                    return -1;
                } else if (p >= (coordinates[MAXGRIDSIZE - 1] + lengths[MAXGRIDSIZE - 1])) {
                    return MAXGRIDSIZE;
                }
                int i = 0;
                while (!((coordinates[i] <= p) && ((coordinates[i] + lengths[i]) > p))) {
                    i++;
                }
                return i;
            }

            private void calculateIntegrals() {
                for (int i = 0; i < MAXGRIDSIZE; i++) {
                    prefLength += prefLengths[i];
                    minLength += minLengths[i];
                    weight += weights[i];
                    if (weights[i] == 0.) {
                        weightlessPrefLength += prefLengths[i];
                        weightlessMinLength += minLengths[i];
                        weightyPartsNum++;
                    }
                }
            }

            private void applyOverrides(int lengthsOverride[], double weightsOverride[]) {
                if (weightsOverride != null) {
                    if (weightsOverride.length > MAXGRIDSIZE) {
                        // awt.8F=Weights' overrides array is too long
                        throw new RuntimeException("Weights' overrides array is too long"); //$NON-NLS-1$
                    }
                    for (int i = 0; i < weightsOverride.length; i++) {
                        weights[i] = Math.max(weights[i], weightsOverride[i]);
                    }
                }
                if (lengthsOverride != null) {
                    if (lengthsOverride.length > MAXGRIDSIZE) {
                        // awt.90=
                        throw new RuntimeException("Lengths' overrides array is too long"); //$NON-NLS-1$
                    }
                    for (int i = 0; i < lengthsOverride.length; i++) {
                        minLengths[i] = lengthsOverride[i];
                        prefLengths[i] = Math.max(prefLengths[i], lengthsOverride[i]);
                    }
                }
            }

            private void spreadComponents(ComponentSide compSides[]) {
                for (ComponentSide element : compSides) {
                    if (element.gridLength == 1) {
                        int insets = element.start_inset + element.end_inset;
                        spreadUnicellularComponent(element.gridStart, element.minLength
                                + insets, element.prefLength + insets, element.weight);
                    }
                }
                for (ComponentSide element : compSides) {
                    if (element.gridLength > 1) {
                        int insets = element.start_inset + element.end_inset;
                        spreadMulticellularComponent(element.gridStart, element.gridLength,
                                element.minLength + insets, element.prefLength + insets,
                                element.weight);
                    }
                }
            }

            private void spreadUnicellularComponent(int part, int minCompLength,
                    int prefCompLength, double compWeight) {
                minLengths[part] = Math.max(minLengths[part], minCompLength);
                prefLengths[part] = Math.max(prefLengths[part], prefCompLength);
                weights[part] = Math.max(weights[part], compWeight);
            }

            private void spreadMulticellularComponent(int startPart, int partsNum,
                    int minCompLength, int prefCompLength, double compWeight) {
                double sumWeight = spreadComponentWeight(weights, startPart, partsNum,
                        compWeight);
                spreadComponentLength(minLengths, startPart, partsNum, minCompLength, sumWeight);
                spreadComponentLength(prefLengths, startPart, partsNum, prefCompLength,
                        sumWeight);
            }

            private void resetCache() {
                Arrays.fill(minLengths, 0);
                minLength = 0;
                Arrays.fill(prefLengths, 0);
                prefLength = 0;
                Arrays.fill(weights, 0.);
                weight = 0.;
                weightlessPrefLength = 0;
                weightlessMinLength = 0;
                weightyPartsNum = 0;
            }

            private void spreadComponentLength(int arr[], int startPart, int partsNum,
                    int compLength, double sumWeight) {
                int rest = compLength;
                int lastPart = startPart + partsNum - 1;
                for (int part = startPart; part < lastPart; part++) {
                    rest -= arr[part];
                }
                if (sumWeight != 0.0) {
                    rest -= arr[lastPart];
                    // divide extra length using weights
                    int sharedExtraL = 0;
                    double accumWeight = 0.0;
                    for (int part = startPart; part <= lastPart; part++) {
                        accumWeight += weights[part];
                        int curExtraL = (int) (rest * (accumWeight / sumWeight)) - sharedExtraL;
                        arr[part] = Math.max(arr[part], arr[part] + curExtraL);
                        sharedExtraL += curExtraL;
                    }
                } else {
                    // just put all extra
                    // length into the last part
                    arr[lastPart] = Math.max(arr[lastPart], rest);
                }
            }

            private double spreadComponentWeight(double arr[], int startPart, int partsNum,
                    double compWeight) {
                int lastPart = startPart + partsNum - 1;
                double sumWeight = .0;
                for (int part = startPart; part <= lastPart; part++) {
                    sumWeight += arr[part];
                }
                if ((compWeight > sumWeight) && (sumWeight > 0)) {
                    for (int part = startPart; part < (startPart + partsNum); part++) {
                        arr[part] = compWeight * arr[part] / sumWeight;
                    }
                } else if (sumWeight == 0) {
                    arr[lastPart] = compWeight;
                }
                return sumWeight;
            }

            private void divideExtraWeightyLength(Segment clientSide) {
                int extraL = clientSide.length - prefLength;
                int sharedExtraL = 0;
                double accumWeight = 0.;
                for (int i = 0; i < MAXGRIDSIZE; i++) {
                    if (weights[i] > 0.) {
                        accumWeight += weights[i];
                        int curExtraL = (int) (extraL * (accumWeight / weight)) - sharedExtraL;
                        lengths[i] = prefLengths[i] + curExtraL;
                        sharedExtraL += curExtraL;
                    } else {
                        lengths[i] = prefLengths[i];
                    }
                }
            }

            private void divideInsufWeightyLength(Segment clientSide) {
                int extraL = clientSide.length - minLength;
                int sharedExtraL = 0;
                double accumWeight = 0.;
                for (int i = 0; i < MAXGRIDSIZE; i++) {
                    if (weights[i] > 0.) {
                        accumWeight += weights[i];
                        int curExtraL = (int) (extraL * (accumWeight / weight)) - sharedExtraL;
                        lengths[i] = minLengths[i] + curExtraL;
                        sharedExtraL += curExtraL;
                    } else {
                        lengths[i] = minLengths[i];
                    }
                }
            }

            private int centerSide(Segment clientSide) {
                for (int i = 0; i < MAXGRIDSIZE; i++) {
                    lengths[i] = prefLengths[i];
                }
                return (clientSide.start + (clientSide.length - prefLength) / 2);
            }

            private void divideInsufficientLength(Segment clientSide) {
                int sharedL = (weightlessMinLength - clientSide.length) / 2;
                if (sharedL < 0) {
                    sharedL = 0;
                }
                for (int i = 0; i < MAXGRIDSIZE; i++) {
                    if (weights[i] > 0.) {
                        lengths[i] = 0;
                    } else {
                        int minL = minLengths[i];
                        if (sharedL >= minL) {
                            sharedL -= minL;
                            lengths[i] = 0;
                        } else {
                            lengths[i] = minL - sharedL;
                            sharedL = 0;
                        }
                    }
                }
            }

            private void calculateCoordinates(int start) {
                coordinates[0] = start;
                for (int i = 1; i < MAXGRIDSIZE; i++) {
                    coordinates[i] = coordinates[i - 1] + lengths[i - 1];
                }
            }
        }
    }

    private class Segment {
        public int start;

        public int length;

        Segment(int start, int length) {
            this.start = start;
            this.length = length;
        }
    }

    private class MixedConstraints {
        public Rectangle initial; //Relative/Absolute

        public Rectangle mapped; //Absolute

        public boolean relative;

        MixedConstraints(GridBagConstraints cons) {
            initial = new Rectangle(cons.gridx, cons.gridy, cons.gridwidth, cons.gridheight);
            mapped = new Rectangle();
            relative = (cons.gridx == GridBagConstraints.RELATIVE)
                    || (cons.gridy == GridBagConstraints.RELATIVE);
        }
    }

    private class ParentInfo {
        final HashMap<GridBagConstraints, MixedConstraints> consTable; // Components' constraints to relative constraints

        final ArrayList<MixedConstraints> allConstraints; // Only mapped rectangle is a part of cache

        final Grid grid;

        boolean valid;

        ComponentSide horCompSides[];

        ComponentSide vertCompSides[];

        Component components[]; // Hashtable is too slow

        
        // true for RTL
        boolean orientation;

        ParentInfo() {
            valid = false;
            consTable = new HashMap<GridBagConstraints, MixedConstraints>();
            allConstraints = new ArrayList<MixedConstraints>();
            grid = new Grid();
            horCompSides = vertCompSides = null;
            components = null;
        }
    }
}

class GridBagLayoutInfo {

    int widths[];
    int heights[];

    GridBagLayoutInfo(int widths[], int heights[]) {
        this.widths = widths;
        this.heights = heights;
    }

    void update(int widths[], int heights[]) {
        this.widths = widths;
        this.heights = heights;
    }

}
