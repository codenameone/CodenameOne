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

import com.codename1.ui.Component;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Mutable semantic configuration for a lightweight component or virtual child.
///
/// Component-owned nodes are obtained with {@link Component#getSemantics()}.
/// Applications can create standalone nodes for an {@link AccessibilityChildProvider};
/// virtual nodes must have a stable {@link #setVirtualKey(java.lang.String) virtual key}.
public class AccessibilityNode {
    private Component owner;
    // Stable runtime IDs belong to the component-owned semantic node. Keeping
    // them here gives them exactly the component's lifetime without relying on
    // the weak-value UI cache, whose boxed Long values may be collected while
    // the component is still alive.
    private long internalId;
    private Map<String, Long> internalVirtualIds;
    private String virtualKey;
    private String identifier;
    private String label;
    private String hint;
    private String description;
    private String value;
    private String validationError;
    private String paneTitle;
    private String roleDescription;
    private AccessibilityRole role = AccessibilityRole.NONE;
    private AccessibilityCheckedState checked = AccessibilityCheckedState.UNSPECIFIED;
    private AccessibilityLiveRegion liveRegion = AccessibilityLiveRegion.OFF;
    private AccessibilityGrouping grouping = AccessibilityGrouping.AUTO;
    private AccessibilityRange range;
    private AccessibilityCollectionInfo collectionInfo;
    private AccessibilityCollectionItemInfo collectionItemInfo;
    private AccessibilityChildProvider childProvider;
    private Rectangle bounds;
    private Boolean selected;
    private Boolean expanded;
    private Boolean enabled;
    private Boolean invalid;
    private Boolean busy;
    private Boolean readOnly;
    private Boolean required;
    private Boolean multiline;
    private Boolean obscured;
    private Boolean pressed;
    private Boolean current;
    private boolean modal;
    private int headingLevel = -1;
    private double sortKey = Double.NaN;
    private Component traversalBefore;
    private Component traversalAfter;
    private final List<AccessibilityAction> actions = new ArrayList<AccessibilityAction>();
    private final List<AccessibilityNode> children = new ArrayList<AccessibilityNode>();

    public AccessibilityNode() {
    }

    public AccessibilityNode(String virtualKey) {
        setVirtualKey(virtualKey);
    }

    /// Internal constructor used by {@link Component}.
    public AccessibilityNode(Component owner) {
        this.owner = owner;
    }

    private AccessibilityNode changed(int type) {
        if (owner != null) {
            AccessibilityManager.getInstance().invalidate(owner, type);
        }
        return this;
    }

    public Component getOwner() {
        return owner;
    }

    long getInternalId() {
        return internalId;
    }

    void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    Long getInternalVirtualId(String path) {
        return internalVirtualIds == null ? null : internalVirtualIds.get(path);
    }

