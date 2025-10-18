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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector.ComponentClosure;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.LayeredLayout.LayeredLayoutConstraint;
import com.codename1.ui.layouts.LayeredLayout.LayeredLayoutConstraint.Inset;
import com.codename1.ui.plaf.Border;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.codename1.ui.ComponentSelector.$;

/**
 * A Split Pane component.
 * <p>A split pane can either be horizontal or vertical, and provides a draggable divider
 * between two components.  If the {@link #orientation} is {@link #HORIZONTAL_SPLIT}, then the
 * child components will be laid out horizontally (side by side with a vertical bar as a divider).
 * If the {@link #orientation} is {@link #VERTICAL_SPLIT}, then the components are laid out vertically (one above
 * the other.</p>
 *
 * <p>The bar divider bar includes arrows to collapse and expand the divider also.</p>
 *
 * <p><strong>Splitpane UI</strong></p>
 * <p>The following is an example of a UI that is built around a split pane.  This has an outer "horizontal" split pane,
 * and the left side has a vertical split pane.</p>
 * <p><img src="https://raw.githubusercontent.com/wiki/codenameone/CodenameOne/img/developer-guide/splitpane-1.png"/></p>
 * <p>Collapsed:</p>
 * <p><img src="https://raw.githubusercontent.com/wiki/codenameone/CodenameOne/img/developer-guide/splitpane-collapsed.png"/></p>
 * <p>Expanded:</p>
 * <p><img src="https://raw.githubusercontent.com/wiki/codenameone/CodenameOne/img/developer-guide/splitpane-expanded.png"/></p>
 *
 * @author Steve Hannah
 */
public class SplitPane extends Container {

    /**
     * Constant used for orientation.
     */
    public static final int HORIZONTAL_SPLIT = 0;
    /**
     * Constant used for orientation.
     */
    public static final int VERTICAL_SPLIT = 1;
    /**
     * The orientation.  One of {@link #HORIZONTAL_SPLIT} or {@link #VERTICAL_SPLIT}
     */
    private final int orientation;
    /**
     * Container for the top or left component.
     */
    private final Container topOrLeft;
    /**
     * Container for the bottom or right component.
     */
    private final Container bottomOrRight;
    /**
     * The draggable divider.
     */
    private final Divider divider;
    /**
     * UIID to use for the expand button
     */
    private String expandButtonUIID = "Label";

    /**
     * UIID to use for the collapse button
     */
    private String collapseButtonUIID = "Label";

    /**
     * UIID to use for the drag handle on the divider
     */
    private String dragHandleUIID = "Label";

    /**
     * Material icon for expand button.
     */
    private char expandMaterialIcon,

    /**
     * Material icon for collapse button.
     */
    collapseMaterialIcon,

    /**
     * Material icon for drag handle.
     */
    dragHandleMaterialIcon;

    /**
     * Icon for expand button.
     */
    private Image expandIcon,
    /**
     * Icon for collapse button.
     */
    collapseIcon,
    /**
     * Icon or drag handle
     */
    dragHandleIcon;

    /**
     * The UIID for the divider.  Default is null so that we can generate the style and border
     * manually.
     */
    private String dividerUIID = null;

    /**
     * The preferred divider thickness in millimetres
     */
    private float dividerThicknessMM = 3;

    /**
     * Whether to show the expand/collapse buttons.
     */
    private boolean showExpandCollapseButtons = true;

    /**
     * Whether to show the drag handle.
     */
    private boolean showDragHandle = true;
    /**
     * The minimum allowable inset for the divider.
     */
    private final LayeredLayoutConstraint minInset;
    /**
     * The maximum allowable inset for the divider.
     */
    private final LayeredLayoutConstraint maxInset;
    /**
     * The starting preferred inset for the divider.  This will be changed over the life of the
     * split pane.  Any time the user explicitly drags the divider to a new location, that location
     * will become the new preferred inset.
     */
    private final LayeredLayoutConstraint preferredInset;
    /**
     * Flag to indicate that the split pane is expanded.
     */
    private boolean isExpanded;
    /**
     * Flag to indicate that the split pane is collapsed.
     */
    private boolean isCollapsed;

