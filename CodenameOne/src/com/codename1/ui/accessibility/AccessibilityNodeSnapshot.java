/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui.accessibility;

import com.codename1.ui.Component;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Immutable resolved node exported to platform accessibility adapters and test tooling.
public final class AccessibilityNodeSnapshot {
    private final long id;
    private final long parentId;
    private final Component component;
    private final String virtualKey;
    private final String identifier;
    private final String label;
    private final String hint;
    private final String description;
    private final String value;
    private final String validationError;
    private final String paneTitle;
    private final String roleDescription;
    private final AccessibilityRole role;
    private final AccessibilityCheckedState checked;
    private final AccessibilityLiveRegion liveRegion;
    private final AccessibilityRange range;
    private final AccessibilityCollectionInfo collectionInfo;
    private final AccessibilityCollectionItemInfo collectionItemInfo;
    private final Rectangle bounds;
    private final Boolean selected;
    private final Boolean expanded;
    private final Boolean enabled;
    private final Boolean invalid;
    private final Boolean busy;
    private final Boolean readOnly;
    private final Boolean required;
    private final Boolean multiline;
    private final Boolean obscured;
    private final Boolean pressed;
    private final Boolean current;
    private final boolean modal;
    private final boolean focusable;
    private final boolean focused;
    private final int headingLevel;
    private final List<Long> childIds;
    private final List<AccessibilityAction> actions;

    AccessibilityNodeSnapshot(Builder b) {
        id = b.id;
        parentId = b.parentId;
        component = b.component;
        virtualKey = b.virtualKey;
        identifier = b.identifier;
        label = b.label;
        hint = b.hint;
        description = b.description;
        value = b.value;
        validationError = b.validationError;
        paneTitle = b.paneTitle;
        roleDescription = b.roleDescription;
        role = b.role;
        checked = b.checked;
        liveRegion = b.liveRegion;
        range = b.range;
        collectionInfo = b.collectionInfo;
        collectionItemInfo = b.collectionItemInfo;
        bounds = b.bounds == null ? new Rectangle() : new Rectangle(b.bounds);
        selected = b.selected;
        expanded = b.expanded;
        enabled = b.enabled;
        invalid = b.invalid;
        busy = b.busy;
        readOnly = b.readOnly;
        required = b.required;
        multiline = b.multiline;
        obscured = b.obscured;
        pressed = b.pressed;
        current = b.current;
        modal = b.modal;
        focusable = b.focusable;
        focused = b.focused;
        headingLevel = b.headingLevel;
        childIds = Collections.unmodifiableList(new ArrayList<Long>(b.childIds));
        actions = Collections.unmodifiableList(new ArrayList<AccessibilityAction>(b.actions));
    }

    public long getId() {
        return id;
    }
    public long getParentId() {
        return parentId;
    }
    public Component getComponent() {
        return component;
    }
    public String getVirtualKey() {
        return virtualKey;
    }
    public String getIdentifier() {
        return identifier;
    }
    public String getLabel() {
        return label;
    }
    public String getHint() {
        return hint;
    }
    public String getDescription() {
        return description;
    }
    public String getValue() {
        return value;
    }
    public String getValidationError() {
        return validationError;
    }
    public String getPaneTitle() {
        return paneTitle;
    }
    public String getRoleDescription() {
        return roleDescription;
    }
    public AccessibilityRole getRole() {
        return role;
    }
    public AccessibilityCheckedState getChecked() {
        return checked;
    }
    public AccessibilityLiveRegion getLiveRegion() {
        return liveRegion;
    }
    public AccessibilityRange getRange() {
        return range;
    }
    public AccessibilityCollectionInfo getCollectionInfo() {
        return collectionInfo;
    }
    public AccessibilityCollectionItemInfo getCollectionItemInfo() {
        return collectionItemInfo;
    }
    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }
    public Boolean getSelected() {
        return selected;
    }
    public Boolean getExpanded() {
        return expanded;
    }
    public Boolean getEnabled() {
        return enabled;
    }
    public Boolean getInvalid() {
        return invalid;
    }
    public Boolean getBusy() {
        return busy;
    }
    public Boolean getReadOnly() {
        return readOnly;
    }
    public Boolean getRequired() {
        return required;
    }
    public Boolean getMultiline() {
        return multiline;
    }
    public Boolean getObscured() {
        return obscured;
    }
    public Boolean getPressed() {
        return pressed;
    }
    public Boolean getCurrent() {
        return current;
    }
    public boolean isModal() {
        return modal;
    }
    public boolean isFocusable() {
        return focusable;
    }
    public boolean isFocused() {
        return focused;
    }
    public int getHeadingLevel() {
        return headingLevel;
    }
    public List<Long> getChildIds() {
        return childIds;
    }
    public List<AccessibilityAction> getActions() {
        return actions;
    }

    public AccessibilityAction getAction(String id) {
        if (id == null) {
            return null;
        }
        for (AccessibilityAction action : actions) {
            if (id.equals(action.getId())) {
                return action;
            }
        }
        return null;
    }

    static final class Builder {
        long id;
        long parentId = -1;
        Component component;
        String virtualKey;
        String identifier;
        String label;
        String hint;
        String description;
        String value;
        String validationError;
        String paneTitle;
        String roleDescription;
        AccessibilityRole role = AccessibilityRole.GENERIC;
        AccessibilityCheckedState checked = AccessibilityCheckedState.UNSPECIFIED;
        AccessibilityLiveRegion liveRegion = AccessibilityLiveRegion.OFF;
        AccessibilityRange range;
        AccessibilityCollectionInfo collectionInfo;
        AccessibilityCollectionItemInfo collectionItemInfo;
        Rectangle bounds;
        Boolean selected;
        Boolean expanded;
        Boolean enabled;
        Boolean invalid;
        Boolean busy;
        Boolean readOnly;
        Boolean required;
        Boolean multiline;
        Boolean obscured;
        Boolean pressed;
        Boolean current;
        boolean modal;
        boolean focusable;
        boolean focused;
        int headingLevel = -1;
        double sortKey = Double.NaN;
        Component traversalBefore;
        Component traversalAfter;
        List<Long> childIds = new ArrayList<Long>();
        List<AccessibilityAction> actions = new ArrayList<AccessibilityAction>();
    }
}
