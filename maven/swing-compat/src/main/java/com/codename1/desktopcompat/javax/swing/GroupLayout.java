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
/*
 * Adapted for the Swing compatibility layer from
 * CodenameOne/src/com/codename1/ui/layouts/GroupLayout.java, Codename One's
 * port of the group layout: the method names, the alignment and placement
 * enumerations and the size sources follow the javax.swing API, and the
 * layout targets the layer's own java.awt component classes.
 */
package com.codename1.desktopcompat.javax.swing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager2;

/// Lays components out through two independent trees of groups, one per
/// axis: a sequential group places its elements one after another, a
/// parallel group places them over the same span and aligns them in it.
///
/// Gaps may be explicit, or asked of the [LayoutStyle] -- between two named
/// components, between whatever components end up next to each other
/// (`addPreferredGap(ComponentPlacement)` and `setAutoCreateGaps`), and
/// between components and the container edge (`addContainerGap` and
/// `setAutoCreateContainerGaps`).
///
/// Not supported: right to left orientation -- the horizontal axis always
/// runs left to right. A component's baseline is honoured when
/// `getBaseline` answers one, but a component is never stretched along its
/// baseline: every component is treated as having the `OTHER` baseline
/// resize behaviour, which is what a plain `java.awt.Component` reports.
public class GroupLayout implements LayoutManager2 {
    /// Indicates the size from the component or gap should be used.
    public static final int DEFAULT_SIZE = -1;

    /// Indicates the preferred size from the component or gap should be
    /// used.
    public static final int PREFERRED_SIZE = -2;

    // Used in size calculations
    private static final int MIN_SIZE = 0;
    private static final int PREF_SIZE = 1;
    private static final int MAX_SIZE = 2;
    // Used by prepare, indicates min, pref or max isn't going to be used.
    private static final int SPECIFIC_SIZE = 3;
    private static final int UNSET = Integer.MIN_VALUE;

    // The axes, as the groups know them.
    private static final int HORIZONTAL = 1;
    private static final int VERTICAL = 2;

    // How a baseline moves as its owner is resized. The kernel's components
    // do not say, so a component is always BRB_OTHER; the groups derive the
    // other answers from their structure.
    private static final int BRB_NONE = 0;
    private static final int BRB_CONSTANT_ASCENT = 1;
    private static final int BRB_CONSTANT_DESCENT = 2;
    private static final int BRB_OTHER = 4;

    // The gap between two components, or a component and the edge, when
    // either is not a JComponent and the layout style cannot be asked.
    private static final int PLAIN_COMPONENT_GAP = 10;

    // Maps from Component to ComponentInfo.  This is used for tracking
    // information specific to a Component.
    private final Map<Component, ComponentInfo> componentInfos;
    // Container we're doing layout for.
    private final Container host;
    // Used by areParallelSiblings, cached to avoid excessive garbage.
    private final ArrayList<Spring> tmpParallelSet;
    // Whether or not we automatically try and create the preferred
    // padding between components.
    private boolean autocreatePadding;
    // Whether or not we automatically try and create the preferred
    // padding between containers
    private boolean autocreateContainerPadding;
    // Group responsible for layout along the horizontal axis.  This is NOT
    // the user specified group, use getHorizontalGroup to dig that out.
    private Group horizontalGroup;
    // Group responsible for layout along the vertical axis.  This is NOT
    // the user specified group, use getVerticalGroup to dig that out.
    private Group verticalGroup;
    // Indicates Springs have changed in some way since last change.
    private boolean springsChanged;
    // Indicates invalidateLayout has been invoked.
    private boolean isValid;
    // Whether or not any preferred padding (or container padding) springs
    // exist
    private boolean hasPreferredPaddingSprings;
    // The LayoutStyle instance to use, if null the shared instance is used.
    private LayoutStyle layoutStyle;
    // If true, components that are not visible are treated as though they
    // aren't there.
    private boolean honorsVisibility;

    /// The ways a parallel group can align its elements.
    public enum Alignment {
        /// Aligned to the origin: the left, or the top.
        LEADING,
        /// Aligned to the end: the right, or the bottom.
        TRAILING,
        /// Centred in the span.
        CENTER,
        /// Aligned along the baseline; vertical axis only.
        BASELINE
    }

    /// Creates a `GroupLayout` for the specified `Container`.
    public GroupLayout(Container host) {
        if (host == null) {
            throw new IllegalArgumentException("Container must be non-null");
        }
        honorsVisibility = true;
        this.host = host;
        componentInfos = new HashMap<Component, ComponentInfo>();
        tmpParallelSet = new ArrayList<Spring>();
        horizontalGroup = createTopLevelGroup(createParallelGroup(Alignment.LEADING, true));
        verticalGroup = createTopLevelGroup(createParallelGroup(Alignment.LEADING, true));
    }

    private static void checkSize(int min, int pref, int max, boolean isComponentSpring) {
        checkResizeType(min, isComponentSpring);
        if (!isComponentSpring && pref < 0) {
            throw new IllegalArgumentException("Pref must be >= 0");
        } else if (isComponentSpring) {
            checkResizeType(pref, true);
        }
        checkResizeType(max, isComponentSpring);
        checkLessThan(min, pref);
        checkLessThan(pref, max);
    }

    private static void checkResizeType(int type, boolean isComponentSpring) {
        if (type < 0 && ((isComponentSpring && type != DEFAULT_SIZE && type != PREFERRED_SIZE)
                || (!isComponentSpring && type != PREFERRED_SIZE))) {
            throw new IllegalArgumentException("Invalid size");
        }
    }

    private static void checkLessThan(int min, int max) {
        if (min >= 0 && max >= 0 && min > max) {
            throw new IllegalArgumentException("Following is not met: min<=pref<=max");
        }
    }

    /// Sets whether component visibility is considered when sizing and
    /// positioning components. `true`, the default, leaves a component that
    /// is not visible out of the layout.
    public void setHonorsVisibility(boolean honorsVisibility) {
        if (this.honorsVisibility != honorsVisibility) {
            this.honorsVisibility = honorsVisibility;
            springsChanged = true;
            isValid = false;
            invalidateHost();
        }
    }

    /// Returns whether component visibility is considered when sizing and
    /// positioning components.
    public boolean getHonorsVisibility() {
        return honorsVisibility;
    }

    /// Sets whether the visibility of one component is considered; `null`
    /// returns the component to the value of the layout as a whole.
    public void setHonorsVisibility(Component component, Boolean honorsVisibility) {
        if (component == null) {
            throw new IllegalArgumentException("Component must be non-null");
        }
        getComponentInfo(component).setHonorsVisibility(honorsVisibility);
        springsChanged = true;
        isValid = false;
        invalidateHost();
    }

    /// Sets whether a gap between components that end up next to each
    /// other is created automatically.
    public void setAutoCreateGaps(boolean autoCreatePadding) {
        if (this.autocreatePadding != autoCreatePadding) {
            this.autocreatePadding = autoCreatePadding;
            invalidateHost();
        }
    }

    /// Returns `true` if gaps between components are automatically
    /// created.
    public boolean getAutoCreateGaps() {
        return autocreatePadding;
    }

    /// Sets whether a gap between the container and the components that
    /// touch its border is created automatically.
    public void setAutoCreateContainerGaps(boolean autoCreateContainerPadding) {
        if (this.autocreateContainerPadding != autoCreateContainerPadding) {
            this.autocreateContainerPadding = autoCreateContainerPadding;
            horizontalGroup = createTopLevelGroup(getHorizontalGroup());
            verticalGroup = createTopLevelGroup(getVerticalGroup());
            invalidateHost();
        }
    }

    /// Returns `true` if gaps between the container and the components
    /// that border it are automatically created.
    public boolean getAutoCreateContainerGaps() {
        return autocreateContainerPadding;
    }