    /**
     * Creates a new SplitPane.
     *
     * @param settings      The settings for the split pane (e.g. insets, styles, etc...).
     * @param topOrLeft     The component to place in the "top" (for vertical), or "left" (for horizontal).
     * @param bottomOrRight The component to place in the "bottom" (for vertical) or "right" (for horizontal).
     */
    public SplitPane(Settings settings, Component topOrLeft, Component bottomOrRight) {
        super(new LayeredLayout());
        int orientation = settings.orientation;
        String minInset = settings.minInset;
        String preferredInset = settings.preferredInset;
        String maxInset = settings.maxInset;

        this.orientation = orientation;
        this.topOrLeft = BorderLayout.center(topOrLeft);
        this.bottomOrRight = BorderLayout.center(bottomOrRight);
        this.expandButtonUIID = settings.expandButtonUIID;
        this.collapseButtonUIID = settings.collapseButtonUIID;
        this.dragHandleUIID = settings.dragHandleUIID;
        this.dividerThicknessMM = settings.dividerThicknessMM;
        this.showDragHandle = settings.showDragHandle;
        this.showExpandCollapseButtons = settings.showExpandCollapseButtons;
        this.dividerUIID = settings.dividerUIID;

        divider = new Divider();
        add(this.topOrLeft).add(this.bottomOrRight).add(divider);

        LayeredLayout l = (LayeredLayout) getLayout();
        this.preferredInset = initDividerInset(l.createConstraint(), preferredInset);
        this.minInset = initDividerInset(l.createConstraint(), minInset);
        this.maxInset = initDividerInset(l.createConstraint(), maxInset);

        l.setInsets(this.topOrLeft, "0 0 0 0")
                .setInsets(this.topOrLeft, "0 0 0 0");
        this.preferredInset.copyTo(l.getOrCreateConstraint(divider));

        switch (orientation) {
            case HORIZONTAL_SPLIT: {
                l.setReferenceComponentRight(this.topOrLeft, divider, 1f);
                l.setReferenceComponentLeft(this.bottomOrRight, divider, 1f);
                break;
            }

            default: {
                l.setReferenceComponentBottom(this.topOrLeft, divider, 1f);
                l.setReferenceComponentTop(this.bottomOrRight, divider, 1f);
                break;
            }

        }


    }

    /**
     * Creates a new SplitPane.
     *
     * @param orientation    Either {@link #HORIZONTAL_SPLIT} or {@link #VERTICAL_SPLIT}
     * @param topOrLeft      The component to place in the "top" (for vertical), or "left" (for horizontal).
     * @param bottomOrRight  The component to place in the "bottom" (for vertical) or "right" (for horizontal).
     * @param minInset       The minimum allowable inset for the divider.  The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "200px".
     * @param preferredInset The default preferred inset for the divider.  The inset should be expressed as a string with both value and unit.  E.g. "75%", "50mm", "20px".
     * @param maxInset       The maximum allowable inset for the divider.  The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "20px".
     */
    public SplitPane(int orientation, Component topOrLeft, Component bottomOrRight, String minInset, String preferredInset, String maxInset) {
        this(new Settings(orientation, minInset, preferredInset, maxInset), topOrLeft, bottomOrRight);
    }

    /**
     * Changes the minimum, preferred, and maximum insets of the split pane.  This will also
     * update the current divider position to the supplied preferred inset.
     *
     * @param minInset       The minimum inset.  Can be expressed in pixels (px), millimetres (mm), or percent (%).  E.g. "25%"
     * @param preferredInset The preferred inset. Can be expressed in pixels (px), millimetres (mm), or percent (%).  E.g. "25%"
     * @param maxInset       Can be expressed in pixels (px), millimetres (mm), or percent (%).  E.g. "25%"
     */
    public void changeInsets(String minInset, String preferredInset, String maxInset) {
        LayeredLayout l = (LayeredLayout) getLayout();
        initDividerInset(l.createConstraint(), preferredInset).copyTo(this.preferredInset);
        initDividerInset(l.createConstraint(), minInset).copyTo(this.minInset);
        initDividerInset(l.createConstraint(), maxInset).copyTo(this.maxInset);

        l.setInsets(this.topOrLeft, "0 0 0 0")
                .setInsets(this.topOrLeft, "0 0 0 0");
        this.preferredInset.copyTo(l.getOrCreateConstraint(divider));
    }

    /**
     * The active inset of the divider.
     *
     * @return
     */
    private Inset getDividerInset() {
        LayeredLayoutConstraint cnst = ((LayeredLayout) getLayout()).getOrCreateConstraint(divider);
        return getFixedInset(cnst);
    }

    /**
     * Gets the inset of the divider that is flexible.
     *
     * @return
     */
    private Inset getAutoInset() {
        LayeredLayoutConstraint cnst = ((LayeredLayout) getLayout()).getOrCreateConstraint(divider);
        return getAutoInset(cnst);
    }