    void putInternalVirtualId(String path, long id) {
        if (internalVirtualIds == null) {
            internalVirtualIds = new LinkedHashMap<String, Long>();
        }
        internalVirtualIds.put(path, Long.valueOf(id));
    }
    public String getVirtualKey() {
        return virtualKey;
    }
    public AccessibilityNode setVirtualKey(String virtualKey) {
        if (virtualKey == null || virtualKey.length() == 0) {
            throw new IllegalArgumentException("Virtual accessibility nodes require a non-empty stable key");
        }
        this.virtualKey = virtualKey;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public String getIdentifier() {
        return identifier;
    }
    public AccessibilityNode setIdentifier(String identifier) {
        this.identifier = identifier;
        return changed(AccessibilityManager.CHANGE_CONTENT);
    }
    public String getLabel() {
        return label;
    }
    public AccessibilityNode setLabel(String label) {
        this.label = label;
        return changed(AccessibilityManager.CHANGE_CONTENT);
    }
    public String getHint() {
        return hint;
    }
    public AccessibilityNode setHint(String hint) {
        this.hint = hint;
        return changed(AccessibilityManager.CHANGE_CONTENT);
    }
    public String getDescription() {
        return description;
    }
    public AccessibilityNode setDescription(String description) {
        this.description = description;
        return changed(AccessibilityManager.CHANGE_CONTENT);
    }
    public String getValue() {
        return value;
    }
    public AccessibilityNode setValue(String value) {
        this.value = value;
        return changed(AccessibilityManager.CHANGE_VALUE);
    }
    public String getValidationError() {
        return validationError;
    }
    public AccessibilityNode setValidationError(String error) {
        this.validationError = error;
        this.invalid = error == null ? null : Boolean.TRUE;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public String getPaneTitle() {
        return paneTitle;
    }
    public AccessibilityNode setPaneTitle(String paneTitle) {
        this.paneTitle = paneTitle;
        return changed(AccessibilityManager.CHANGE_PANE);
    }
    public String getRoleDescription() {
        return roleDescription;
    }
    public AccessibilityNode setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
        return changed(AccessibilityManager.CHANGE_CONTENT);
    }
    public AccessibilityRole getRole() {
        return role;
    }
    public AccessibilityNode setRole(AccessibilityRole role) {
        this.role = role == null ? AccessibilityRole.NONE : role;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public AccessibilityCheckedState getChecked() {
        return checked;
    }
    public AccessibilityNode setChecked(AccessibilityCheckedState checked) {
        this.checked = checked == null ? AccessibilityCheckedState.UNSPECIFIED : checked;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public AccessibilityLiveRegion getLiveRegion() {
        return liveRegion;
    }
    public AccessibilityNode setLiveRegion(AccessibilityLiveRegion liveRegion) {
        this.liveRegion = liveRegion == null ? AccessibilityLiveRegion.OFF : liveRegion;
        return changed(AccessibilityManager.CHANGE_LIVE_REGION);
    }
    public AccessibilityGrouping getGrouping() {
        return grouping;
    }
    public AccessibilityNode setGrouping(AccessibilityGrouping grouping) {
        this.grouping = grouping == null ? AccessibilityGrouping.AUTO : grouping;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public AccessibilityRange getRange() {
        return range;
    }
    public AccessibilityNode setRange(AccessibilityRange range) {
        this.range = range;
        return changed(AccessibilityManager.CHANGE_VALUE);
    }
    public AccessibilityCollectionInfo getCollectionInfo() {
        return collectionInfo;
    }
    public AccessibilityNode setCollectionInfo(AccessibilityCollectionInfo info) {
        this.collectionInfo = info;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public AccessibilityCollectionItemInfo getCollectionItemInfo() {
        return collectionItemInfo;
    }
    public AccessibilityNode setCollectionItemInfo(AccessibilityCollectionItemInfo info) {
        this.collectionItemInfo = info;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public AccessibilityChildProvider getChildProvider() {
        return childProvider;
    }
    public AccessibilityNode setChildProvider(AccessibilityChildProvider provider) {
        this.childProvider = provider;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public Rectangle getBounds() {
        return bounds == null ? null : new Rectangle(bounds);
    }
    public AccessibilityNode setBounds(Rectangle bounds) {
        this.bounds = bounds == null ? null : new Rectangle(bounds);
        return changed(AccessibilityManager.CHANGE_BOUNDS);
    }
    public Boolean getSelected() {
        return selected;
    }
    public AccessibilityNode setSelected(Boolean selected) {
        this.selected = selected;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getExpanded() {
        return expanded;
    }
    public AccessibilityNode setExpanded(Boolean expanded) {
        this.expanded = expanded;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getEnabled() {
        return enabled;
    }
    public AccessibilityNode setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getInvalid() {
        return invalid;
    }
    public AccessibilityNode setInvalid(Boolean invalid) {
        this.invalid = invalid;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getBusy() {
        return busy;
    }
    public AccessibilityNode setBusy(Boolean busy) {
        this.busy = busy;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getReadOnly() {
        return readOnly;
    }
    public AccessibilityNode setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getRequired() {
        return required;
    }
    public AccessibilityNode setRequired(Boolean required) {
        this.required = required;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getMultiline() {
        return multiline;
    }
    public AccessibilityNode setMultiline(Boolean multiline) {
        this.multiline = multiline;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getObscured() {
        return obscured;
    }
    public AccessibilityNode setObscured(Boolean obscured) {
        this.obscured = obscured;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getPressed() {
        return pressed;
    }
    public AccessibilityNode setPressed(Boolean pressed) {
        this.pressed = pressed;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public Boolean getCurrent() {
        return current;
    }
    public AccessibilityNode setCurrent(Boolean current) {
        this.current = current;
        return changed(AccessibilityManager.CHANGE_STATE);
    }
    public boolean isModal() {
        return modal;
    }
    public AccessibilityNode setModal(boolean modal) {
        this.modal = modal;
        return changed(AccessibilityManager.CHANGE_PANE);
    }
    public int getHeadingLevel() {
        return headingLevel;
    }
    public AccessibilityNode setHeadingLevel(int level) {
        this.headingLevel = level;
        if (level > 0 && role == AccessibilityRole.NONE) {
            role = AccessibilityRole.HEADING;
        }
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public double getSortKey() {
        return sortKey;
    }
    public AccessibilityNode setSortKey(double sortKey) {
        this.sortKey = sortKey;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public Component getTraversalBefore() {
        return traversalBefore;
    }
    public AccessibilityNode setTraversalBefore(Component component) {
        this.traversalBefore = component;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }
    public Component getTraversalAfter() {
        return traversalAfter;
    }
    public AccessibilityNode setTraversalAfter(Component component) {
        this.traversalAfter = component;
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }

    public AccessibilityNode addAction(AccessibilityAction action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i).getId().equals(action.getId())) {
                actions.remove(i);
            }
        }
        actions.add(action);
        return changed(AccessibilityManager.CHANGE_ACTIONS);
    }

    public AccessibilityNode removeAction(String id) {
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i).getId().equals(id)) {
                actions.remove(i);
            }
        }
        return changed(AccessibilityManager.CHANGE_ACTIONS);
    }

    public List<AccessibilityAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public AccessibilityNode addChild(AccessibilityNode child) {
        if (child == null) {
            throw new NullPointerException("child");
        }
        children.add(child);
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }

    public AccessibilityNode clearChildren() {
        children.clear();
        return changed(AccessibilityManager.CHANGE_STRUCTURE);
    }

    public List<AccessibilityNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean hasExplicitConfiguration() {
        return role != AccessibilityRole.NONE || label != null || hint != null || description != null ||
                value != null || validationError != null || paneTitle != null || roleDescription != null ||
                checked != AccessibilityCheckedState.UNSPECIFIED || liveRegion != AccessibilityLiveRegion.OFF ||
                grouping != AccessibilityGrouping.AUTO || range != null || collectionInfo != null ||
                collectionItemInfo != null || childProvider != null || bounds != null || selected != null ||
                expanded != null || enabled != null || invalid != null || busy != null || readOnly != null ||
                required != null || multiline != null || obscured != null || pressed != null || current != null ||
                modal || headingLevel > 0 || !Double.isNaN(sortKey) || traversalBefore != null ||
                traversalAfter != null || !actions.isEmpty() || !children.isEmpty();
    }
}