    /// Sets the `Group` that positions and sizes components along the
    /// horizontal axis.
    public void setHorizontalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        horizontalGroup = createTopLevelGroup(group);
        invalidateHost();
    }

    private Group getHorizontalGroup() {
        int index = 0;
        if (horizontalGroup.springs.size() > 1) {
            index = 1;
        }
        return asGroup(horizontalGroup.springs.get(index));
    }

    /// Sets the `Group` that positions and sizes components along the
    /// vertical axis.
    public void setVerticalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        verticalGroup = createTopLevelGroup(group);
        invalidateHost();
    }

    private Group getVerticalGroup() {
        int index = 0;
        if (verticalGroup.springs.size() > 1) {
            index = 1;
        }
        return asGroup(verticalGroup.springs.get(index));
    }

    private static Group asGroup(Spring spring) {
        if (spring instanceof Group) {
            return (Group) spring;
        }
        throw new IllegalStateException("Not a group");
    }

    /// Wraps the user specified group in a sequential group.  If
    /// container gaps should be generated the necessary springs are
    /// added.
    private Group createTopLevelGroup(Group specifiedGroup) {
        SequentialGroup group = createSequentialGroup();
        if (getAutoCreateContainerGaps()) {
            group.addSpring(new ContainerAutoPreferredGapSpring());
            group.addGroup(specifiedGroup);
            group.addSpring(new ContainerAutoPreferredGapSpring());
        } else {
            group.addGroup(specifiedGroup);
        }
        return group;
    }

    /// Creates and returns a `SequentialGroup`.
    public SequentialGroup createSequentialGroup() {
        return new SequentialGroup();
    }

    /// Creates and returns a resizable `ParallelGroup` with an alignment
    /// of `Alignment.LEADING`.
    public ParallelGroup createParallelGroup() {
        return createParallelGroup(Alignment.LEADING);
    }

    /// Creates and returns a resizable `ParallelGroup` with the specified
    /// alignment.
    public ParallelGroup createParallelGroup(Alignment alignment) {
        return createParallelGroup(alignment, true);
    }

    /// Creates and returns a `ParallelGroup` with the specified alignment
    /// and resize behaviour. A group that is not resizable has its
    /// preferred size for a minimum and a maximum.
    public ParallelGroup createParallelGroup(Alignment alignment, boolean resizable) {
        if (alignment == null) {
            throw new IllegalArgumentException("alignment must be non null");
        }
        if (alignment == Alignment.BASELINE) {
            return new BaselineGroup(resizable);
        }
        return new ParallelGroup(alignment, resizable);
    }

    /// Creates and returns a `ParallelGroup` that aligns its elements
    /// along the baseline.
    public ParallelGroup createBaselineGroup(boolean resizable, boolean anchorBaselineToTop) {
        return new BaselineGroup(resizable, anchorBaselineToTop);
    }

    /// Forces the specified components to have the same size regardless
    /// of their preferred, minimum or maximum sizes: all take the largest
    /// preferred size among them.
    public void linkSize(Component... components) {
        linkSize(SwingConstants.HORIZONTAL, components);
        linkSize(SwingConstants.VERTICAL, components);
    }

    /// Forces the specified components to have the same size along the
    /// specified axis, `SwingConstants.HORIZONTAL` or
    /// `SwingConstants.VERTICAL`.
    public void linkSize(int axis, Component... components) {
        if (components == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        for (int counter = components.length - 1; counter >= 0; counter--) {
            Component c = components[counter];
            if (c == null) {
                throw new IllegalArgumentException("Components must be non-null");
            }
            // Force the component to be added
            getComponentInfo(c);
        }
        int glAxis;
        if (axis == SwingConstants.HORIZONTAL) {
            glAxis = HORIZONTAL;
        } else if (axis == SwingConstants.VERTICAL) {
            glAxis = VERTICAL;
        } else {
            throw new IllegalArgumentException(
                    "Axis must be one of SwingConstants.HORIZONTAL or SwingConstants.VERTICAL");
        }
        if (components.length == 0) {
            return;
        }
        LinkInfo master = getComponentInfo(components[components.length - 1]).getLinkInfo(glAxis);
        for (int counter = components.length - 2; counter >= 0; counter--) {
            master.add(getComponentInfo(components[counter]));
        }
        invalidateHost();
    }

    /// Replaces an existing component with a new one.
    public void replace(Component existingComponent, Component newComponent) {
        if (existingComponent == null || newComponent == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        // Make sure all the components have been registered, otherwise we may
        // not update the correct Springs.
        if (springsChanged) {
            registerComponents(horizontalGroup, HORIZONTAL);
            registerComponents(verticalGroup, VERTICAL);
        }
        ComponentInfo info = componentInfos.remove(existingComponent);
        if (info == null) {
            throw new IllegalArgumentException("Component must already exist");
        }
        host.remove(existingComponent);
        if (newComponent.getParent() != host) {
            host.add(newComponent);
        }
        info.setComponent(newComponent);
        componentInfos.put(newComponent, info);
        invalidateHost();
    }

    /// Sets the `LayoutStyle` used to calculate the preferred gaps between
    /// components. `null` means the shared instance of `LayoutStyle`.
    public void setLayoutStyle(LayoutStyle layoutStyle) {
        this.layoutStyle = layoutStyle;
        invalidateHost();
    }

    /// Returns the `LayoutStyle` used for calculating the preferred gap
    /// between components, or `null` when the shared instance is used.
    public LayoutStyle getLayoutStyle() {
        return layoutStyle;
    }

    private LayoutStyle getLayoutStyle0() {
        LayoutStyle style = getLayoutStyle();
        if (style == null) {
            style = LayoutStyle.getInstance();
        }
        return style;
    }

    private void invalidateHost() {
        if (host instanceof JComponent) {
            ((JComponent) host).revalidate();
        } else {
            host.invalidate();
        }
        host.repaint();
    }

    //
    // LayoutManager
    //

    /// Does nothing: components are added through the groups.
    @Override
    public void addLayoutComponent(String name, Component component) {
    }

    /// Notification that a `Component` has been removed from the parent
    /// container.
    @Override
    public void removeLayoutComponent(Component component) {
        ComponentInfo info = componentInfos.remove(component);
        if (info != null) {
            info.dispose();
            springsChanged = true;
            isValid = false;
        }
    }

    /// Returns the preferred size for the specified container.
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        checkParent(parent);
        prepare(PREF_SIZE);
        return adjustSize(horizontalGroup.getPreferredSize(HORIZONTAL), verticalGroup.getPreferredSize(VERTICAL));
    }

    /// Returns the minimum size for the specified container.
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        checkParent(parent);
        prepare(MIN_SIZE);
        return adjustSize(horizontalGroup.getMinimumSize(HORIZONTAL), verticalGroup.getMinimumSize(VERTICAL));
    }

    /// Lays out the specified container.
    @Override
    public void layoutContainer(Container parent) {
        // Step 1: Prepare for layout.
        prepare(SPECIFIC_SIZE);
        Insets insets = parent.getInsets();
        int width = parent.getWidth() - insets.left - insets.right;
        int height = parent.getHeight() - insets.top - insets.bottom;
        if (getAutoCreateGaps() || getAutoCreateContainerGaps() || hasPreferredPaddingSprings) {
            // Step 2: Calculate autopadding springs
            calculateAutopadding(horizontalGroup, HORIZONTAL, SPECIFIC_SIZE, 0, width);
            calculateAutopadding(verticalGroup, VERTICAL, SPECIFIC_SIZE, 0, height);
        }
        // Step 3: set the size of the groups.
        horizontalGroup.setSize(HORIZONTAL, 0, width);
        verticalGroup.setSize(VERTICAL, 0, height);
        // Step 4: apply the size to the components.
        for (ComponentInfo info : componentInfos.values()) {
            info.setBounds(insets);
        }
    }

    //
    // LayoutManager2
    //

    /// Does nothing: components are added through the groups.
    @Override
    public void addLayoutComponent(Component component, Object constraints) {
    }

    /// Returns the maximum size for the specified container.
    @Override
    public Dimension maximumLayoutSize(Container parent) {
        checkParent(parent);
        prepare(MAX_SIZE);
        return adjustSize(horizontalGroup.getMaximumSize(HORIZONTAL), verticalGroup.getMaximumSize(VERTICAL));
    }

    /// Returns the alignment along the x axis, always the centre.
    @Override
    public float getLayoutAlignmentX(Container parent) {
        checkParent(parent);
        return .5f;
    }

    /// Returns the alignment along the y axis, always the centre.
    @Override
    public float getLayoutAlignmentY(Container parent) {
        checkParent(parent);
        return .5f;
    }

    /// Invalidates the layout, discarding the sizes it cached.
    @Override
    public void invalidateLayout(Container parent) {
        checkParent(parent);
        isValid = false;
    }

    private void prepare(int sizeType) {
        boolean visChanged = false;
        // Step 1: If not-valid, clear springs and update visibility.
        if (!isValid) {
            isValid = true;
            horizontalGroup.setSize(HORIZONTAL, UNSET, UNSET);
            verticalGroup.setSize(VERTICAL, UNSET, UNSET);
            for (ComponentInfo ci : componentInfos.values()) {
                if (ci.updateVisibility()) {
                    visChanged = true;
                }
                ci.clearCachedSize();
            }
        }
        // Step 2: Make sure components are bound to ComponentInfos
        if (springsChanged) {
            registerComponents(horizontalGroup, HORIZONTAL);
            registerComponents(verticalGroup, VERTICAL);
        }
        // Step 3: Adjust the autopadding. This removes existing
        // autopadding, then recalculates where it should go.
        if (springsChanged || visChanged) {
            checkComponents();
            horizontalGroup.removeAutopadding();
            verticalGroup.removeAutopadding();
            if (getAutoCreateGaps()) {
                insertAutopadding(true);
            } else if (hasPreferredPaddingSprings || getAutoCreateContainerGaps()) {
                insertAutopadding(false);
            }
            springsChanged = false;
        }
        // Step 4: (for min/pref/max size calculations only) calculate the
        // autopadding. This invokes for unsetting the calculated values, then
        // recalculating them.
        // If sizeType == SPECIFIC_SIZE, it indicates we're doing layout, this
        // step will be done later on.
        if (sizeType != SPECIFIC_SIZE
                && (getAutoCreateGaps() || getAutoCreateContainerGaps() || hasPreferredPaddingSprings)) {
            calculateAutopadding(horizontalGroup, HORIZONTAL, sizeType, 0, 0);
            calculateAutopadding(verticalGroup, VERTICAL, sizeType, 0, 0);
        }
    }

    private void calculateAutopadding(Group group, int axis, int sizeType, int origin, int size) {
        group.unsetAutopadding();
        switch (sizeType) {
            case MIN_SIZE:
                size = group.getMinimumSize(axis);
                break;
            case PREF_SIZE:
                size = group.getPreferredSize(axis);
                break;
            case MAX_SIZE:
                size = group.getMaximumSize(axis);
                break;
            default:
                break;
        }
        group.setSize(axis, origin, size);
        group.calculateAutopadding(axis);
    }

    private void checkComponents() {
        for (ComponentInfo info : componentInfos.values()) {
            if (info.horizontalSpring == null) {
                throw new IllegalStateException(info.component + " is not attached to a horizontal group");
            }
            if (info.verticalSpring == null) {
                throw new IllegalStateException(info.component + " is not attached to a vertical group");
            }
        }
    }

    private void registerComponents(Group group, int axis) {
        List<Spring> springs = group.springs;
        for (int counter = springs.size() - 1; counter >= 0; counter--) {
            Spring spring = springs.get(counter);
            if (spring instanceof ComponentSpring) {
                ((ComponentSpring) spring).installIfNecessary(axis);
            } else if (spring instanceof Group) {
                registerComponents((Group) spring, axis);
            }
        }
    }

    private Dimension adjustSize(int width, int height) {
        Insets insets = host.getInsets();
        return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
    }

    private void checkParent(Container parent) {
        if (parent != host) {
            throw new IllegalArgumentException("GroupLayout can only be used with one Container at a time");
        }
    }

    /// Returns the `ComponentInfo` for the specified Component,
    /// creating one if necessary.
    private ComponentInfo getComponentInfo(Component component) {
        ComponentInfo info = componentInfos.get(component);
        if (info == null) {
            info = new ComponentInfo(component);
            componentInfos.put(component, info);
            if (component.getParent() != host) {
                host.add(component);
            }
        }
        return info;
    }

    /// Adjusts the autopadding springs for the horizontal and vertical
    /// groups.  If `insert` is true this will insert auto padding
    /// springs, otherwise this will only adjust the springs that
    /// comprise auto preferred padding springs.
    private void insertAutopadding(boolean insert) {
        horizontalGroup.insertAutopadding(HORIZONTAL, new ArrayList<AutoPreferredGapSpring>(1),
                new ArrayList<AutoPreferredGapSpring>(1), new ArrayList<ComponentSpring>(1),
                new ArrayList<ComponentSpring>(1), insert);
        verticalGroup.insertAutopadding(VERTICAL, new ArrayList<AutoPreferredGapSpring>(1),
                new ArrayList<AutoPreferredGapSpring>(1), new ArrayList<ComponentSpring>(1),
                new ArrayList<ComponentSpring>(1), insert);
    }

    /// Returns true if the two Components have a common ParallelGroup
    /// ancestor along the particular axis.
    private boolean areParallelSiblings(Component source, Component target, int axis) {
        ComponentInfo sourceInfo = getComponentInfo(source);
        ComponentInfo targetInfo = getComponentInfo(target);
        Spring sourceSpring;
        Spring targetSpring;
        if (axis == HORIZONTAL) {
            sourceSpring = sourceInfo.horizontalSpring;
            targetSpring = targetInfo.horizontalSpring;
        } else {
            sourceSpring = sourceInfo.verticalSpring;
            targetSpring = targetInfo.verticalSpring;
        }
        ArrayList<Spring> sourcePath = tmpParallelSet;
        sourcePath.clear();
        Spring spring = sourceSpring.getParent();
        while (spring != null) {
            sourcePath.add(spring);
            spring = spring.getParent();
        }
        spring = targetSpring.getParent();
        while (spring != null) {
            if (containsIdentical(sourcePath, spring)) {
                sourcePath.clear();
                while (spring != null) {
                    if (spring instanceof ParallelGroup) {
                        return true;
                    }
                    spring = spring.getParent();
                }
                return false;
            }
            spring = spring.getParent();
        }
        sourcePath.clear();
        return false;
    }

    private static boolean containsIdentical(ArrayList<Spring> list, Spring spring) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) == spring) {
                return true;
            }
        }
        return false;
    }

    /// Used in figuring out how much space to give resizable springs.
    private static final class SpringDelta {
        // Original index.
        final int index;
        // Delta, one of pref - min or max - pref.
        int delta;

        SpringDelta(int index, int delta) {
            this.index = index;
            this.delta = delta;
        }
    }

    /// Represents two springs that should have autopadding inserted between
    /// them.
    private static final class AutoPreferredGapMatch {
        final ComponentSpring source;
        final ComponentSpring target;

        AutoPreferredGapMatch(ComponentSpring source, ComponentSpring target) {
            this.source = source;
            this.target = target;
        }
    }

    // LinkInfo contains the set of ComponentInfos that are linked along a
    // particular axis.
    private static final class LinkInfo {
        private final int axis;
        private final List<ComponentInfo> linked;
        private int size;

        LinkInfo(int axis) {
            linked = new ArrayList<ComponentInfo>();
            size = UNSET;
            this.axis = axis;
        }

        void add(ComponentInfo child) {
            LinkInfo childMaster = child.getLinkInfo(axis, false);
            if (childMaster == null) {
                linked.add(child);
                child.setLinkInfo(axis, this);
            } else if (childMaster != this) {
                linked.addAll(childMaster.linked);
                for (int i = 0; i < childMaster.linked.size(); i++) {
                    childMaster.linked.get(i).setLinkInfo(axis, this);
                }
            }
            clearCachedSize();
        }

        void remove(ComponentInfo info) {
            linked.remove(info);
            info.setLinkInfo(axis, null);
            if (linked.size() == 1) {
                linked.get(0).setLinkInfo(axis, null);
            }
            clearCachedSize();
        }

        void clearCachedSize() {
            size = UNSET;
        }

        int getSize(int axis) {
            if (size == UNSET) {
                size = calculateLinkedSize(axis);
            }
            return size;
        }

        private int calculateLinkedSize(int axis) {
            int size = 0;
            for (int i = 0; i < linked.size(); i++) {
                ComponentInfo info = linked.get(i);
                ComponentSpring spring;
                if (axis == HORIZONTAL) {
                    spring = info.horizontalSpring;
                } else {
                    spring = info.verticalSpring;
                }
                size = Math.max(size, spring.calculateNonlinkedPreferredSize(axis));
            }
            return size;
        }
    }

    /// Spring consists of a range: min, pref and max, a value some where in
    /// the middle of that, and a location.  Spring caches the
    /// min/max/pref.  If the min/pref/max has internally changes, or needs
    /// to be updated you must invoke clear.
    private abstract class Spring {
        private int size;
        private int min;
        private int max;
        private int pref;
        private Spring parent;

        // Null when the spring takes the alignment of its parallel group.
        private Alignment alignment;

        Spring() {
            min = pref = max = UNSET;
        }

        abstract int calculateMinimumSize(int axis);

        abstract int calculatePreferredSize(int axis);

        abstract int calculateMaximumSize(int axis);

        Spring getParent() {
            return parent;
        }

        void setParent(Spring parent) {
            this.parent = parent;
        }

        Alignment getAlignment() {
            return alignment;
        }

        // This is here purely as a convenience for ParallelGroup to avoid
        // having to track alignment separately.
        void setAlignment(Alignment alignment) {
            this.alignment = alignment;
        }

        final int getMinimumSize(int axis) {
            if (min == UNSET) {
                min = constrain(calculateMinimumSize(axis));
            }
            return min;
        }

        final int getPreferredSize(int axis) {
            if (pref == UNSET) {
                pref = constrain(calculatePreferredSize(axis));
            }
            return pref;
        }

        final int getMaximumSize(int axis) {
            if (max == UNSET) {
                max = constrain(calculateMaximumSize(axis));
            }
            return max;
        }

        /// Resets the cached min/max/pref.
        void unset() {
            size = min = pref = max = UNSET;
        }

        /// Sets the value and location of the spring.  Subclasses
        /// will want to invoke super, then do any additional sizing.
        void setSize(int axis, int origin, int size) {
            this.size = size;
            if (size == UNSET) {
                unset();
            }
        }

        int getSize() {
            return size;
        }

        int constrain(int value) {
            return Math.min(value, Short.MAX_VALUE);
        }

        int getBaseline() {
            return -1;
        }

        int getBaselineResizeBehavior() {
            return BRB_OTHER;
        }

        final boolean isResizable(int axis) {
            int min = getMinimumSize(axis);
            int pref = getPreferredSize(axis);
            return min != pref || pref != getMaximumSize(axis);
        }

        /// Returns true if this Spring will ALWAYS have a zero size. This
        /// should NOT check the current size, rather it's meant to
        /// quickly test if this Spring will always have a zero size.
        abstract boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized);
    }

    /// What the two kinds of group have in common: a list of components,
    /// gaps and other groups. Created by `createSequentialGroup` and
    /// `createParallelGroup`.
    public abstract class Group extends Spring {
        List<Spring> springs;

        Group() {
            springs = new ArrayList<Spring>();
        }

        /// Adds a `Group` to this `Group`.
        public Group addGroup(Group group) {
            return addSpring(group);
        }

        /// Adds a `Component` to this `Group`.
        public Group addComponent(Component component) {
            return addComponent(component, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /// Adds a `Component` to this `Group` with the specified sizes,
        /// each a size, `DEFAULT_SIZE` or `PREFERRED_SIZE`.
        public Group addComponent(Component component, int min, int pref, int max) {
            return addSpring(new ComponentSpring(component, min, pref, max));
        }

        /// Adds a rigid gap to this `Group`.
        public Group addGap(int size) {
            return addGap(size, size, size);
        }

        /// Adds a gap to this `Group` with the specified sizes.
        public Group addGap(int min, int pref, int max) {
            return addSpring(new GapSpring(min, pref, max));
        }

        Spring getSpring(int index) {
            return springs.get(index);
        }

        int indexOf(Spring spring) {
            return springs.indexOf(spring);
        }

        /// Adds the Spring to the list of `Spring`s and returns
        /// the receiver.
        Group addSpring(Spring spring) {
            springs.add(spring);
            spring.setParent(this);
            if (!(spring instanceof AutoPreferredGapSpring) || !((AutoPreferredGapSpring) spring).getUserCreated()) {
                springsChanged = true;
            }
            return this;
        }

        //
        // Spring methods
        //

        @Override
        void setSize(int axis, int origin, int size) {
            super.setSize(axis, origin, size);
            if (size == UNSET) {
                for (int counter = springs.size() - 1; counter >= 0; counter--) {
                    getSpring(counter).setSize(axis, origin, size);
                }
            } else {
                setValidSize(axis, origin, size);
            }
        }

        /// This is invoked from `setSize` if passed a value
        /// other than UNSET.
        abstract void setValidSize(int axis, int origin, int size);

        @Override
        int calculateMinimumSize(int axis) {
            return calculateSize(axis, MIN_SIZE);
        }

        @Override
        int calculatePreferredSize(int axis) {
            return calculateSize(axis, PREF_SIZE);
        }

        @Override
        int calculateMaximumSize(int axis) {
            return calculateSize(axis, MAX_SIZE);
        }

        /// Used to compute how the two values representing two springs
        /// will be combined.  For example, a group that layed things out
        /// one after the next would return `a + b`.
        abstract int operator(int a, int b);

        /// Calculates the specified size, combining the springs with
        /// `operator`.
        int calculateSize(int axis, int type) {
            int count = springs.size();
            if (count == 0) {
                return 0;
            }
            if (count == 1) {
                return getSpringSize(getSpring(0), axis, type);
            }
            int size = constrain(
                    operator(getSpringSize(getSpring(0), axis, type), getSpringSize(getSpring(1), axis, type)));
            for (int counter = 2; counter < count; counter++) {
                size = constrain(operator(size, getSpringSize(getSpring(counter), axis, type)));
            }
            return size;
        }

        int getSpringSize(Spring spring, int axis, int type) {
            switch (type) {
                case MIN_SIZE:
                    return spring.getMinimumSize(axis);
                case PREF_SIZE:
                    return spring.getPreferredSize(axis);
                case MAX_SIZE:
                    return spring.getMaximumSize(axis);
                default:
                    break;
            }
            return 0;
        }

        // Padding

        /// Adjusts the autopadding springs in this group and its children.
        /// If `insert` is true this will insert auto padding
        /// springs, otherwise this will only adjust the springs that
        /// comprise auto preferred padding springs.
        ///
        /// `leadingPadding` holds the padding springs that occur before
        /// this group and `leading` the component springs that do; on exit
        /// the trailing ones of this group have been added to
        /// `trailingPadding` and `trailing`.
        abstract void insertAutopadding(int axis, List<AutoPreferredGapSpring> leadingPadding,
                List<AutoPreferredGapSpring> trailingPadding, List<ComponentSpring> leading,
                List<ComponentSpring> trailing, boolean insert);

        /// Removes any AutoPreferredGapSprings the layout inserted, and
        /// resets the ones the user created.
        void removeAutopadding() {
            unset();
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = springs.get(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    if (((AutoPreferredGapSpring) spring).getUserCreated()) {
                        ((AutoPreferredGapSpring) spring).reset();
                    } else {
                        springs.remove(counter);
                    }
                } else if (spring instanceof Group) {
                    ((Group) spring).removeAutopadding();
                }
            }
        }

        void unsetAutopadding() {
            // Clear cached pref/min/max.
            unset();
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = springs.get(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    spring.unset();
                } else if (spring instanceof Group) {
                    ((Group) spring).unsetAutopadding();
                }
            }
        }

        void calculateAutopadding(int axis) {
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = springs.get(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    // Force size to be reset.
                    spring.unset();
                    ((AutoPreferredGapSpring) spring).calculatePadding(axis);
                } else if (spring instanceof Group) {
                    ((Group) spring).calculateAutopadding(axis);
                }
            }
            // Clear cached pref/min/max.
            unset();
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            for (int i = springs.size() - 1; i >= 0; i--) {
                Spring spring = springs.get(i);
                if (!spring.willHaveZeroSize(treatAutopaddingAsZeroSized)) {
                    return false;
                }
            }
            return true;
        }
    }

    /// A `Group` that positions and sizes its elements sequentially, one
    /// after another. Created by `createSequentialGroup`.
    public class SequentialGroup extends Group {
        private Spring baselineSpring;

        SequentialGroup() {
        }

        @Override
        public SequentialGroup addGroup(Group group) {
            super.addGroup(group);
            return this;
        }

        /// Adds a `Group` to this `Group`, optionally as the element this
        /// group takes its baseline from.
        public SequentialGroup addGroup(boolean useAsBaseline, Group group) {
            super.addGroup(group);
            if (useAsBaseline) {
                baselineSpring = group;
            }
            return this;
        }

        @Override
        public SequentialGroup addComponent(Component component) {
            super.addComponent(component);
            return this;
        }

        /// Adds a `Component` to this `Group`, optionally as the element
        /// this group takes its baseline from.
        public SequentialGroup addComponent(boolean useAsBaseline, Component component) {
            super.addComponent(component);
            if (useAsBaseline) {
                baselineSpring = springs.get(springs.size() - 1);
            }
            return this;
        }

        @Override
        public SequentialGroup addComponent(Component component, int min, int pref, int max) {
            super.addComponent(component, min, pref, max);
            return this;
        }

        /// Adds a `Component` to this `Group` with the specified sizes,
        /// optionally as the element this group takes its baseline from.
        public SequentialGroup addComponent(boolean useAsBaseline, Component component, int min, int pref,
                int max) {
            super.addComponent(component, min, pref, max);
            if (useAsBaseline) {
                baselineSpring = springs.get(springs.size() - 1);
            }
            return this;
        }

        @Override
        public SequentialGroup addGap(int size) {
            super.addGap(size);
            return this;
        }

        @Override
        public SequentialGroup addGap(int min, int pref, int max) {
            super.addGap(min, pref, max);
            return this;
        }

        /// Adds an element representing the preferred gap between two
        /// components, as the layout style gives it.
        public SequentialGroup addPreferredGap(JComponent comp1, JComponent comp2,
                LayoutStyle.ComponentPlacement type) {
            return addPreferredGap(comp1, comp2, type, DEFAULT_SIZE, PREFERRED_SIZE);
        }

        /// Adds an element representing the preferred gap between two
        /// components, with a preferred and a maximum size of its own.
        public SequentialGroup addPreferredGap(JComponent comp1, JComponent comp2,
                LayoutStyle.ComponentPlacement type, int pref, int max) {
            if (type == null) {
                throw new IllegalArgumentException("Type must be non-null");
            }
            if (comp1 == null || comp2 == null) {
                throw new IllegalArgumentException("Components must be non-null");
            }
            checkPreferredGapValues(pref, max);
            addSpring(new PreferredGapSpring(comp1, comp2, type, pref, max));
            return this;
        }

        /// Adds an element representing the preferred gap between the
        /// nearest components, whichever they turn out to be.
        public SequentialGroup addPreferredGap(LayoutStyle.ComponentPlacement type) {
            return addPreferredGap(type, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /// Adds an element representing the preferred gap between the
        /// nearest components, with a preferred and a maximum size of its
        /// own.
        public SequentialGroup addPreferredGap(LayoutStyle.ComponentPlacement type, int pref, int max) {
            if (type != LayoutStyle.ComponentPlacement.RELATED && type != LayoutStyle.ComponentPlacement.UNRELATED) {
                throw new IllegalArgumentException(
                        "Type must be one of LayoutStyle.ComponentPlacement.RELATED or "
                                + "LayoutStyle.ComponentPlacement.UNRELATED");
            }
            checkPreferredGapValues(pref, max);
            hasPreferredPaddingSprings = true;
            addSpring(new AutoPreferredGapSpring(type, pref, max));
            return this;
        }

        /// Adds an element representing the preferred gap between an edge
        /// of the container and the components that touch it.
        public SequentialGroup addContainerGap() {
            return addContainerGap(DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /// Adds a container gap with a preferred and a maximum size of
        /// its own.
        public SequentialGroup addContainerGap(int pref, int max) {
            if ((pref < 0 && pref != DEFAULT_SIZE) || (max < 0 && max != DEFAULT_SIZE && max != PREFERRED_SIZE)
                    || (pref >= 0 && max >= 0 && pref > max)) {
                throw new IllegalArgumentException(
                        "Pref and max must be either DEFAULT_VALUE or >= 0 and pref <= max");
            }
            hasPreferredPaddingSprings = true;
            addSpring(new ContainerAutoPreferredGapSpring(pref, max));
            return this;
        }

        private void checkPreferredGapValues(int pref, int max) {
            if ((pref < 0 && pref != DEFAULT_SIZE && pref != PREFERRED_SIZE)
                    || (max < 0 && max != DEFAULT_SIZE && max != PREFERRED_SIZE)
                    || (pref >= 0 && max >= 0 && pref > max)) {
                throw new IllegalArgumentException(
                        "Pref and max must be either DEFAULT_SIZE, PREFERRED_SIZE, or >= 0 and pref <= max");
            }
        }

        @Override
        int operator(int a, int b) {
            return constrain(a) + constrain(b);
        }

        @Override
        void setValidSize(int axis, int origin, int size) {
            int pref = getPreferredSize(axis);
            if (size == pref) {
                // Layout at preferred size
                for (int counter = 0, max = springs.size(); counter < max; counter++) {
                    Spring spring = getSpring(counter);
                    int springPref = spring.getPreferredSize(axis);
                    spring.setSize(axis, origin, springPref);
                    origin += springPref;
                }
            } else if (springs.size() == 1) {
                Spring spring = getSpring(0);
                spring.setSize(axis, origin,
                        Math.min(Math.max(size, spring.getMinimumSize(axis)), spring.getMaximumSize(axis)));
            } else if (springs.size() > 1) {
                // Adjust between min/pref
                setValidSizeNotPreferred(axis, origin, size);
            }
        }

        private void setValidSizeNotPreferred(int axis, int origin, int size) {
            int delta = size - getPreferredSize(axis);
            boolean useMin = delta < 0;
            int springCount = springs.size();
            if (useMin) {
                delta *= -1;
            }
            // The following algorithm if used for resizing springs:
            // 1. Calculate the resizability of each spring (pref - min or
            //    max - pref) into a list.
            // 2. Sort the list in ascending order
            // 3. Iterate through each of the resizable Springs, attempting
            //    to give them (pref - size) / resizeCount
            // 4. For any Springs that can not accommodate that much space
            //    add the remainder back to the amount to distribute and
            //    recalculate how must space the remaining springs will get.
            // 5. Set the size of the springs.

            // First pass, sort the resizable springs into resizable
            List<SpringDelta> resizable = buildResizableList(axis, useMin);
            int resizableCount = resizable.size();
            if (resizableCount > 0) {
                // How much we would like to give each Spring.
                int sDelta = delta / resizableCount;
                // Remaining space.
                int slop = delta - sDelta * resizableCount;
                int[] sizes = new int[springCount];
                int sign = useMin ? -1 : 1;
                // Second pass, accumulate the resulting deltas (relative to
                // preferred) into sizes.
                for (int counter = 0; counter < resizableCount; counter++) {
                    SpringDelta springDelta = resizable.get(counter);
                    if ((counter + 1) == resizableCount) {
                        sDelta += slop;
                    }
                    springDelta.delta = Math.min(sDelta, springDelta.delta);
                    delta -= springDelta.delta;
                    if (springDelta.delta != sDelta && counter + 1 < resizableCount) {
                        // Spring didn't take all the space, reset how much
                        // each spring will get.
                        int left = resizableCount - counter - 1;
                        sDelta = delta / left;
                        slop = delta - sDelta * left;
                    }
                    sizes[springDelta.index] = sign * springDelta.delta;
                }
                // And finally set the size of each spring
                for (int counter = 0; counter < springCount; counter++) {
                    Spring spring = getSpring(counter);
                    int sSize = spring.getPreferredSize(axis) + sizes[counter];
                    spring.setSize(axis, origin, sSize);
                    origin += sSize;
                }
            } else {
                // Nothing resizable, use the min or max of each of the
                // springs.
                for (int counter = 0; counter < springCount; counter++) {
                    Spring spring = getSpring(counter);
                    int sSize;
                    if (useMin) {
                        sSize = spring.getMinimumSize(axis);
                    } else {
                        sSize = spring.getMaximumSize(axis);
                    }
                    spring.setSize(axis, origin, sSize);
                    origin += sSize;
                }
            }
        }

        /// Returns the sorted list of SpringDelta's for the current set of
        /// Springs. The list is ordered by how much each can give, least
        /// first, and springs that give equally keep their order.
        private List<SpringDelta> buildResizableList(int axis, boolean useMin) {
            // First pass, figure out what is resizable
            int size = springs.size();
            List<SpringDelta> sorted = new ArrayList<SpringDelta>(size);
            for (int counter = 0; counter < size; counter++) {
                Spring spring = getSpring(counter);
                int sDelta;
                if (useMin) {
                    sDelta = spring.getPreferredSize(axis) - spring.getMinimumSize(axis);
                } else {
                    sDelta = spring.getMaximumSize(axis) - spring.getPreferredSize(axis);
                }
                if (sDelta > 0) {
                    sorted.add(new SpringDelta(counter, sDelta));
                }
            }
            // A stable insertion sort; the list is short.
            for (int i = 1; i < sorted.size(); i++) {
                SpringDelta moving = sorted.get(i);
                int j = i - 1;
                while (j >= 0 && sorted.get(j).delta > moving.delta) {
                    sorted.set(j + 1, sorted.get(j));
                    j--;
                }
                sorted.set(j + 1, moving);
            }
            return sorted;
        }

        private int indexOfNextNonZeroSpring(int index, boolean treatAutopaddingAsZeroSized) {
            while (index < springs.size()) {
                Spring spring = springs.get(index);
                if (!spring.willHaveZeroSize(treatAutopaddingAsZeroSized)) {
                    return index;
                }
                index++;
            }
            return index;
        }

        @Override
        void insertAutopadding(int axis, List<AutoPreferredGapSpring> leadingPadding,
                List<AutoPreferredGapSpring> trailingPadding, List<ComponentSpring> leading,
                List<ComponentSpring> trailing, boolean insert) {
            List<AutoPreferredGapSpring> newLeadingPadding = new ArrayList<AutoPreferredGapSpring>(leadingPadding);
            List<AutoPreferredGapSpring> newTrailingPadding = new ArrayList<AutoPreferredGapSpring>(1);
            List<ComponentSpring> newLeading = new ArrayList<ComponentSpring>(leading);
            List<ComponentSpring> newTrailing = null;
            int counter = 0;
            // Warning, this must use springs.size, as it may change during the
            // loop.
            while (counter < springs.size()) {
                Spring spring = getSpring(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    if (newLeadingPadding.isEmpty()) {
                        // Autopadding spring. Set the sources of the
                        // autopadding spring based on newLeading.
                        AutoPreferredGapSpring padding = (AutoPreferredGapSpring) spring;
                        padding.setSources(newLeading);
                        newLeading.clear();
                        counter = indexOfNextNonZeroSpring(counter + 1, true);
                        if (counter == springs.size()) {
                            // Last spring in the list, add it to
                            // trailingPadding.
                            if (!(padding instanceof ContainerAutoPreferredGapSpring)) {
                                trailingPadding.add(padding);
                            }
                        } else {
                            newLeadingPadding.clear();
                            newLeadingPadding.add(padding);
                        }
                    } else {
                        counter = indexOfNextNonZeroSpring(counter + 1, true);
                    }
                } else {
                    // Not a padding spring
                    if (!newLeading.isEmpty() && insert) {
                        // There's leading ComponentSprings, create an
                        // autopadding spring.
                        AutoPreferredGapSpring padding = new AutoPreferredGapSpring();
                        // Force the newly created spring to be considered
                        // by NOT incrementing counter
                        springs.add(counter, padding);
                        continue;
                    }
                    if (spring instanceof ComponentSpring) {
                        // Spring is a Component, make it the target of any
                        // leading AutopaddingSpring.
                        ComponentSpring cSpring = (ComponentSpring) spring;
                        if (!cSpring.isVisible()) {
                            counter++;
                            continue;
                        }
                        for (int i = 0; i < newLeadingPadding.size(); i++) {
                            newLeadingPadding.get(i).addTarget(cSpring, axis);
                        }
                        newLeading.clear();
                        newLeadingPadding.clear();
                        counter = indexOfNextNonZeroSpring(counter + 1, false);
                        if (counter == springs.size()) {
                            // Last Spring, add it to trailing
                            trailing.add(cSpring);
                        } else {
                            // Not that last Spring, add it to leading
                            newLeading.add(cSpring);
                        }
                    } else if (spring instanceof Group) {
                        // Forward call to child Group
                        if (newTrailing == null) {
                            newTrailing = new ArrayList<ComponentSpring>(1);
                        } else {
                            newTrailing.clear();
                        }
                        newTrailingPadding.clear();
                        ((Group) spring).insertAutopadding(axis, newLeadingPadding, newTrailingPadding, newLeading,
                                newTrailing, insert);
                        newLeading.clear();
                        newLeadingPadding.clear();
                        counter = indexOfNextNonZeroSpring(counter + 1, newTrailing.isEmpty());
                        if (counter == springs.size()) {
                            trailing.addAll(newTrailing);
                            trailingPadding.addAll(newTrailingPadding);
                        } else {
                            newLeading.addAll(newTrailing);
                            newLeadingPadding.addAll(newTrailingPadding);
                        }
                    } else {
                        // Gap
                        newLeadingPadding.clear();
                        newLeading.clear();
                        counter++;
                    }
                }
            }
        }

        @Override
        int getBaseline() {
            if (baselineSpring != null) {
                int baseline = baselineSpring.getBaseline();
                if (baseline >= 0) {
                    int size = 0;
                    for (int i = 0, max = springs.size(); i < max; i++) {
                        Spring spring = getSpring(i);
                        if (spring == baselineSpring) {
                            return size + baseline;
                        } else {
                            size += spring.getPreferredSize(VERTICAL);
                        }
                    }
                }
            }
            return -1;
        }

        @Override
        int getBaselineResizeBehavior() {
            if (isResizable(VERTICAL)) {
                if (baselineSpring != null && !baselineSpring.isResizable(VERTICAL)) {
                    // Spring to use for baseline isn't resizable. In this case
                    // baseline resize behavior can be determined based on how
                    // preceding springs resize.
                    boolean leadingResizable = false;
                    for (int i = 0, max = springs.size(); i < max; i++) {
                        Spring spring = getSpring(i);
                        if (spring == baselineSpring) {
                            break;
                        } else if (spring.isResizable(VERTICAL)) {
                            leadingResizable = true;
                            break;
                        }
                    }
                    boolean trailingResizable = false;
                    for (int i = springs.size() - 1; i >= 0; i--) {
                        Spring spring = getSpring(i);
                        if (spring == baselineSpring) {
                            break;
                        }
                        if (spring.isResizable(VERTICAL)) {
                            trailingResizable = true;
                            break;
                        }
                    }
                    if (leadingResizable && !trailingResizable) {
                        return BRB_CONSTANT_DESCENT;
                    } else if (!leadingResizable && trailingResizable) {
                        return BRB_CONSTANT_ASCENT;
                    }
                    // If we get here, both leading and trailing springs are
                    // resizable. Fall through to OTHER.
                } else {
                    int brb = BRB_NONE;
                    if (baselineSpring != null) {
                        brb = baselineSpring.getBaselineResizeBehavior();
                    }
                    if (brb == BRB_CONSTANT_ASCENT) {
                        for (int i = 0, max = springs.size(); i < max; i++) {
                            Spring spring = getSpring(i);
                            if (spring == baselineSpring) {
                                return BRB_CONSTANT_ASCENT;
                            }
                            if (spring.isResizable(VERTICAL)) {
                                return BRB_OTHER;
                            }
                        }
                    } else if (brb == BRB_CONSTANT_DESCENT) {
                        for (int i = springs.size() - 1; i >= 0; i--) {
                            Spring spring = getSpring(i);
                            if (spring == baselineSpring) {
                                return BRB_CONSTANT_DESCENT;
                            }
                            if (spring.isResizable(VERTICAL)) {
                                return BRB_OTHER;
                            }
                        }
                    }
                }
                return BRB_OTHER;
            }
            // Not resizable, treat as constant_ascent
            return BRB_CONSTANT_ASCENT;
        }
    }

    /// A `Group` that aligns and sizes its children over the same span.
    /// Created by `createParallelGroup` and `createBaselineGroup`.
    public class ParallelGroup extends Group {
        // How children are layed out.
        private final Alignment childAlignment;
        // Whether or not we're resizable.
        private final boolean resizable;

        ParallelGroup(Alignment childAlignment, boolean resizable) {
            this.childAlignment = childAlignment;
            this.resizable = resizable;
        }

        @Override
        public ParallelGroup addGroup(Group group) {
            super.addGroup(group);
            return this;
        }

        @Override
        public ParallelGroup addComponent(Component component) {
            super.addComponent(component);
            return this;
        }

        @Override
        public ParallelGroup addComponent(Component component, int min, int pref, int max) {
            super.addComponent(component, min, pref, max);
            return this;
        }

        @Override
        public ParallelGroup addGap(int pref) {
            super.addGap(pref);
            return this;
        }

        @Override
        public ParallelGroup addGap(int min, int pref, int max) {
            super.addGap(min, pref, max);
            return this;
        }

        /// Adds a `Group` to this `ParallelGroup` with an alignment of
        /// its own.
        public ParallelGroup addGroup(Alignment alignment, Group group) {
            checkChildAlignment(alignment);
            group.setAlignment(alignment);
            addSpring(group);
            return this;
        }

        /// Adds a `Component` to this `ParallelGroup` with an alignment
        /// of its own.
        public ParallelGroup addComponent(Component component, Alignment alignment) {
            return addComponent(component, alignment, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /// Adds a `Component` to this `ParallelGroup` with an alignment
        /// and sizes of its own.
        public ParallelGroup addComponent(Component component, Alignment alignment, int min, int pref, int max) {
            checkChildAlignment(alignment);
            ComponentSpring spring = new ComponentSpring(component, min, pref, max);
            spring.setAlignment(alignment);
            addSpring(spring);
            return this;
        }

        boolean isResizable() {
            return resizable;
        }

        @Override
        int operator(int a, int b) {
            return Math.max(a, b);
        }

        @Override
        int calculateMinimumSize(int axis) {
            if (!isResizable()) {
                return getPreferredSize(axis);
            }
            return super.calculateMinimumSize(axis);
        }

        @Override
        int calculateMaximumSize(int axis) {
            if (!isResizable()) {
                return getPreferredSize(axis);
            }
            return super.calculateMaximumSize(axis);
        }

        @Override
        void setValidSize(int axis, int origin, int size) {
            for (int i = 0, max = springs.size(); i < max; i++) {
                setChildSize(getSpring(i), axis, origin, size);
            }
        }

        void setChildSize(Spring spring, int axis, int origin, int size) {
            Alignment alignment = spring.getAlignment();
            int springSize = Math.min(Math.max(spring.getMinimumSize(axis), size), spring.getMaximumSize(axis));
            if (alignment == null) {
                alignment = childAlignment;
            }
            if (alignment == Alignment.TRAILING) {
                spring.setSize(axis, origin + size - springSize, springSize);
            } else if (alignment == Alignment.CENTER) {
                spring.setSize(axis, origin + (size - springSize) / 2, springSize);
            } else {
                // LEADING, or BASELINE
                spring.setSize(axis, origin, springSize);
            }
        }

        @Override
        void insertAutopadding(int axis, List<AutoPreferredGapSpring> leadingPadding,
                List<AutoPreferredGapSpring> trailingPadding, List<ComponentSpring> leading,
                List<ComponentSpring> trailing, boolean insert) {
            for (int counter = 0, max = springs.size(); counter < max; counter++) {
                Spring spring = getSpring(counter);
                if (spring instanceof ComponentSpring) {
                    ComponentSpring cSpring = (ComponentSpring) spring;
                    if (cSpring.isVisible()) {
                        for (int i = 0; i < leadingPadding.size(); i++) {
                            leadingPadding.get(i).addTarget(cSpring, axis);
                        }
                        trailing.add(cSpring);
                    }
                } else if (spring instanceof Group) {
                    ((Group) spring).insertAutopadding(axis, leadingPadding, trailingPadding, leading, trailing,
                            insert);
                } else if (spring instanceof AutoPreferredGapSpring) {
                    AutoPreferredGapSpring padding = (AutoPreferredGapSpring) spring;
                    padding.setSources(leading);
                    trailingPadding.add(padding);
                }
            }
        }

        private void checkChildAlignment(Alignment alignment) {
            boolean allowsBaseline = this instanceof BaselineGroup;
            if (alignment == null) {
                throw new IllegalArgumentException("Alignment must be non-null");
            }
            if (!allowsBaseline && alignment == Alignment.BASELINE) {
                throw new IllegalArgumentException("Alignment must be one of:LEADING, TRAILING or CENTER");
            }
        }
    }

    /// An extension of `ParallelGroup` that aligns its
    /// constituent `Spring`s along the baseline.
    private class BaselineGroup extends ParallelGroup {
        // Whether or not all child springs have a baseline
        private boolean allSpringsHaveBaseline;
        // max(spring.getBaseline()) of all springs aligned along the baseline
        // that have a baseline
        private int prefAscent;
        // max(spring.getPreferredSize().height - spring.getBaseline()) of all
        // springs aligned along the baseline that have a baseline
        private int prefDescent;
        // Whether baselineAnchoredToTop was explicitly set
        private boolean baselineAnchorSet;
        // Whether the baseline is anchored to the top or the bottom.
        // If anchored to the top the baseline is always at prefAscent,
        // otherwise the baseline is at (height - prefDescent)
        private boolean baselineAnchoredToTop;
        // Whether or not the baseline has been calculated.
        private boolean calcedBaseline;

        BaselineGroup(boolean resizable) {
            super(Alignment.LEADING, resizable);
            prefAscent = prefDescent = -1;
            calcedBaseline = false;
        }

        BaselineGroup(boolean resizable, boolean baselineAnchoredToTop) {
            this(resizable);
            this.baselineAnchoredToTop = baselineAnchoredToTop;
            baselineAnchorSet = true;
        }

        @Override
        void unset() {
            super.unset();
            prefAscent = prefDescent = -1;
            calcedBaseline = false;
        }

        @Override
        void setValidSize(int axis, int origin, int size) {
            checkAxis(axis);
            if (prefAscent == -1) {
                super.setValidSize(axis, origin, size);
            } else {
                // do baseline layout
                baselineLayout(origin, size);
            }
        }

        @Override
        int calculateSize(int axis, int type) {
            checkAxis(axis);
            if (!calcedBaseline) {
                calculateBaselineAndResizeBehavior();
            }
            if (type == MIN_SIZE) {
                return calculateMinSize();
            }
            if (type == MAX_SIZE) {
                return calculateMaxSize();
            }
            if (allSpringsHaveBaseline) {
                return prefAscent + prefDescent;
            }
            return Math.max(prefAscent + prefDescent, super.calculateSize(axis, type));
        }

        private boolean alignsOnBaseline(Spring spring) {
            return spring.getAlignment() == null || spring.getAlignment() == Alignment.BASELINE;
        }

        private void calculateBaselineAndResizeBehavior() {
            // calculate baseline
            prefAscent = 0;
            prefDescent = 0;
            int baselineSpringCount = 0;
            int resizeBehavior = BRB_NONE;
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = getSpring(counter);
                if (alignsOnBaseline(spring)) {
                    int baseline = spring.getBaseline();
                    if (baseline >= 0) {
                        if (spring.isResizable(VERTICAL)) {
                            int brb = spring.getBaselineResizeBehavior();
                            if (resizeBehavior == BRB_NONE) {
                                resizeBehavior = brb;
                            } else if (brb != resizeBehavior) {
                                resizeBehavior = BRB_CONSTANT_ASCENT;
                            }
                        }
                        prefAscent = Math.max(prefAscent, baseline);
                        prefDescent = Math.max(prefDescent, spring.getPreferredSize(VERTICAL) - baseline);
                        baselineSpringCount++;
                    }
                }
            }
            if (!baselineAnchorSet) {
                this.baselineAnchoredToTop = resizeBehavior != BRB_CONSTANT_DESCENT;
            }
            allSpringsHaveBaseline = baselineSpringCount == springs.size();
            calcedBaseline = true;
        }

        private int calculateMaxSize() {
            int maxAscent = prefAscent;
            int maxDescent = prefDescent;
            int nonBaselineMax = 0;
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = getSpring(counter);
                int springMax = spring.getMaximumSize(VERTICAL);
                int baseline = alignsOnBaseline(spring) ? spring.getBaseline() : -1;
                if (baseline >= 0) {
                    int springPref = spring.getPreferredSize(VERTICAL);
                    if (springPref != springMax) {
                        int brb = spring.getBaselineResizeBehavior();
                        if (brb == BRB_CONSTANT_ASCENT) {
                            if (baselineAnchoredToTop) {
                                maxDescent = Math.max(maxDescent, springMax - baseline);
                            }
                        } else if (brb == BRB_CONSTANT_DESCENT) {
                            if (!baselineAnchoredToTop) {
                                maxAscent = Math.max(maxAscent, springMax - springPref + baseline);
                            }
                        }
                        // CENTER_OFFSET and OTHER, not resizable
                    }
                } else {
                    // Not aligned along the baseline, or no baseline.
                    nonBaselineMax = Math.max(nonBaselineMax, springMax);
                }
            }
            return Math.max(nonBaselineMax, maxAscent + maxDescent);
        }

        private int calculateMinSize() {
            int minAscent = 0;
            int minDescent = 0;
            int nonBaselineMin = 0;
            if (baselineAnchoredToTop) {
                minAscent = prefAscent;
            } else {
                minDescent = prefDescent;
            }
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = getSpring(counter);
                int springMin = spring.getMinimumSize(VERTICAL);
                int baseline = alignsOnBaseline(spring) ? spring.getBaseline() : -1;
                if (baseline >= 0) {
                    int springPref = spring.getPreferredSize(VERTICAL);
                    int brb = spring.getBaselineResizeBehavior();
                    if (brb == BRB_CONSTANT_ASCENT) {
                        if (baselineAnchoredToTop) {
                            minDescent = Math.max(springMin - baseline, minDescent);
                        } else {
                            minAscent = Math.max(baseline, minAscent);
                        }
                    } else if (brb == BRB_CONSTANT_DESCENT) {
                        if (!baselineAnchoredToTop) {
                            minAscent = Math.max(baseline - (springPref - springMin), minAscent);
                        } else {
                            minDescent = Math.max(springPref - baseline, minDescent);
                        }
                    } else {
                        // CENTER_OFFSET and OTHER are !resizable, use
                        // the preferred size.
                        minAscent = Math.max(baseline, minAscent);
                        minDescent = Math.max(springPref - baseline, minDescent);
                    }
                } else {
                    // Not aligned along the baseline, or no baseline.
                    nonBaselineMin = Math.max(nonBaselineMin, springMin);
                }
            }
            return Math.max(nonBaselineMin, minAscent + minDescent);
        }

        /// Lays out springs that have a baseline along the baseline.  All
        /// others are centered.
        private void baselineLayout(int origin, int size) {
            int ascent;
            int descent;
            if (baselineAnchoredToTop) {
                ascent = prefAscent;
                descent = size - ascent;
            } else {
                ascent = size - prefDescent;
                descent = prefDescent;
            }
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = getSpring(counter);
                int baseline = alignsOnBaseline(spring) ? spring.getBaseline() : -1;
                if (baseline >= 0) {
                    int springMax = spring.getMaximumSize(VERTICAL);
                    int springPref = spring.getPreferredSize(VERTICAL);
                    int height = springPref;
                    int y;
                    int brb = spring.getBaselineResizeBehavior();
                    if (brb == BRB_CONSTANT_ASCENT) {
                        y = origin + ascent - baseline;
                        height = Math.min(descent, springMax - baseline) + baseline;
                    } else if (brb == BRB_CONSTANT_DESCENT) {
                        height = Math.min(ascent, springMax - springPref + baseline) + (springPref - baseline);
                        y = origin + ascent + (springPref - baseline) - height;
                    } else {
                        // CENTER_OFFSET & OTHER, not resizable
                        y = origin + ascent - baseline;
                    }
                    spring.setSize(VERTICAL, y, height);
                } else {
                    setChildSize(spring, VERTICAL, origin, size);
                }
            }
        }

        @Override
        int getBaseline() {
            if (springs.size() > 1) {
                // Force the baseline to be calculated
                getPreferredSize(VERTICAL);
                return prefAscent;
            } else if (springs.size() == 1) {
                return getSpring(0).getBaseline();
            }
            return -1;
        }

        @Override
        int getBaselineResizeBehavior() {
            if (springs.size() == 1) {
                return getSpring(0).getBaselineResizeBehavior();
            }
            if (baselineAnchoredToTop) {
                return BRB_CONSTANT_ASCENT;
            }
            return BRB_CONSTANT_DESCENT;
        }

        // If the axis is HORIZONTAL, throws an IllegalStateException
        private void checkAxis(int axis) {
            if (axis == HORIZONTAL) {
                throw new IllegalStateException("Baseline must be used along vertical axis");
            }
        }
    }

    private final class ComponentSpring extends Spring {
        // min/pref/max are either a value >= 0 or one of
        // DEFAULT_SIZE or PREFERRED_SIZE
        private final int min;
        private final int pref;
        private final int max;
        private Component component;
        private int origin;
        // Baseline for the component, computed as necessary.
        private int baseline = -1;
        // Whether or not the size has been requested yet.
        private boolean installed;

        ComponentSpring(Component component, int min, int pref, int max) {
            this.component = component;
            if (component == null) {
                throw new IllegalArgumentException("Component must be non-null");
            }
            checkSize(min, pref, max, true);
            this.min = min;
            this.max = max;
            this.pref = pref;
            // getComponentInfo makes sure component is a child of the
            // Container GroupLayout is the LayoutManager for.
            getComponentInfo(component);
        }

        @Override
        int calculateMinimumSize(int axis) {
            if (isLinked(axis)) {
                return getLinkSize(axis);
            }
            return calculateNonlinkedMinimumSize(axis);
        }

        @Override
        int calculatePreferredSize(int axis) {
            if (isLinked(axis)) {
                return getLinkSize(axis);
            }
            int min = getMinimumSize(axis);
            int pref = calculateNonlinkedPreferredSize(axis);
            int max = getMaximumSize(axis);
            return Math.min(max, Math.max(min, pref));
        }

        @Override
        int calculateMaximumSize(int axis) {
            if (isLinked(axis)) {
                return getLinkSize(axis);
            }
            return Math.max(getMinimumSize(axis), calculateNonlinkedMaximumSize(axis));
        }

        boolean isVisible() {
            return getComponentInfo(getComponent()).isVisible();
        }

        int calculateNonlinkedMinimumSize(int axis) {
            if (!isVisible()) {
                return 0;
            }
            if (min >= 0) {
                return min;
            }
            if (min == PREFERRED_SIZE) {
                return calculateNonlinkedPreferredSize(axis);
            }
            return getSizeAlongAxis(axis, component.getMinimumSize());
        }

        int calculateNonlinkedPreferredSize(int axis) {
            if (!isVisible()) {
                return 0;
            }
            if (pref >= 0) {
                return pref;
            }
            return getSizeAlongAxis(axis, component.getPreferredSize());
        }

        int calculateNonlinkedMaximumSize(int axis) {
            if (!isVisible()) {
                return 0;
            }
            if (max >= 0) {
                return max;
            }
            if (max == PREFERRED_SIZE) {
                return calculateNonlinkedPreferredSize(axis);
            }
            return getSizeAlongAxis(axis, component.getMaximumSize());
        }

        private int getSizeAlongAxis(int axis, Dimension size) {
            return (axis == HORIZONTAL) ? size.width : size.height;
        }

        private int getLinkSize(int axis) {
            if (!isVisible()) {
                return 0;
            }
            return getComponentInfo(component).getLinkSize(axis);
        }

        @Override
        void setSize(int axis, int origin, int size) {
            super.setSize(axis, origin, size);
            this.origin = origin;
            if (size == UNSET) {
                baseline = -1;
            }
        }

        int getOrigin() {
            return origin;
        }

        Component getComponent() {
            return component;
        }

        void setComponent(Component component) {
            this.component = component;
        }

        @Override
        int getBaseline() {
            if (baseline == -1) {
                Spring horizontalSpring = getComponentInfo(component).horizontalSpring;
                int width = horizontalSpring.getPreferredSize(HORIZONTAL);
                int height = getPreferredSize(VERTICAL);
                if (width > 0 && height > 0) {
                    baseline = component.getBaseline(width, height);
                }
            }
            return baseline;
        }

        private boolean isLinked(int axis) {
            return getComponentInfo(component).isLinked(axis);
        }

        void installIfNecessary(int axis) {
            if (!installed) {
                installed = true;
                if (axis == HORIZONTAL) {
                    getComponentInfo(component).horizontalSpring = this;
                } else {
                    getComponentInfo(component).verticalSpring = this;
                }
            }
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return !isVisible();
        }
    }

    /// Spring representing the preferred distance between two components.
    private final class PreferredGapSpring extends Spring {
        private final JComponent source;
        private final JComponent target;
        private final LayoutStyle.ComponentPlacement type;
        private final int pref;
        private final int max;

        PreferredGapSpring(JComponent source, JComponent target, LayoutStyle.ComponentPlacement type, int pref,
                int max) {
            this.source = source;
            this.target = target;
            this.type = type;
            this.pref = pref;
            this.max = max;
        }

        @Override
        int calculateMinimumSize(int axis) {
            return getPadding(axis);
        }

        @Override
        int calculatePreferredSize(int axis) {
            if (pref == DEFAULT_SIZE || pref == PREFERRED_SIZE) {
                return getMinimumSize(axis);
            }
            int min = getMinimumSize(axis);
            int max = getMaximumSize(axis);
            return Math.min(max, Math.max(min, pref));
        }

        @Override
        int calculateMaximumSize(int axis) {
            if (max == PREFERRED_SIZE || max == DEFAULT_SIZE) {
                return getPadding(axis);
            }
            return Math.max(getMinimumSize(axis), max);
        }

        private int getPadding(int axis) {
            int position;
            if (axis == HORIZONTAL) {
                position = SwingConstants.EAST;
            } else {
                position = SwingConstants.SOUTH;
            }
            return getLayoutStyle0().getPreferredGap(source, target, type, position, host);
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return false;
        }
    }

    /// Spring represented a certain amount of space.
    private final class GapSpring extends Spring {
        private final int min;
        private final int pref;
        private final int max;

        GapSpring(int min, int pref, int max) {
            checkSize(min, pref, max, false);
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        @Override
        int calculateMinimumSize(int axis) {
            if (min == PREFERRED_SIZE) {
                return getPreferredSize(axis);
            }
            return min;
        }

        @Override
        int calculatePreferredSize(int axis) {
            return pref;
        }

        @Override
        int calculateMaximumSize(int axis) {
            if (max == PREFERRED_SIZE) {
                return getPreferredSize(axis);
            }
            return max;
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return false;
        }
    }

    /// Spring reprensenting the distance between any number of sources and
    /// targets.  The targets and sources are computed during layout.  An
    /// instance of this can either be dynamically created when
    /// autocreatePadding is true, or explicitly created by the developer.
    private class AutoPreferredGapSpring extends Spring {
        private final int pref;
        private final int max;
        List<ComponentSpring> sources;
        int size;
        int lastSize;
        private List<AutoPreferredGapMatch> matches;
        private LayoutStyle.ComponentPlacement type;
        private boolean userCreated;

        AutoPreferredGapSpring() {
            this.pref = PREFERRED_SIZE;
            this.max = PREFERRED_SIZE;
            this.type = LayoutStyle.ComponentPlacement.RELATED;
        }

        AutoPreferredGapSpring(int pref, int max) {
            this.pref = pref;
            this.max = max;
        }

        AutoPreferredGapSpring(LayoutStyle.ComponentPlacement type, int pref, int max) {
            this.type = type;
            this.pref = pref;
            this.max = max;
            this.userCreated = true;
        }

        void setSources(List<ComponentSpring> sources) {
            this.sources = new ArrayList<ComponentSpring>(sources);
        }

        boolean getUserCreated() {
            return userCreated;
        }

        void setUserCreated(boolean userCreated) {
            this.userCreated = userCreated;
        }

        @Override
        void unset() {
            lastSize = getSize();
            super.unset();
            size = 0;
        }

        void reset() {
            size = 0;
            sources = null;
            matches = null;
        }

        void calculatePadding(int axis) {
            size = UNSET;
            int maxPadding = UNSET;
            if (matches != null) {
                LayoutStyle p = getLayoutStyle0();
                int position;
                if (axis == HORIZONTAL) {
                    position = SwingConstants.EAST;
                } else {
                    position = SwingConstants.SOUTH;
                }
                for (int i = matches.size() - 1; i >= 0; i--) {
                    AutoPreferredGapMatch match = matches.get(i);
                    maxPadding = Math.max(maxPadding, calculatePadding(p, position, match.source, match.target));
                }
            }
            if (size == UNSET) {
                size = 0;
            }
            if (maxPadding == UNSET) {
                maxPadding = 0;
            }
            if (lastSize != UNSET) {
                size += Math.min(maxPadding, lastSize);
            }
        }

        private int calculatePadding(LayoutStyle p, int position, ComponentSpring source, ComponentSpring target) {
            int delta = target.getOrigin() - (source.getOrigin() + source.getSize());
            if (delta >= 0) {
                int padding;
                Component a = source.getComponent();
                Component b = target.getComponent();
                if (a instanceof JComponent && b instanceof JComponent) {
                    padding = p.getPreferredGap((JComponent) a, (JComponent) b, type, position, host);
                } else {
                    padding = PLAIN_COMPONENT_GAP;
                }
                if (padding > delta) {
                    size = Math.max(size, padding - delta);
                }
                return padding;
            }
            return 0;
        }

        void addTarget(ComponentSpring spring, int axis) {
            int oAxis = (axis == HORIZONTAL) ? VERTICAL : HORIZONTAL;
            List<ComponentSpring> from = sources;
            if (from == null) {
                return;
            }
            Component component = spring.getComponent();
            for (int counter = from.size() - 1; counter >= 0; counter--) {
                ComponentSpring candidate = from.get(counter);
                if (areParallelSiblings(candidate.getComponent(), component, oAxis)) {
                    addValidTarget(candidate, spring);
                }
            }
        }

        private void addValidTarget(ComponentSpring source, ComponentSpring target) {
            if (matches == null) {
                matches = new ArrayList<AutoPreferredGapMatch>(1);
            }
            matches.add(new AutoPreferredGapMatch(source, target));
        }

        @Override
        int calculateMinimumSize(int axis) {
            return size;
        }

        @Override
        int calculatePreferredSize(int axis) {
            if (pref == PREFERRED_SIZE || pref == DEFAULT_SIZE) {
                return size;
            }
            return Math.max(size, pref);
        }

        @Override
        int calculateMaximumSize(int axis) {
            if (max >= 0) {
                return Math.max(getPreferredSize(axis), max);
            }
            return size;
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return treatAutopaddingAsZeroSized;
        }
    }

    /// An extension of AutoPreferredGapSpring used for container level
    /// padding.
    private final class ContainerAutoPreferredGapSpring extends AutoPreferredGapSpring {
        private List<ComponentSpring> targets;

        ContainerAutoPreferredGapSpring() {
            super();
            setUserCreated(true);
        }

        ContainerAutoPreferredGapSpring(int pref, int max) {
            super(pref, max);
            setUserCreated(true);
        }

        @Override
        void addTarget(ComponentSpring spring, int axis) {
            if (targets == null) {
                targets = new ArrayList<ComponentSpring>(1);
            }
            targets.add(spring);
        }

        @Override
        void calculatePadding(int axis) {
            LayoutStyle p = getLayoutStyle0();
            int maxPadding = 0;
            int position;
            size = 0;
            if (targets != null) {
                // Leading. The vertical axis asks for the SOUTH gap at
                // the top edge too, as the JDK does.
                if (axis == HORIZONTAL) {
                    position = SwingConstants.WEST;
                } else {
                    position = SwingConstants.SOUTH;
                }
                for (int i = targets.size() - 1; i >= 0; i--) {
                    ComponentSpring targetSpring = targets.get(i);
                    int padding = PLAIN_COMPONENT_GAP;
                    Component c = targetSpring.getComponent();
                    if (c instanceof JComponent) {
                        padding = p.getContainerGap((JComponent) c, position, host);
                        maxPadding = Math.max(padding, maxPadding);
                        padding -= targetSpring.getOrigin();
                    } else {
                        maxPadding = Math.max(padding, maxPadding);
                    }
                    size = Math.max(size, padding);
                }
            } else {
                // Trailing
                if (axis == HORIZONTAL) {
                    position = SwingConstants.EAST;
                } else {
                    position = SwingConstants.SOUTH;
                }
                if (sources != null) {
                    for (int i = sources.size() - 1; i >= 0; i--) {
                        maxPadding = Math.max(maxPadding, updateSize(p, sources.get(i), position));
                    }
                }
            }
            if (lastSize != UNSET) {
                size += Math.min(maxPadding, lastSize);
            }
        }

        private int updateSize(LayoutStyle p, ComponentSpring sourceSpring, int position) {
            int padding = PLAIN_COMPONENT_GAP;
            Component c = sourceSpring.getComponent();
            if (c instanceof JComponent) {
                padding = p.getContainerGap((JComponent) c, position, host);
            }
            int delta = Math.max(0, getParent().getSize() - sourceSpring.getSize() - sourceSpring.getOrigin());
            size = Math.max(size, padding - delta);
            return padding;
        }
    }

    /// Tracks the horizontal and vertical spring of a Component, and what
    /// it is linked to.
    private final class ComponentInfo {
        ComponentSpring horizontalSpring;
        ComponentSpring verticalSpring;
        // Component being layed out
        private Component component;
        // If the component's size is linked to other components, the
        // horizontalMaster and/or verticalMaster reference the group of
        // linked components.
        private LinkInfo horizontalMaster;
        private LinkInfo verticalMaster;
        private boolean visible;
        private Boolean honorsVisibility;

        ComponentInfo(Component component) {
            this.component = component;
            updateVisibility();
        }

        void dispose() {
            // Remove horizontal/vertical springs
            removeSpring(horizontalSpring);
            horizontalSpring = null;
            removeSpring(verticalSpring);
            verticalSpring = null;
            // Clean up links
            if (horizontalMaster != null) {
                horizontalMaster.remove(this);
            }
            if (verticalMaster != null) {
                verticalMaster.remove(this);
            }
        }

        void setHonorsVisibility(Boolean honorsVisibility) {
            this.honorsVisibility = honorsVisibility;
        }

        private void removeSpring(Spring spring) {
            if (spring != null) {
                Spring parent = spring.getParent();
                if (parent instanceof Group) {
                    ((Group) parent).springs.remove(spring);
                }
            }
        }

        boolean isVisible() {
            return visible;
        }

        /// Updates the cached visibility.
        ///
        /// #### Returns
        ///
        /// true if the visibility changed
        boolean updateVisibility() {
            boolean honors;
            if (this.honorsVisibility == null) {
                honors = GroupLayout.this.getHonorsVisibility();
            } else {
                honors = this.honorsVisibility.booleanValue();
            }
            boolean newVisible = !honors || component.isVisible();
            if (visible != newVisible) {
                visible = newVisible;
                return true;
            }
            return false;
        }

        void setBounds(Insets insets) {
            int x = horizontalSpring.getOrigin();
            int w = horizontalSpring.getSize();
            int y = verticalSpring.getOrigin();
            int h = verticalSpring.getSize();
            component.setBounds(x + insets.left, y + insets.top, w, h);
        }

        void setComponent(Component component) {
            this.component = component;
            if (horizontalSpring != null) {
                horizontalSpring.setComponent(component);
            }
            if (verticalSpring != null) {
                verticalSpring.setComponent(component);
            }
        }

        boolean isLinked(int axis) {
            if (axis == HORIZONTAL) {
                return horizontalMaster != null;
            }
            return verticalMaster != null;
        }

        void setLinkInfo(int axis, LinkInfo linkInfo) {
            if (axis == HORIZONTAL) {
                horizontalMaster = linkInfo;
            } else {
                verticalMaster = linkInfo;
            }
        }

        LinkInfo getLinkInfo(int axis) {
            return getLinkInfo(axis, true);
        }

        LinkInfo getLinkInfo(int axis, boolean create) {
            if (axis == HORIZONTAL) {
                if (horizontalMaster == null && create) {
                    // horizontalMaster field is directly set by adding
                    // us to the LinkInfo.
                    new LinkInfo(HORIZONTAL).add(this);
                }
                return horizontalMaster;
            } else {
                if (verticalMaster == null && create) {
                    // verticalMaster field is directly set by adding
                    // us to the LinkInfo.
                    new LinkInfo(VERTICAL).add(this);
                }
                return verticalMaster;
            }
        }

        void clearCachedSize() {
            if (horizontalMaster != null) {
                horizontalMaster.clearCachedSize();
            }
            if (verticalMaster != null) {
                verticalMaster.clearCachedSize();
            }
        }

        int getLinkSize(int axis) {
            if (axis == HORIZONTAL) {
                return horizontalMaster.getSize(axis);
            } else {
                return verticalMaster.getSize(axis);
            }
        }
    }
}