    /**
     * Gets the inset of the provided constraint that is fixed.
     *
     * @param cnst
     * @return
     */
    private Inset getFixedInset(LayeredLayoutConstraint cnst) {
        switch (orientation) {
            case VERTICAL_SPLIT:
                return cnst.top();
            default:
                return cnst.left();
        }
    }

    private Inset getMinDividerInset() {
        return getFixedInset(minInset);
    }

    private Inset getMaxDividerInset() {
        return getFixedInset(maxInset);
    }

    private Inset getAutoInset(LayeredLayoutConstraint cnst) {
        switch (orientation) {
            case VERTICAL_SPLIT:
                return cnst.bottom();
            default:
                return cnst.right();
        }
    }

    private Set<Inset> getZeroInsets(LayeredLayoutConstraint cnst) {
        Set<Inset> out = new HashSet<Inset>();
        switch (orientation) {
            case VERTICAL_SPLIT:
                out.add(cnst.left());
                out.add(cnst.right());
                break;
            default:
                out.add(cnst.top());
                out.add(cnst.bottom());

        }
        return out;
    }

    private LayeredLayoutConstraint initDividerInset(LayeredLayoutConstraint cnst, String insetVal) {
        getFixedInset(cnst).setValueAsString(insetVal);
        getAutoInset(cnst).setValueAsString("auto");
        for (Inset i : getZeroInsets(cnst)) {
            i.setValueAsString("0");
        }
        return cnst;
    }

    // PMD Fix (UnusedPrivateMethod): Removed unused setDividerInset helper.
    private void clampInset() {

        int px = getDividerInset().getAbsolutePixels(divider);
        isCollapsed = false;
        isExpanded = false;
        Inset minInset = getMinDividerInset();
        if (minInset.getAbsolutePixels(divider) >= px) {
            minInset.copyTo(getDividerInset());
            isCollapsed = true;
            isExpanded = false;
        }
        Inset maxInset = getMaxDividerInset();
        if (maxInset.getAbsolutePixels(divider) <= px) {
            maxInset.copyTo(getDividerInset());
            isExpanded = true;
            isCollapsed = false;
        }

        px = getAutoInset().getAbsolutePixels(divider);

        if (px < 0) {
            // Make sure that the divider is fully visible
            getDividerInset().translatePixels(px, true, divider.getParent());
            isExpanded = true;
            isCollapsed = false;

        }
    }

