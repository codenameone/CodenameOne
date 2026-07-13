/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.accessibility;

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.table.Table;
import com.codename1.ui.util.WeakHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Builds, caches, diffs, and dispatches actions for the portable semantic tree.
///
/// The tree is always constructed on the Codename One EDT. Native ports read the
/// last immutable snapshot and post actions through {@link #performAction(long, String, Object)},
/// so native accessibility threads never synchronously enter application code.
public final class AccessibilityManager {
    public static final int CHANGE_STRUCTURE = 1;
    public static final int CHANGE_CONTENT = 2;
    public static final int CHANGE_STATE = 4;
    public static final int CHANGE_VALUE = 8;
    public static final int CHANGE_BOUNDS = 16;
    public static final int CHANGE_FOCUS = 32;
    public static final int CHANGE_ACTIONS = 64;
    public static final int CHANGE_LIVE_REGION = 128;
    public static final int CHANGE_PANE = 256;
    public static final int CHANGE_ALL = 0x7fffffff;

    private static final AccessibilityManager INSTANCE = new AccessibilityManager();
    private final Map<Component, Long> componentIds = new WeakHashMap<Component, Long>();
    private final Map<Component, Map<String, Long>> virtualIds =
            new WeakHashMap<Component, Map<String, Long>>();
    private long nextId = 1;
    private long generation;
    private boolean dirty = true;
    private boolean refreshScheduled;
    private int pendingChanges = CHANGE_ALL;
    private Form snapshotForm;
    private AccessibilityTreeSnapshot snapshot = new AccessibilityTreeSnapshot(
            0, Collections.<Long>emptyList(), Collections.<Long, AccessibilityNodeSnapshot>emptyMap());

    private AccessibilityManager() {
    }

    public static AccessibilityManager getInstance() {
        return INSTANCE;
    }