    /**
     * Gets the component that is currently placed in the bottom or right of the split pane.
     *
     * @return
     */
    public Component getBottomOrRightComponent() {
        Iterator<Component> iterator = bottomOrRight.iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private void setBottomOrRightComponent(Component cmp) {
        bottomOrRight.removeAll();
        bottomOrRight.add(BorderLayout.CENTER, cmp);
    }

    /**
     * Gets the component that is currently placed in the bottom of the split pane.
     *
     * @return
     */
    public Component getBottom() {
        return getBottomOrRightComponent();
    }

    /**
     * Sets the component to be placed on the bottom of the split pane.
     *
     * @param cmp The component to place on the bottom.
     */
    public void setBottom(Component cmp) {
        setBottomOrRightComponent(cmp);
    }

    /**
     * Gets the component that is currently placed in the right of the split pane.
     *
     * @return
     */
    public Component getRight() {
        return getBottomOrRightComponent();
    }

    /**
     * Sets the component to be placed on the right of the split pane.
     *
     * @param cmp The component to place on the right.
     */
    public void setRight(Component cmp) {
        setBottomOrRightComponent(cmp);
    }

    /**
     * Gets the component that is currently placed in the top or left of the split pane.
     *
     * @return
     */
    public Component getTopOrLeftComponent() {
        Iterator<Component> iterator = topOrLeft.iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private void setTopOrLeftComponent(Component cmp) {
        topOrLeft.removeAll();
        topOrLeft.add(BorderLayout.CENTER, cmp);
    }

    /**
     * Gets the component that is currently placed in the top of the split pane.
     *
     * @return
     */
    public Component getTop() {
        return getTopOrLeftComponent();
    }

    /**
     * Sets the component that should be placed in the top section of the split pane.
     *
     * @param cmp The component to place on top.
     */
    public void setTop(Component cmp) {
        setTopOrLeftComponent(cmp);
    }

    /**
     * Gets the component that is currently placed in the left of the split pane.
     *
     * @return
     */
    public Component getLeft() {
        return getTopOrLeftComponent();
    }

    /**
     * Sets the component that should be placed in the left section of the split pane.
     *
     * @param cmp The component to place on the left.
     */
    public void setLeft(Component cmp) {
        setTopOrLeftComponent(cmp);
    }

    /**
     * Toggles the split pane between collapsed state and preferred state.  E.g. If the inset is currently
     * not collapsed, it will collapse it.  If it is collapsed, it will open to the last position that the user
     * selected.
     */
    public void toggleCollapsePreferred() {
        if (isCollapsed) {
            expand();
        } else if (isExpanded) {
            collapse(true);
        } else {
            collapse();
        }
    }

    /**
     * Toggles the split pane between expanded state and preferred state.  E.g. If the inset is currently expanded,
     * then it will be moved to the last position that the user selected.  If it is not expanded, it will expand it all the way.
     */
    public void toggleExpandPreferred() {
        if (isExpanded) {
            collapse();
        } else if (isCollapsed) {
            expand(true);
        } else {
            expand();
        }
    }

    /**
     * Expands the split pane.  If it is currently completely collapsed, it will transition to the preferred
     * position.  If it is in the preferred position, it will expand all the way.
     */
    public void expand() {
        expand(false);
    }

    /**
     * Expands the split pane. It will either expand it to the preferred position, or the maximum position
     * depending on the value of the {@literal force} parameter.
     *
     * @param force If this is true, then it will only expand "all the way".  It will skip the preferred position if it is
     *              currently in collapsed state.
     */
    public void expand(boolean force) {
        if (isCollapsed && !force) {
            getFixedInset(preferredInset).copyTo(getDividerInset());
            clampInset();
            isCollapsed = false;
            // PMD Fix (UselessQualifiedThis): Call animateLayout directly within the class scope.
            animateLayout(300);
        } else if (!isExpanded) {
            getFixedInset(maxInset).copyTo(getDividerInset());
            clampInset();
            isExpanded = true;
            // PMD Fix (UselessQualifiedThis): Call animateLayout directly within the class scope.
            animateLayout(300);
        }
    }

    /**
     * Collapses the aplit pane.  If it is currently expanded, then it will shift to the preferred posiiton.  If it is
     * already in the preferred position, it will collapse all the way to the minimum position.
     */
    public void collapse() {
        collapse(false);
    }

    /**
     * Collapses the split pane.
     *
     * @param force True to force it to collapse to minimum position (skipping preferred position if it is in expanded state).
     */
    public void collapse(boolean force) {
        if (!isCollapsed) {
            if (isExpanded && !force) {
                getFixedInset(preferredInset).copyTo(getDividerInset());
                clampInset();
                isExpanded = false;
                // PMD Fix (UselessQualifiedThis): Call animateLayout directly within the class scope.
                animateLayout(300);
            } else {
                getFixedInset(minInset).copyTo(getDividerInset());
                clampInset();
                isCollapsed = true;
                // PMD Fix (UselessQualifiedThis): Call animateLayout directly within the class scope.
                animateLayout(300);
            }
        }

    }

    /**
     * Sets the inset of the divider explicitly.  This The inset is measured from the top for
     * vertical split panes and the left for horizontal split panes.  Setting this to "50%" will
     * move the divider to the middle point.  Setting it to "0" would set it all the way to the
     * left/top.  This will clamp the value at the minimum and maximum offsets if clamp is true.
     *
     * @param inset
     * @param clamp True to clamp the inset to prevent it from running off the page.
     */
    public void setInset(String inset, boolean clamp) {
        getDividerInset().setValueAsString(inset);
        isExpanded = false;
        isCollapsed = false;
        if (clamp) {
            clampInset();
        }
    }

    /**
     * Sets the inset of the divider explicitly.  This The inset is measured from the top for
     * vertical split panes and the left for horizontal split panes.  Setting this to "50%" will
     * move the divider to the middle point.  Setting it to "0" would set it all the way to the
     * left/top.  This will clamp the value at the minimum and maximum offsets.
     *
     * @param inset
     */
    public void setInset(String inset) {
        setInset(inset, true);
    }

    /**
     * Gets the string value of the preferred inset.  E.g. "25mm", or "50%".  Note:  The preferred
     * inset is changed automatically when the user drags it to a new location so the value returned here
     * may be different than the inset supplied in the constructor.
     *
     * @return The current preferred inset of the divider.
     */
    public String getPreferredInset() {
        return getFixedInset(preferredInset).getValueAsString();
    }

    /**
     * Sets the preferred inset of this split pane.  The preferred inset will be automatically
     * changed whenever the user explicitly moves the divider to a new position.
     *
     * @param inset The inset.  E.g. "2mm", "25%", "200px".
     */
    public void setPreferredInset(String inset) {
        getFixedInset(preferredInset).setValueAsString(inset);
    }

    /**
     * Gets the string value of the minimum inset of the divider.  E.g. "25mm", or "50%".
     *
     * @return
     */
    public String getMinInset() {
        return getFixedInset(minInset).getValueAsString();
    }

    /**
     * Sets the minimum inset allowed for the divider.
     *
     * @param inset The inset.  E.g. "2mm", "10%", "200px"
     */
    public void setMinInset(String inset) {
        getFixedInset(minInset).setValueAsString(inset);
    }

    /**
     * Gets the string value of the maximum inset of the divider.  E.g. "25mm", or "50%"
     *
     * @return
     */
    public String getMaxInset() {
        return getFixedInset(maxInset).getValueAsString();
    }

    /**
     * Sets the maximum inset allowed for the divider.
     *
     * @param inset The inset.  E.g. "2mm", "10%", "200px"
     */
    public void setMaxInset(String inset) {
        getFixedInset(maxInset).setValueAsString(inset);
    }

    /**
     * An object to configure settings for a SplitPane.  Build an instance of this
     * class to define such things as the divider thickness, the insets, and the
     * UIIDs to use for the various icons (collapse/expand/drag) on the divider.
     * Once you have set all of the settings, you can pass this to {@link #SplitPane(com.codename1.components.SplitPane.Settings, com.codename1.ui.Component, com.codename1.ui.Component) }
     * to create the corresponding SplitPane instance.
     */
    public static class Settings {
        private int orientation = HORIZONTAL_SPLIT;

        // leave dividerUIID null so that we create the compound border by default.
        // If the dev wants to override the UIID then they're on their own for the border.
        private String dividerUIID = null;
        private String expandButtonUIID = "Label";
        private String collapseButtonUIID = "Label";
        private String dragHandleUIID = "Label";
        private float dividerThicknessMM = 3;
        private boolean showExpandCollapseButtons = true;
        private boolean showDragHandle = true;
        private String minInset = "0";
        private String preferredInset = "50%";
        private String maxInset = "100%";

        private char expandMaterialIcon, collapseMaterialIcon, dragHandleMaterialIcon;
        private Image expandIcon, collapseIcon, dragHandleIcon;

        /**
         * Creates a new Settings with default values.
         */
        public Settings() {

        }

        /**
         * Creates a new Settings with the provided orientation, and insets.
         *
         * @param orientation    The orientation.  One of {@link #HORIZONTAL_SPLIT} or {@link #VERTICAL_SPLIT}.
         * @param minInset       The minimum allowable inset for the divider.  The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "200px".
         * @param preferredInset The default preferred inset for the divider.  The inset should be expressed as a string with both value and unit.  E.g. "75%", "50mm", "20px".
         * @param maxInset       The maximum allowable inset for the divider.  The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "20px".
         */
        public Settings(int orientation, String minInset, String preferredInset, String maxInset) {
            this.orientation = orientation;
            this.minInset = minInset;
            this.preferredInset = preferredInset;
            this.maxInset = maxInset;
        }


        /**
         * Sets the orientation.
         *
         * @param orientation The orientation.  One of {@link #HORIZONTAL_SPLIT} or {@link #VERTICAL_SPLIT}.
         * @return Self for chaining.
         */
        public Settings orientation(int orientation) {
            this.orientation = orientation;
            return this;
        }

        /**
         * Sets the UIID to use for the expand button.  Default is "Label"
         *
         * @param expandButtonUIID The UIID to use for the expand button.
         * @return Self for chaining.
         * @see #collapseButtonUIID(java.lang.String)
         * @see #dragHandleUIID(java.lang.String)
         * @see #buttonUIIDs(java.lang.String)
         */
        public Settings expandButtonUIID(String expandButtonUIID) {
            this.expandButtonUIID = expandButtonUIID;
            return this;
        }

        /**
         * Sets the UIID to use for the collapse button.  Default is "Label"
         *
         * @param collapseButtonUIID The UIID to use for the collapse button.
         * @return Self for chaining.
         * @see #dragHandleUIID(java.lang.String)
         * @see #buttonUIIDs(java.lang.String)
         * @see #expandButtonUIID(java.lang.String)
         */
        public Settings collapseButtonUIID(String collapseButtonUIID) {
            this.collapseButtonUIID = collapseButtonUIID;
            return this;
        }

        /**
         * Sets the UIID to use for the drag handle on the divider.  Default is "Label"
         *
         * @param dragHandleUIID The UIID to use for the drag handle of the divider.
         * @return Self for chaining.
         * @see #buttonUIIDs(java.lang.String)
         * @see #expandButtonUIID(java.lang.String)
         * @see #collapseButtonUIID(java.lang.String)
         */
        public Settings dragHandleUIID(String dragHandleUIID) {
            this.dragHandleUIID = dragHandleUIID;
            return this;
        }

        /**
         * Sets the UIID to use for all of the buttons on the divider.  This includes the drag handle,
         * the collapse button, and the expand button.  This is a convenience method that is equivalent of
         * calling {@link #expandButtonUIID(java.lang.String) }, {@link #collapseButtonUIID(java.lang.String)},
         * and {@link #dragHandleUIID(java.lang.String) } all with the same value.
         *
         * @param uiid The UIID to use for the buttons on the divider.
         * @return Self for chaining.
         */
        public Settings buttonUIIDs(String uiid) {
            this.dragHandleUIID = uiid;
            this.collapseButtonUIID = uiid;
            this.expandButtonUIID = uiid;
            return this;
        }

        /**
         * Sets the icon to use for the collapse button.
         *
         * @param icon
         * @return
         */
        public Settings collapseIcon(Image icon) {
            this.collapseIcon = icon;
            return this;
        }

        /**
         * Sets the icon to use for the expand button.
         *
         * @param icon
         * @return
         */
        public Settings expandIcon(Image icon) {
            this.expandIcon = icon;
            return this;
        }

        /**
         * Sets the icon to use for the drag handle.
         *
         * @param icon
         * @return
         */
        public Settings dragHandleIcon(Image icon) {
            this.dragHandleIcon = icon;
            return this;
        }

        /**
         * Sets the material icon to use for the collapse button.
         *
         * @param icon
         * @return
         */
        public Settings collapseMaterialIcon(char icon) {
            this.collapseMaterialIcon = icon;
            return this;
        }

        /**
         * Sets the material icon to use for the expand button.
         *
         * @param icon
         * @return
         */
        public Settings expandMaterialIcon(char icon) {
            this.expandMaterialIcon = icon;
            return this;
        }

        /**
         * Sets the material icon to use for the drag handle.
         *
         * @param icon
         * @return
         */
        public Settings dragHandleMaterialIcon(char icon) {
            this.dragHandleMaterialIcon = icon;
            return this;
        }

        /**
         * Sets the preferred divider thickness in Millimetres.
         *
         * @param dividerThicknessMM The divider thickness in Millimetres.
         * @return Self for chaining.
         */
        public Settings dividerThicknessMM(float dividerThicknessMM) {
            this.dividerThicknessMM = dividerThicknessMM;
            return this;
        }

        /**
         * A custom UIID to use for the divider.  Leave null to use default.
         *
         * @param uiid The custom UIID
         * @return self for chaining.
         */
        public Settings dividerUIID(String uiid) {
            dividerUIID = uiid;
            return this;
        }

        /**
         * Sets the minimum inset for the divider.
         *
         * @param minInset The minimum allowable inset for the divider.  The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "200px".
         * @return Self for chaining.
         */
        public Settings minInset(String minInset) {
            this.minInset = minInset;
            return this;
        }

        /**
         * Sets the preferred inset for the divider.
         *
         * @param preferredInset The preferred inset for the divider.  The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "200px".
         * @return Self for chaining.
         */
        public Settings preferredInset(String preferredInset) {
            this.preferredInset = preferredInset;
            return this;
        }

        /**
         * Sets the max inset for the divider.
         *
         * @param maxInset The max inset for the divider. The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "200px".
         * @return
         */
        public Settings maxInset(String maxInset) {
            this.maxInset = maxInset;
            return this;
        }

        /**
         * Sets the min, preferred, and max insets in a single method.  This is equivalent of calling {@link #minInset(java.lang.String) },
         * {@link #maxInset(java.lang.String) }, and {@link #preferredInset(java.lang.String) } separately.
         *
         * @param min       The min inset for the divider. The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "200px".
         * @param preferred The preferred inset for the divider. The inset should be expressed as a string with both a value and a unit.  E.g. "75%", "50mm", "200px".
         * @param max       The max inset for the divider.
         * @return Self for chaining.
         */
        public Settings insets(String min, String preferred, String max) {
            this.minInset = min;
            this.preferredInset = preferred;
            this.maxInset = max;
            return this;
        }

        /**
         * Set whether to show the expand/collapse buttons on the divider. Default is {@literal true}.
         *
         * @param show {@literal true} to show the expand/collapse buttons.  {@literal false} to hide them.
         * @return Self for chaining.
         */
        public Settings showExpandCollapseButtons(boolean show) {
            this.showExpandCollapseButtons = show;
            return this;
        }

        /**
         * Set whether to show the drag handle on the divider.  Default is {@literal true}.
         *
         * @param show {@literal true} to show the expand/collapse buttons.  {@literal false} to hide them.
         * @return Self for chaining.
         */
        public Settings showDragHandle(boolean show) {
            this.showDragHandle = show;
            return this;
        }

    }

    /**
     * Internal component used as the divider.  This responds to drag events and
     * updates its own insets.  The parent layout is layerd layout, and the left and
     * right containers are anchored to the divider so they are automatically resized
     * according to the divider's location.
     */
    private class Divider extends Container {
        private final Button btnCollapse;
        private final Button btnExpand;
        private final Label dragHandle;
        int pressedX, pressedY, draggedX, draggedY;
        LayeredLayoutConstraint pressedPreferredConstraint;
        LayeredLayoutConstraint pressedConstraint;
        private boolean inDrag;


        Divider() {
            super(new LayeredLayout());
            if (dividerUIID != null) {
                setUIID(dividerUIID);
            }
            btnCollapse = $(new Button())
                    .setUIID(collapseButtonUIID)
                    .setCursor(Component.HAND_CURSOR)
                    .each(new ComponentClosure() {
                        @Override
                        public void call(Component c) {
                            if (collapseIcon != null) {
                                ((Label) c).setIcon(collapseIcon);
                            } else {
                                ((Label) c).setMaterialIcon(getCollapseMaterialIcon());
                            }
                        }

                    })
                    .addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            collapse();
                        }
                    })
                    .selectAllStyles()
                    .setMargin(0)
                    .setPadding(0)
                    .asComponent(Button.class);
            btnExpand = $(new Button())
                    .setCursor(Component.HAND_CURSOR)
                    .setUIID(expandButtonUIID)
                    .each(new ComponentClosure() {
                        @Override
                        public void call(Component c) {
                            if (expandIcon != null) {
                                ((Label) c).setIcon(expandIcon);
                            } else {
                                ((Label) c).setMaterialIcon(getExpandMaterialIcon());
                            }
                        }

                    })
                    .addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            expand();
                        }
                    })
                    .selectAllStyles()
                    .setMargin(0)
                    .setPadding(0)
                    .asComponent(Button.class);

            dragHandle = $(new Label())
                    .setUIID(dragHandleUIID)
                    .setIcon(getDragIconImage())
                    .setMargin(0)
                    .setIgnorePointerEvents(true)
                    .setPadding(0)
                    .setDraggable(true)
                    .setCursor(getDragCursor())
                    .asComponent(Label.class);
            if (showExpandCollapseButtons) {
                add(btnCollapse).add(btnExpand);
            }
            if (showDragHandle) {
                add(dragHandle);
            }

            boolean isDesktop = CN.isDesktop();

            LayeredLayout l = (LayeredLayout) getLayout();
            switch (orientation) {
                case HORIZONTAL_SPLIT: {
                    l.setInsets(btnCollapse, "0 0 auto 0")
                            .setInsets(btnExpand, "0 0 auto 0")
                            .setInsets(dragHandle, "auto auto auto auto")
                            .setReferenceComponentTop(btnExpand, btnCollapse, 1f);
                    if (!isDesktop) {
                        // On tablets and phones it is difficult to use the collapse
                        // expand buttons when they are adjacent.
                        // On these devices we'll place them at opposite ends of the divider
                        l.setInsets(btnExpand, "auto 0 0 0")
                                .setReferenceComponentTop(btnExpand, null, 1f);
                    }
                    break;
                }
                default: {
                    l.setInsets(btnCollapse, "0 auto 0 0")
                            .setInsets(btnExpand, "0 auto 0 0")
                            .setInsets(dragHandle, "auto auto auto auto")
                            .setReferenceComponentLeft(btnExpand, btnCollapse, 1f);
                    if (!isDesktop) {
                        // On tablets and phones it is difficult to use the collapse
                        // expand buttons when they are adjacent.
                        // On these devices we'll place them at opposite ends of the divider
                        l.setInsets(btnExpand, "0 0 0 auto")
                                .setReferenceComponentLeft(btnExpand, null, 1f);
                    }
                }
            }

            if (dividerUIID == null) {
                $(this)
                        .setBorder(createBorder())
                        .setCursor(getDragCursor())
                        .setDraggable(true)

                ;
            } else {
                $(this)
                        .setCursor(getDragCursor())
                        .setDraggable(true)

                ;
            }


        }

        private char getCollapseMaterialIcon() {
            if (collapseMaterialIcon != 0) {
                return collapseMaterialIcon;
            }
            switch (orientation) {
                case HORIZONTAL_SPLIT:
                    return 0xe314;
                default:
                    return 0xe316;
            }
        }

        private char getExpandMaterialIcon() {
            if (expandMaterialIcon != 0) {
                return expandMaterialIcon;
            }
            switch (orientation) {
                case HORIZONTAL_SPLIT:
                    return 0xe315;
                default:
                    return 0xe313;
            }
        }

        private Image getDragIconImage() {
            Image img = null;
            if (dragHandleIcon != null) {
                img = dragHandleIcon;
            } else {
                char materialIcon = FontImage.MATERIAL_DRAG_HANDLE;
                if (dragHandleMaterialIcon != 0) {
                    materialIcon = dragHandleMaterialIcon;
                }
                img = FontImage.createMaterial(materialIcon, getStyle(), 3);
            }
            switch (orientation) {
                case HORIZONTAL_SPLIT:
                    return img.rotate90Degrees(true);
                default:
                    return img;
            }

        }

        private int getDragCursor() {
            return orientation == HORIZONTAL_SPLIT ? Component.E_RESIZE_CURSOR : Component.N_RESIZE_CURSOR;
        }

        private Border createBorder() {
            return orientation == HORIZONTAL_SPLIT ? Border.createCompoundBorder(
                    Border.createEmpty(),
                    Border.createEmpty(),
                    Border.createBevelRaised(),
                    Border.createBevelRaised()) :
                    Border.createCompoundBorder(
                            Border.createBevelRaised(),
                            Border.createBevelRaised(),
                            Border.createEmpty(),
                            Border.createEmpty()
                    );

        }

        @Override
        protected boolean isStickyDrag() {
            return true;
        }


        @Override
        protected void initComponent() {
            super.initComponent();
            getComponentForm().setEnableCursors(true);
        }


        @Override
        protected Dimension calcPreferredSize() {
            Display d = Display.getInstance();
            switch (orientation) {
                case VERTICAL_SPLIT:
                    return new Dimension(d.getDisplayWidth(), d.convertToPixels(dividerThicknessMM));
                default:
                    return new Dimension(d.convertToPixels(dividerThicknessMM), d.getDisplayHeight());
            }
        }


        @Override
        public void pointerPressed(int x, int y) {


            super.pointerPressed(x, y);

            pressedX = x;
            pressedY = y;
            pressedConstraint = ((LayeredLayout) getLayout()).getOrCreateConstraint(this).copy();
            pressedPreferredConstraint = preferredInset.copy();
            inDrag = true;
            pointerDragged(x, y);
        }

        @Override
        public void pointerDragged(int x, int y) {

            super.pointerDragged(x, y);
            if (!inDrag) {
                return;
            }
            setVisible(true);
            draggedX = x;
            draggedY = y;
            updateInsets();
            SplitPane.this.revalidate();
        }

        @Override
        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y);
            inDrag = false;
        }

        @Override
        protected void dragFinished(int x, int y) {
            super.dragFinished(x, y);
            if (!isExpanded && !isCollapsed) {
                getDividerInset().constraint().copyTo(preferredInset);
            }
            inDrag = false;
        }


        private void updateInsets() {
            LayeredLayout ll = (LayeredLayout) SplitPane.this.getLayout();
            LayeredLayoutConstraint cnst = pressedConstraint.copy();
            int diff = 0;
            if (orientation == HORIZONTAL_SPLIT) {
                diff = draggedX - pressedX;
                cnst.left().translatePixels(diff, false, getParent());

            } else {
                diff = draggedY - pressedY;
                cnst.top().translatePixels(diff, false, getParent());
            }
            cnst.copyTo(ll.getOrCreateConstraint(this));
            clampInset();


        }

        @Override
        protected Image getDragImage() {
            return null;
        }

        @Override
        protected void drawDraggedImage(Graphics g, Image img, int x, int y) {

        }

        @Override
        protected int getDragRegionStatus(int x, int y) {
            switch (orientation) {
                case HORIZONTAL_SPLIT:
                    return Component.DRAG_REGION_IMMEDIATELY_DRAG_X;
                default:
                    return Component.DRAG_REGION_IMMEDIATELY_DRAG_Y;
            }
        }


    }

}