    public synchronized void invalidate(Component component, int changeType) {
        dirty = true;
        pendingChanges |= changeType;
        try {
            // Most mutations only need to make the cached snapshot stale. Ports
            // that can pull the tree do so on demand, and ports such as Android
            // opt into eager projection only while assistive technology is active.
            // This keeps layout, scrolling, and text setters at O(1) when nobody
            // is consuming the semantic tree.
            if (!Display.getInstance().isAccessibilityTreeUpdateRequired()) {
                return;
            }
            if (!refreshScheduled) {
                refreshScheduled = true;
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        int changes;
                        synchronized (AccessibilityManager.this) {
                            changes = pendingChanges;
                        }
                        getSnapshot(Display.getInstance().getCurrent());
                        synchronized (AccessibilityManager.this) {
                            refreshScheduled = false;
                        }
                        Display.getInstance().accessibilityTreeChanged(changes);
                    }
                });
            }
        } catch (Throwable ignored) {
            // Display may not be initialized yet while an application constructs its first form.
            refreshScheduled = false;
        }
    }

    public void invalidateAll() {
        invalidate(null, CHANGE_ALL);
    }

    public AccessibilityTreeSnapshot getCurrentSnapshot() {
        synchronized (this) {
            if (!Display.getInstance().isEdt()) {
                return snapshot;
            }
        }
        return getSnapshot(Display.getInstance().getCurrent());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public synchronized AccessibilityTreeSnapshot getSnapshot(Form form) {
        // Walking a live lightweight component hierarchy off the Codename One
        // EDT is unsafe. Native bridges on other threads receive the last
        // immutable snapshot; active bridges arrange eager refreshes on the EDT.
        if (!Display.getInstance().isEdt()) {
            return snapshot;
        }
        if (!dirty && form == snapshotForm) {
            return snapshot;
        }
        if (form == null) {
            snapshot = new AccessibilityTreeSnapshot(++generation, Collections.<Long>emptyList(),
                                                     Collections.<Long, AccessibilityNodeSnapshot>emptyMap());
            snapshotForm = null;
            dirty = false;
            pendingChanges = 0;
            return snapshot;
        }

        List<BuildNode> roots = new ArrayList<BuildNode>();
        resolveComponent(form, roots);
        sortTree(roots);
        List<Long> rootIds = new ArrayList<Long>();
        LinkedHashMap<Long, AccessibilityNodeSnapshot> nodes = new LinkedHashMap<Long, AccessibilityNodeSnapshot>();
        freeze(roots, -1, rootIds, nodes);
        snapshot = new AccessibilityTreeSnapshot(++generation, rootIds, nodes);
        snapshotForm = form;
        dirty = false;
        pendingChanges = 0;
        return snapshot;
    }

    public synchronized int getPendingChanges() {
        return pendingChanges;
    }

    public boolean performAction(long nodeId, String actionId, final Object argument) {
        final AccessibilityNodeSnapshot node;
        final AccessibilityAction action;
        synchronized (this) {
            node = snapshot.getNode(nodeId);
            action = node == null ? null : node.getAction(actionId);
        }
        if (node == null || action == null || !action.isEnabled()) {
            return false;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                action.perform(node.getComponent(), argument);
                invalidate(node.getComponent(), CHANGE_STATE | CHANGE_VALUE | CHANGE_CONTENT);
            }
        });
        return true;
    }

    private long idFor(Component component) {
        Long id = componentIds.get(component);
        if (id == null) {
            id = Long.valueOf(nextId++);
            componentIds.put(component, id);
        }
        return id.longValue();
    }

    private long idForVirtual(Component host, String path) {
        Map<String, Long> hostIds = virtualIds.get(host);
        if (hostIds == null) {
            hostIds = new LinkedHashMap<String, Long>();
            virtualIds.put(host, hostIds);
        }
        Long id = hostIds.get(path);
        if (id == null) {
            id = Long.valueOf(nextId++);
            hostIds.put(path, id);
        }
        return id.longValue();
    }

    private void resolveComponent(Component component, List<BuildNode> destination) {
        if (component == null || (!(component instanceof Form) && !component.isVisible()) || component.isHidden(true)) {
            return;
        }
        AccessibilityNode config = component.getSemantics();
        AccessibilityGrouping grouping = config.getGrouping();
        if (grouping == AccessibilityGrouping.EXCLUDE_SUBTREE) {
            return;
        }

        BuildNode node = buildComponentNode(component, config);
        List<BuildNode> children = new ArrayList<BuildNode>();
        if (grouping != AccessibilityGrouping.LEAF && component instanceof Container) {
            Container container = (Container) component;
            for (int i = 0; i < container.getComponentCount(); i++) {
                resolveComponent(container.getComponentAt(i), children);
            }
        }
        if (grouping != AccessibilityGrouping.LEAF) {
            addVirtualChildren(component, config, "custom", children);
            if (component instanceof com.codename1.ui.List) {
                addListChildren((com.codename1.ui.List) component, children);
            }
        }

        boolean expose = shouldExpose(component, config, node);
        if (grouping == AccessibilityGrouping.EXCLUDE) {
            expose = false;
        }
        if (grouping == AccessibilityGrouping.MERGE_DESCENDANTS) {
            String merged = collectLabels(children);
            if (isEmpty(node.builder.label)) {
                node.builder.label = merged;
            } else if (!isEmpty(merged)) {
                node.builder.description = join(node.builder.description, merged);
            }
            children.clear();
            expose = true;
        }
        if (grouping == AccessibilityGrouping.LEAF || grouping == AccessibilityGrouping.GROUP) {
            expose = true;
        }

        if (expose) {
            node.children.addAll(children);
            destination.add(node);
        } else {
            destination.addAll(children);
        }
    }

    private BuildNode buildComponentNode(final Component component, AccessibilityNode config) {
        BuildNode out = new BuildNode();
        out.id = idFor(component);
        out.builder.id = out.id;
        out.builder.component = component;
        out.builder.identifier = config.getIdentifier();
        out.builder.label = firstNonEmpty(config.getLabel(), component.getAccessibilityText());
        out.builder.hint = config.getHint();
        out.builder.description = config.getDescription();
        out.builder.value = config.getValue();
        out.builder.validationError = config.getValidationError();
        out.builder.paneTitle = config.getPaneTitle();
        out.builder.roleDescription = config.getRoleDescription();
        out.builder.role = config.getRole() == AccessibilityRole.NONE ? inferRole(component) : config.getRole();
        out.builder.checked = inferChecked(component, config);
        out.builder.liveRegion = config.getLiveRegion();
        out.builder.range = config.getRange() == null ? inferRange(component) : config.getRange();
        out.builder.collectionInfo =
                config.getCollectionInfo() == null ? inferCollection(component) : config.getCollectionInfo();
        out.builder.collectionItemInfo = config.getCollectionItemInfo() == null ? inferCollectionItem(component)
                                                                                : config.getCollectionItemInfo();
        out.builder.bounds = componentBounds(component);
        out.builder.selected = config.getSelected();
        if (out.builder.selected == null) {
            applyInferredSelected(component, out.builder);
        }
        out.builder.expanded = config.getExpanded();
        out.builder.enabled =
                config.getEnabled() == null ? Boolean.valueOf(component.isEnabled()) : config.getEnabled();
        out.builder.invalid = config.getInvalid();
        out.builder.busy = config.getBusy();
        out.builder.readOnly = config.getReadOnly();
        out.builder.required = config.getRequired();
        out.builder.multiline = config.getMultiline();
        out.builder.obscured = config.getObscured();
        applyInferredTextStates(component, out.builder);
        out.builder.pressed = config.getPressed();
        out.builder.current = config.getCurrent();
        out.builder.modal = config.isModal() || component instanceof Dialog;
        out.builder.focusable = component.isFocusable() || isInteractiveRole(out.builder.role);
        out.builder.focused = component.hasFocus();
        out.builder.headingLevel = config.getHeadingLevel();
        out.builder.sortKey = config.getSortKey();
        out.builder.traversalBefore = config.getTraversalBefore();
        out.builder.traversalAfter = config.getTraversalAfter();
        out.builder.actions.addAll(config.getActions());
        addDefaultActions(component, out.builder);
        if (component instanceof Form && isEmpty(out.builder.paneTitle)) {
            out.builder.paneTitle = ((Form) component).getTitle();
        }
        out.component = component;
        return out;
    }

    private void addVirtualChildren(Component host, AccessibilityNode config, String path,
                                    List<BuildNode> destination) {
        List<AccessibilityNode> virtual = new ArrayList<AccessibilityNode>();
        virtual.addAll(config.getChildren());
        if (config.getChildProvider() != null) {
            List<AccessibilityNode> provided = config.getChildProvider().getAccessibilityChildren(host);
            if (provided != null) {
                virtual.addAll(provided);
            }
        }
        for (int i = 0; i < virtual.size(); i++) {
            AccessibilityNode child = virtual.get(i);
            String key = child.getVirtualKey();
            if (isEmpty(key)) {
                key = "index-" + i;
            }
            BuildNode resolved = buildVirtualNode(host, child, path + "/" + key);
            addVirtualChildren(host, child, path + "/" + key, resolved.children);
            destination.add(resolved);
        }
    }

    private BuildNode buildVirtualNode(Component host, AccessibilityNode config, String path) {
        BuildNode out = new BuildNode();
        out.id = idForVirtual(host, path);
        out.builder.id = out.id;
        out.builder.component = host;
        out.builder.virtualKey = config.getVirtualKey();
        out.builder.identifier = config.getIdentifier();
        out.builder.label = config.getLabel();
        out.builder.hint = config.getHint();
        out.builder.description = config.getDescription();
        out.builder.value = config.getValue();
        out.builder.validationError = config.getValidationError();
        out.builder.paneTitle = config.getPaneTitle();
        out.builder.roleDescription = config.getRoleDescription();
        out.builder.role = config.getRole() == AccessibilityRole.NONE ? AccessibilityRole.GENERIC : config.getRole();
        out.builder.checked = config.getChecked();
        out.builder.liveRegion = config.getLiveRegion();
        out.builder.range = config.getRange();
        out.builder.collectionInfo = config.getCollectionInfo();
        out.builder.collectionItemInfo = config.getCollectionItemInfo();
        out.builder.bounds = virtualBounds(host, config.getBounds());
        out.builder.selected = config.getSelected();
        out.builder.expanded = config.getExpanded();
        out.builder.enabled = config.getEnabled() == null ? Boolean.valueOf(host.isEnabled()) : config.getEnabled();
        out.builder.invalid = config.getInvalid();
        out.builder.busy = config.getBusy();
        out.builder.readOnly = config.getReadOnly();
        out.builder.required = config.getRequired();
        out.builder.multiline = config.getMultiline();
        out.builder.obscured = config.getObscured();
        out.builder.pressed = config.getPressed();
        out.builder.current = config.getCurrent();
        out.builder.modal = config.isModal();
        out.builder.focusable = !config.getActions().isEmpty() || isInteractiveRole(out.builder.role);
        out.builder.headingLevel = config.getHeadingLevel();
        out.builder.sortKey = config.getSortKey();
        out.builder.actions.addAll(config.getActions());
        out.component = host;
        return out;
    }

    private void addListChildren(final com.codename1.ui.List list, List<BuildNode> destination) {
        int size = list.size();
        int[] visibleItems = list.getAccessibilityVisibleItemIndices();
        for (int i : visibleItems) {
            final int index = i;
            AccessibilityNode item = new AccessibilityNode("item-" + i)
                                             .setRole(AccessibilityRole.LIST_ITEM)
                                             .setLabel(list.getAccessibilityItemText(i))
                                             .setSelected(Boolean.valueOf(i == list.getSelectedIndex()))
                                             .setCollectionItemInfo(new AccessibilityCollectionItemInfo(
                                                     i, 1, 0, 1, i + 1, size, 1, false))
                                             .setBounds(list.getAccessibilityItemBounds(i, new Rectangle()))
                                             .addAction(new AccessibilityAction(AccessibilityAction.ACTIVATE, null,
                                                                                new ListActivateHandler(list, index)));
            destination.add(buildVirtualNode(list, item, "list/item-" + i));
        }
    }

    private void addDefaultActions(final Component component, AccessibilityNodeSnapshot.Builder builder) {
        if (component.isFocusable() && !hasAction(builder.actions, AccessibilityAction.FOCUS)) {
            builder.actions.add(new AccessibilityAction(AccessibilityAction.FOCUS, null, FocusHandler.INSTANCE));
        }
        if (component instanceof Button && !hasAction(builder.actions, AccessibilityAction.ACTIVATE)) {
            builder.actions.add(new AccessibilityAction(AccessibilityAction.ACTIVATE, null, ActivateHandler.INSTANCE));
        }
        if (component instanceof Slider) {
            final Slider slider = (Slider) component;
            if (slider.isEditable() && !hasAction(builder.actions, AccessibilityAction.INCREMENT)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.INCREMENT, null,
                                                            new SliderAdjustmentHandler(slider, 1)));
            }
            if (slider.isEditable() && !hasAction(builder.actions, AccessibilityAction.DECREMENT)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.DECREMENT, null,
                                                            new SliderAdjustmentHandler(slider, -1)));
            }
        }
        if (component instanceof com.codename1.ui.List) {
            final com.codename1.ui.List list = (com.codename1.ui.List) component;
            if (list.size() > 0 && !hasAction(builder.actions, AccessibilityAction.SCROLL_FORWARD)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.SCROLL_FORWARD, null,
                                                            new ListScrollHandler(list, 1)));
            }
            if (list.size() > 0 && !hasAction(builder.actions, AccessibilityAction.SCROLL_BACKWARD)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.SCROLL_BACKWARD, null,
                                                            new ListScrollHandler(list, -1)));
            }
        }
        if (component instanceof TextArea) {
            final TextArea text = (TextArea) component;
            if (!hasAction(builder.actions, AccessibilityAction.FOCUS)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.FOCUS, null, FocusHandler.INSTANCE));
            }
            if (text.isEditable() && !hasAction(builder.actions, AccessibilityAction.SET_TEXT)) {
                builder.actions.add(
                        new AccessibilityAction(AccessibilityAction.SET_TEXT, null, new SetTextHandler(text)));
            }
        }
    }

    private AccessibilityRole inferRole(Component component) {
        if (component.getParent() instanceof Table) {
            return ((Table) component.getParent()).getCellRow(component) < 0 ? AccessibilityRole.COLUMN_HEADER
                                                                             : AccessibilityRole.CELL;
        }
        Tabs tabOwner = tabPanelOwner(component);
        if (tabOwner != null) {
            return AccessibilityRole.TAB_PANEL;
        }
        if (component instanceof Dialog) {
            return AccessibilityRole.DIALOG;
        }
        if (component instanceof RadioButton) {
            return AccessibilityRole.RADIO_BUTTON;
        }
        if (component instanceof CheckBox) {
            return AccessibilityRole.CHECKBOX;
        }
        if (component instanceof Button) {
            if (isTabButton((Button) component)) {
                return AccessibilityRole.TAB;
            }
            return ((Button) component).isToggle() ? AccessibilityRole.TOGGLE_BUTTON : AccessibilityRole.BUTTON;
        }
        if (component instanceof Slider) {
            return ((Slider) component).isEditable() ? AccessibilityRole.SLIDER : AccessibilityRole.PROGRESS_BAR;
        }
        if (component instanceof TextField) {
            return AccessibilityRole.TEXT_FIELD;
        }
        if (component instanceof TextArea) {
            return AccessibilityRole.TEXT_FIELD;
        }
        if (component instanceof com.codename1.ui.List) {
            return AccessibilityRole.LIST;
        }
        if (component instanceof Table) {
            return AccessibilityRole.GRID;
        }
        if (component instanceof Tabs) {
            return AccessibilityRole.TAB_LIST;
        }
        if (component instanceof Form) {
            return AccessibilityRole.GENERIC;
        }
        if (component instanceof Label) {
            return AccessibilityRole.STATIC_TEXT;
        }
        return AccessibilityRole.NONE;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean isTabButton(Button button) {
        Container parent = button.getParent();
        while (parent != null) {
            Container owner = parent.getParent();
            if (owner instanceof Tabs && ((Tabs) owner).getTabsContainer() == parent) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private AccessibilityCheckedState inferChecked(Component component, AccessibilityNode config) {
        if (config.getChecked() != AccessibilityCheckedState.UNSPECIFIED) {
            return config.getChecked();
        }
        if (component instanceof CheckBox || component instanceof RadioButton) {
            return ((Button) component).isSelected() ? AccessibilityCheckedState.CHECKED
                                                     : AccessibilityCheckedState.UNCHECKED;
        }
        return AccessibilityCheckedState.UNSPECIFIED;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private void applyInferredSelected(Component component, AccessibilityNodeSnapshot.Builder builder) {
        if (component instanceof Button && (((Button) component).isToggle() || isTabButton((Button) component))) {
            builder.selected = Boolean.valueOf(((Button) component).isSelected());
            return;
        }
        Tabs tabOwner = tabPanelOwner(component);
        if (tabOwner != null) {
            builder.selected = Boolean.valueOf(tabOwner.getSelectedComponent() == component);
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private Tabs tabPanelOwner(Component component) {
        Container parent = component.getParent();
        if (parent != null && parent.getParent() instanceof Tabs) {
            Tabs tabs = (Tabs) parent.getParent();
            if (tabs.getContentPane() == parent) {
                return tabs;
            }
        }
        return null;
    }

    private AccessibilityRange inferRange(Component component) {
        if (component instanceof Slider) {
            Slider slider = (Slider) component;
            return new AccessibilityRange(slider.getMinValue(), slider.getMaxValue(), slider.getProgress(),
                                          slider.getIncrements(), null);
        }
        return null;
    }

    private AccessibilityCollectionInfo inferCollection(Component component) {
        if (component instanceof com.codename1.ui.List) {
            return new AccessibilityCollectionInfo(((com.codename1.ui.List) component).size(), 1, false,
                                                   AccessibilityCollectionInfo.SELECTION_SINGLE);
        }
        if (component instanceof Table) {
            Table table = (Table) component;
            return new AccessibilityCollectionInfo(table.getModel().getRowCount(), table.getModel().getColumnCount(),
                                                   false, AccessibilityCollectionInfo.SELECTION_SINGLE);
        }
        if (component instanceof Tabs) {
            return new AccessibilityCollectionInfo(1, ((Tabs) component).getTabCount(), false,
                                                   AccessibilityCollectionInfo.SELECTION_SINGLE);
        }
        return null;
    }

    private AccessibilityCollectionItemInfo inferCollectionItem(Component component) {
        if (component.getParent() instanceof Table) {
            Table table = (Table) component.getParent();
            int sourceRow = table.getCellRow(component);
            int row = sourceRow < 0 ? 0 : sourceRow + (table.isIncludeHeader() ? 1 : 0);
            int column = table.getCellColumn(component);
            return new AccessibilityCollectionItemInfo(row, 1, column, 1, column + 1, table.getModel().getColumnCount(),
                                                       1, sourceRow < 0);
        }
        return null;
    }

    private void applyInferredTextStates(Component component, AccessibilityNodeSnapshot.Builder builder) {
        if (!(component instanceof TextArea)) {
            return;
        }
        TextArea text = (TextArea) component;
        if (builder.readOnly == null) {
            builder.readOnly = Boolean.valueOf(!text.isEditable());
        }
        if (builder.multiline == null) {
            builder.multiline = Boolean.valueOf(!(component instanceof TextField) || text.getRows() > 1);
        }
        if (builder.obscured == null) {
            builder.obscured = Boolean.valueOf((text.getConstraint() & TextArea.PASSWORD) != 0);
        }
    }

    private boolean shouldExpose(Component component, AccessibilityNode config, BuildNode node) {
        if (component instanceof Form) {
            return true;
        }
        if (config.hasExplicitConfiguration()) {
            return true;
        }
        return node.builder.role != AccessibilityRole.NONE &&
                (!isEmpty(node.builder.label) || !isEmpty(node.builder.value) || node.builder.focusable ||
                 !node.builder.actions.isEmpty() || node.builder.collectionInfo != null || node.builder.range != null);
    }

    private Rectangle componentBounds(Component component) {
        Rectangle bounds = new Rectangle(component.getAbsoluteX() + component.getScrollX(),
                                         component.getAbsoluteY() + component.getScrollY(), component.getWidth(),
                                         component.getHeight());
        Container parent = component.getParent();
        while (parent != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            Rectangle clip =
                    new Rectangle(parent.getAbsoluteX() + parent.getScrollX(),
                                  parent.getAbsoluteY() + parent.getScrollY(), parent.getWidth(), parent.getHeight());
            intersect(bounds, clip);
            parent = parent.getParent();
        }
        return bounds;
    }

    private Rectangle virtualBounds(Component host, Rectangle relative) {
        if (relative == null) {
            return componentBounds(host);
        }
        Rectangle bounds = new Rectangle(host.getAbsoluteX() + host.getScrollX() + relative.getX(),
                                         host.getAbsoluteY() + host.getScrollY() + relative.getY(), relative.getWidth(),
                                         relative.getHeight());
        intersect(bounds, componentBounds(host));
        return bounds;
    }

    private void intersect(Rectangle target, Rectangle clip) {
        int x1 = Math.max(target.getX(), clip.getX());
        int y1 = Math.max(target.getY(), clip.getY());
        int x2 = Math.min(target.getX() + target.getWidth(), clip.getX() + clip.getWidth());
        int y2 = Math.min(target.getY() + target.getHeight(), clip.getY() + clip.getHeight());
        target.setBounds(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }

    private void sortTree(List<BuildNode> nodes) {
        Collections.sort(nodes, SortKeyComparator.INSTANCE);
        applyRelativeOrder(nodes);
        for (BuildNode node : nodes) {
            sortTree(node.children);
        }
    }

    private void applyRelativeOrder(List<BuildNode> nodes) {
        int remainingPasses = nodes.size();
        while (remainingPasses > 0) {
            boolean changed = false;
            for (int i = 0; i < nodes.size(); i++) {
                BuildNode node = nodes.get(i);
                int target = indexOf(nodes, node.builder.traversalBefore);
                if (target >= 0 && i > target) {
                    nodes.remove(i);
                    nodes.add(target, node);
                    changed = true;
                    break;
                }
                target = indexOf(nodes, node.builder.traversalAfter);
                if (target >= 0 && i < target) {
                    nodes.remove(i);
                    nodes.add(Math.min(target, nodes.size()), node);
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                return;
            }
            remainingPasses--;
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private int indexOf(List<BuildNode> nodes, Component component) {
        if (component == null) {
            return -1;
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).component == component) {
                return i;
            }
        }
        return -1;
    }

    private void freeze(List<BuildNode> source, long parentId, List<Long> childIds,
                        LinkedHashMap<Long, AccessibilityNodeSnapshot> nodes) {
        for (BuildNode node : source) {
            node.builder.parentId = parentId;
            for (BuildNode child : node.children) {
                node.builder.childIds.add(Long.valueOf(child.id));
            }
            AccessibilityNodeSnapshot frozen = new AccessibilityNodeSnapshot(node.builder);
            nodes.put(Long.valueOf(node.id), frozen);
            childIds.add(Long.valueOf(node.id));
            List<Long> ignored = new ArrayList<Long>();
            freeze(node.children, node.id, ignored, nodes);
        }
    }

    private String collectLabels(List<BuildNode> nodes) {
        String value = null;
        for (BuildNode node : nodes) {
            value = join(value, node.builder.label);
            value = join(value, collectLabels(node.children));
        }
        return value;
    }

    private static boolean hasAction(List<AccessibilityAction> actions, String id) {
        for (AccessibilityAction action : actions) {
            if (id.equals(action.getId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInteractiveRole(AccessibilityRole role) {
        return role == AccessibilityRole.BUTTON || role == AccessibilityRole.TOGGLE_BUTTON ||
                role == AccessibilityRole.CHECKBOX || role == AccessibilityRole.RADIO_BUTTON ||
                role == AccessibilityRole.SWITCH || role == AccessibilityRole.LINK ||
                role == AccessibilityRole.TEXT_FIELD || role == AccessibilityRole.SEARCH_FIELD ||
                role == AccessibilityRole.SLIDER || role == AccessibilityRole.TAB ||
                role == AccessibilityRole.MENU_ITEM || role == AccessibilityRole.SPIN_BUTTON ||
                role == AccessibilityRole.COMBO_BOX || role == AccessibilityRole.TREE_ITEM;
    }

    private static String firstNonEmpty(String first, String second) {
        return !isEmpty(first) ? first : second;
    }

    private static String join(String first, String second) {
        if (isEmpty(first)) {
            return second;
        }
        if (isEmpty(second)) {
            return first;
        }
        return first + ", " + second;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private static final class BuildNode {
        long id;
        Component component;
        AccessibilityNodeSnapshot.Builder builder = new AccessibilityNodeSnapshot.Builder();
        List<BuildNode> children = new ArrayList<BuildNode>();
    }

    private static final class SortKeyComparator implements Comparator<BuildNode> {
        private static final SortKeyComparator INSTANCE = new SortKeyComparator();

        @Override
        public int compare(BuildNode a, BuildNode b) {
            boolean an = Double.isNaN(a.builder.sortKey);
            boolean bn = Double.isNaN(b.builder.sortKey);
            if (an && bn) {
                return 0;
            }
            if (an) {
                return 1;
            }
            if (bn) {
                return -1;
            }
            return Double.compare(a.builder.sortKey, b.builder.sortKey);
        }
    }

    private static final class FocusHandler implements AccessibilityAction.Handler {
        private static final FocusHandler INSTANCE = new FocusHandler();

        @Override
        public boolean perform(Component source, Object argument) {
            if (!source.isEnabled()) {
                return false;
            }
            source.requestFocus();
            return true;
        }
    }

    private static final class ActivateHandler implements AccessibilityAction.Handler {
        private static final ActivateHandler INSTANCE = new ActivateHandler();

        @Override
        public boolean perform(Component source, Object argument) {
            if (!source.isEnabled()) {
                return false;
            }
            source.keyReleased(Display.getInstance().getKeyCode(Display.GAME_FIRE));
            return true;
        }
    }

    private static final class ListActivateHandler implements AccessibilityAction.Handler {
        private final com.codename1.ui.List list;
        private final int index;

        private ListActivateHandler(com.codename1.ui.List list, int index) {
            this.list = list;
            this.index = index;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            if (!list.isEnabled()) {
                return false;
            }
            list.setSelectedIndex(index);
            list.keyReleased(Display.getInstance().getKeyCode(Display.GAME_FIRE));
            return true;
        }
    }

    private static final class ListScrollHandler implements AccessibilityAction.Handler {
        private final com.codename1.ui.List list;
        private final int direction;

        private ListScrollHandler(com.codename1.ui.List list, int direction) {
            this.list = list;
            this.direction = direction;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            if (!list.isEnabled() || list.size() == 0) {
                return false;
            }
            int selected = list.getSelectedIndex();
            if (selected < 0) {
                selected = direction > 0 ? 0 : list.size() - 1;
            } else {
                selected = Math.max(0, Math.min(list.size() - 1, selected + direction));
            }
            if (selected == list.getSelectedIndex()) {
                return false;
            }
            list.setSelectedIndex(selected);
            return true;
        }
    }

    private static final class SliderAdjustmentHandler implements AccessibilityAction.Handler {
        private final Slider slider;
        private final int direction;

        private SliderAdjustmentHandler(Slider slider, int direction) {
            this.slider = slider;
            this.direction = direction;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            int increment = Math.max(1, slider.getIncrements());
            int value = slider.getProgress() + direction * increment;
            slider.setProgress(Math.max(slider.getMinValue(), Math.min(slider.getMaxValue(), value)));
            return true;
        }
    }

    private static final class SetTextHandler implements AccessibilityAction.Handler {
        private final TextArea text;

        private SetTextHandler(TextArea text) {
            this.text = text;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            if (!(argument instanceof String)) {
                return false;
            }
            text.setText((String) argument);
            return true;
        }
    }
}
