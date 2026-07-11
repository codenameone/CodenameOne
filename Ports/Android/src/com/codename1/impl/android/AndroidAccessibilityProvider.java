/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.impl.android;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityCheckedState;
import com.codename1.ui.accessibility.AccessibilityCollectionInfo;
import com.codename1.ui.accessibility.AccessibilityCollectionItemInfo;
import com.codename1.ui.accessibility.AccessibilityLiveRegion;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityRange;
import com.codename1.ui.accessibility.AccessibilityRole;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Android virtual-view adapter for Codename One's lightweight semantic tree. */
final class AndroidAccessibilityProvider extends AccessibilityNodeProvider {
    private static final int CUSTOM_ACTION_BASE = 0x01000000;
    private static final String ROLE_DESCRIPTION_KEY = "AccessibilityNodeInfo.roleDescription";
    private final View host;
    private final AndroidImplementation implementation;
    private int accessibilityFocusedId = Integer.MIN_VALUE;

    AndroidAccessibilityProvider(View host, AndroidImplementation implementation) {
        this.host = host;
        this.implementation = implementation;
        host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
        AccessibilityTreeSnapshot tree = implementation.getAccessibilityTreeSnapshot();
        if (virtualViewId == View.NO_ID || virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
            return createHostNode(tree);
        }
        AccessibilityNodeSnapshot node = tree.getNode(virtualViewId);
        return node == null ? null : createVirtualNode(tree, node, virtualViewId);
    }

    private AccessibilityNodeInfo createHostNode(AccessibilityTreeSnapshot tree) {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(host);
        host.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("com.codename1.ui.Form");
        info.setPackageName(host.getContext().getPackageName());
        info.setScrollable(false);
        for (Long rootId : tree.getRootIds()) {
            info.addChild(host, toVirtualId(rootId.longValue()));
        }
        return info;
    }

    private AccessibilityNodeInfo createVirtualNode(AccessibilityTreeSnapshot tree,
            AccessibilityNodeSnapshot node, int virtualViewId) {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        info.setPackageName(host.getContext().getPackageName());
        info.setClassName(className(node.getRole()));
        info.setSource(host, virtualViewId);
        if (node.getParentId() < 0) info.setParent(host);
        else info.setParent(host, toVirtualId(node.getParentId()));
        for (Long childId : node.getChildIds()) info.addChild(host, toVirtualId(childId.longValue()));

        String label = node.getLabel();
        if (usesText(node.getRole())) info.setText(label);
        else info.setContentDescription(label);
        if (Build.VERSION.SDK_INT >= 26 && node.getHint() != null) info.setHintText(node.getHint());
        if (Build.VERSION.SDK_INT >= 19 && node.getRoleDescription() != null) {
            info.getExtras().putCharSequence(ROLE_DESCRIPTION_KEY, node.getRoleDescription());
        }
        if (Build.VERSION.SDK_INT >= 18 && node.getIdentifier() != null) {
            info.setViewIdResourceName(host.getContext().getPackageName() + ":id/" + node.getIdentifier());
        }

        boolean enabled = node.getEnabled() == null || node.getEnabled().booleanValue();
        info.setEnabled(enabled);
        info.setVisibleToUser(isVisible(node));
        info.setFocusable(node.isFocusable());
        info.setFocused(node.isFocused());
        info.setAccessibilityFocused(virtualViewId == accessibilityFocusedId);
        info.setSelected(Boolean.TRUE.equals(node.getSelected()));
        info.setClickable(hasAction(node, AccessibilityAction.ACTIVATE));
        info.setLongClickable(hasAction(node, AccessibilityAction.LONG_PRESS));
        info.setScrollable(hasAction(node, AccessibilityAction.SCROLL_FORWARD)
                || hasAction(node, AccessibilityAction.SCROLL_BACKWARD));

        AccessibilityCheckedState checked = node.getChecked();
        if (checked != AccessibilityCheckedState.UNSPECIFIED) {
            info.setCheckable(true);
            info.setChecked(checked == AccessibilityCheckedState.CHECKED || checked == AccessibilityCheckedState.MIXED);
            if (checked == AccessibilityCheckedState.MIXED && Build.VERSION.SDK_INT >= 19) {
                info.getExtras().putCharSequence("com.codename1.accessibility.checkedState", "mixed");
            }
        }
        if (Build.VERSION.SDK_INT >= 19) {
            info.setContentInvalid(Boolean.TRUE.equals(node.getInvalid()));
            if (node.getCollectionInfo() != null) info.setCollectionInfo(collection(node.getCollectionInfo()));
            if (node.getCollectionItemInfo() != null) info.setCollectionItemInfo(collectionItem(node.getCollectionItemInfo(), node));
            if (node.getRange() != null) info.setRangeInfo(range(node.getRange()));
            if (node.getExpanded() != null) {
                info.getExtras().putBoolean("com.codename1.accessibility.expanded", node.getExpanded().booleanValue());
            }
        }
        if (Build.VERSION.SDK_INT >= 21 && node.getValidationError() != null) info.setError(node.getValidationError());
        if (Build.VERSION.SDK_INT >= 28) {
            info.setHeading(node.getHeadingLevel() > 0
                    || node.getRole() == AccessibilityRole.HEADING
                    || node.getCollectionItemInfo() != null && node.getCollectionItemInfo().isHeading());
            info.setScreenReaderFocusable(node.isFocusable() || label != null);
            if (node.getPaneTitle() != null) info.setPaneTitle(node.getPaneTitle());
        }
        if (Build.VERSION.SDK_INT >= 19) {
            info.setLiveRegion(node.getLiveRegion() == AccessibilityLiveRegion.ASSERTIVE
                    ? View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
                    : node.getLiveRegion() == AccessibilityLiveRegion.POLITE
                    ? View.ACCESSIBILITY_LIVE_REGION_POLITE : View.ACCESSIBILITY_LIVE_REGION_NONE);
        }

        addActions(info, node);
        setBounds(info, node);
        return info;
    }

    private void addActions(AccessibilityNodeInfo info, AccessibilityNodeSnapshot node) {
        if (node.isFocusable()) info.addAction(AccessibilityNodeInfo.ACTION_FOCUS);
        if (hasAction(node, AccessibilityAction.ACTIVATE)) info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (hasAction(node, AccessibilityAction.LONG_PRESS)) info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        if (hasAction(node, AccessibilityAction.INCREMENT)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        if (hasAction(node, AccessibilityAction.DECREMENT)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        if (hasAction(node, AccessibilityAction.SCROLL_FORWARD)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        if (hasAction(node, AccessibilityAction.SCROLL_BACKWARD)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        info.addAction(accessibilityFocusedId == toVirtualId(node.getId())
                ? AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS
                : AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
        if (Build.VERSION.SDK_INT >= 21) {
            for (AccessibilityAction action : node.getActions()) {
                if (isStandard(action.getId())) continue;
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(customActionId(action.getId()),
                        action.getLabel() == null ? action.getId() : action.getLabel()));
            }
            if (hasAction(node, AccessibilityAction.SET_TEXT)) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);
            }
        }
    }

    @Override
    public boolean performAction(int virtualViewId, int action, Bundle arguments) {
        if (virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
            return host.performAccessibilityAction(action, arguments);
        }
        AccessibilityNodeSnapshot node = implementation.getAccessibilityTreeSnapshot().getNode(virtualViewId);
        if (node == null) return false;
        if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
            if (accessibilityFocusedId == virtualViewId) return false;
            int previous = accessibilityFocusedId;
            accessibilityFocusedId = virtualViewId;
            if (previous != Integer.MIN_VALUE) sendEvent(previous, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
            sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) {
            if (accessibilityFocusedId != virtualViewId) return false;
            accessibilityFocusedId = Integer.MIN_VALUE;
            sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
            return true;
        }
        String actionId = null;
        Object argument = null;
        if (action == AccessibilityNodeInfo.ACTION_CLICK) actionId = AccessibilityAction.ACTIVATE;
        else if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK) actionId = AccessibilityAction.LONG_PRESS;
        else if (action == AccessibilityNodeInfo.ACTION_FOCUS) actionId = AccessibilityAction.FOCUS;
        else if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            actionId = hasAction(node, AccessibilityAction.INCREMENT)
                    ? AccessibilityAction.INCREMENT : AccessibilityAction.SCROLL_FORWARD;
        } else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            actionId = hasAction(node, AccessibilityAction.DECREMENT)
                    ? AccessibilityAction.DECREMENT : AccessibilityAction.SCROLL_BACKWARD;
        } else if (Build.VERSION.SDK_INT >= 21 && action == AccessibilityNodeInfo.ACTION_SET_TEXT) {
            actionId = AccessibilityAction.SET_TEXT;
            argument = arguments == null ? null : arguments.getCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE);
            if (argument != null) argument = argument.toString();
        } else {
            for (AccessibilityAction candidate : node.getActions()) {
                if (customActionId(candidate.getId()) == action) {
                    actionId = candidate.getId();
                    break;
                }
            }
        }
        return actionId != null && implementation.performAccessibilityAction(node.getId(), actionId, argument);
    }

    @Override
    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched, int virtualViewId) {
        if (searched == null) return Collections.emptyList();
        String lower = searched.toLowerCase();
        List<AccessibilityNodeInfo> result = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityTreeSnapshot tree = implementation.getAccessibilityTreeSnapshot();
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            if (contains(node.getLabel(), lower) || contains(node.getValue(), lower)
                    || contains(node.getDescription(), lower)) {
                result.add(createVirtualNode(tree, node, toVirtualId(node.getId())));
            }
        }
        return result;
    }

    @Override
    public AccessibilityNodeInfo findFocus(int focus) {
        AccessibilityTreeSnapshot tree = implementation.getAccessibilityTreeSnapshot();
        if (focus == AccessibilityNodeInfo.FOCUS_ACCESSIBILITY && accessibilityFocusedId != Integer.MIN_VALUE) {
            AccessibilityNodeSnapshot node = tree.getNode(accessibilityFocusedId);
            return node == null ? null : createVirtualNode(tree, node, accessibilityFocusedId);
        }
        if (focus == AccessibilityNodeInfo.FOCUS_INPUT) {
            for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
                if (node.isFocused()) return createVirtualNode(tree, node, toVirtualId(node.getId()));
            }
        }
        return null;
    }

    void invalidate(int changes) {
        if ((changes & AccessibilityManager.CHANGE_STRUCTURE) != 0
                || (changes & AccessibilityManager.CHANGE_PANE) != 0) {
            sendHostEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        } else if ((changes & AccessibilityManager.CHANGE_FOCUS) != 0) {
            sendHostEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        } else {
            sendHostEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        }
    }

    private void sendHostEvent(int type) {
        AccessibilityEvent event = AccessibilityEvent.obtain(type);
        event.setPackageName(host.getContext().getPackageName());
        event.setClassName("com.codename1.ui.Form");
        event.setSource(host);
        send(event);
    }

    private void sendEvent(int virtualId, int type) {
        AccessibilityEvent event = AccessibilityEvent.obtain(type);
        event.setPackageName(host.getContext().getPackageName());
        event.setClassName("com.codename1.ui.Component");
        event.setSource(host, virtualId);
        send(event);
    }

    private void send(AccessibilityEvent event) {
        ViewParent parent = host.getParent();
        if (parent != null) parent.requestSendAccessibilityEvent(host, event);
    }

    private void setBounds(AccessibilityNodeInfo info, AccessibilityNodeSnapshot node) {
        com.codename1.ui.geom.Rectangle b = node.getBounds();
        Rect parent = new Rect(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight());
        info.setBoundsInParent(parent);
        int[] location = new int[2];
        host.getLocationOnScreen(location);
        Rect screen = new Rect(parent);
        screen.offset(location[0], location[1]);
        info.setBoundsInScreen(screen);
    }

    private boolean isVisible(AccessibilityNodeSnapshot node) {
        com.codename1.ui.geom.Rectangle b = node.getBounds();
        return host.isShown() && b.getWidth() > 0 && b.getHeight() > 0;
    }

    private AccessibilityNodeInfo.CollectionInfo collection(AccessibilityCollectionInfo info) {
        int mode = info.getSelectionMode() == AccessibilityCollectionInfo.SELECTION_MULTIPLE
                ? AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_MULTIPLE
                : info.getSelectionMode() == AccessibilityCollectionInfo.SELECTION_SINGLE
                ? AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE
                : AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_NONE;
        if (Build.VERSION.SDK_INT >= 21) {
            return AccessibilityNodeInfo.CollectionInfo.obtain(info.getRowCount(), info.getColumnCount(),
                    info.isHierarchical(), mode);
        }
        return AccessibilityNodeInfo.CollectionInfo.obtain(info.getRowCount(), info.getColumnCount(),
                info.isHierarchical());
    }

    private AccessibilityNodeInfo.CollectionItemInfo collectionItem(AccessibilityCollectionItemInfo item,
            AccessibilityNodeSnapshot node) {
        if (Build.VERSION.SDK_INT >= 21) {
            return AccessibilityNodeInfo.CollectionItemInfo.obtain(item.getRowIndex(), Math.max(1, item.getRowSpan()),
                    item.getColumnIndex(), Math.max(1, item.getColumnSpan()), item.isHeading(),
                    Boolean.TRUE.equals(node.getSelected()));
        }
        return AccessibilityNodeInfo.CollectionItemInfo.obtain(item.getRowIndex(), Math.max(1, item.getRowSpan()),
                item.getColumnIndex(), Math.max(1, item.getColumnSpan()), item.isHeading());
    }

    private AccessibilityNodeInfo.RangeInfo range(AccessibilityRange range) {
        return AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
                (float)range.getMinimum(), (float)range.getMaximum(), (float)range.getCurrent());
    }

    private String className(AccessibilityRole role) {
        switch (role) {
            case BUTTON: return "android.widget.Button";
            case TOGGLE_BUTTON: return "android.widget.ToggleButton";
            case CHECKBOX: return "android.widget.CheckBox";
            case RADIO_BUTTON: return "android.widget.RadioButton";
            case SWITCH: return "android.widget.Switch";
            case TEXT_FIELD:
            case SEARCH_FIELD: return "android.widget.EditText";
            case SLIDER: return "android.widget.SeekBar";
            case PROGRESS_BAR: return "android.widget.ProgressBar";
            case LIST: return "android.widget.ListView";
            case GRID: return "android.widget.GridView";
            case IMAGE: return "android.widget.ImageView";
            case TAB: return "android.app.ActionBar$Tab";
            case DIALOG:
            case ALERT: return "android.app.Dialog";
            default: return "android.view.View";
        }
    }

    private boolean usesText(AccessibilityRole role) {
        return role == AccessibilityRole.STATIC_TEXT || role == AccessibilityRole.TEXT_FIELD
                || role == AccessibilityRole.SEARCH_FIELD || role == AccessibilityRole.HEADING;
    }

    private boolean contains(String value, String searchedLower) {
        return value != null && value.toLowerCase().contains(searchedLower);
    }

    private boolean hasAction(AccessibilityNodeSnapshot node, String id) {
        return node.getAction(id) != null;
    }

    private boolean isStandard(String id) {
        return AccessibilityAction.ACTIVATE.equals(id) || AccessibilityAction.LONG_PRESS.equals(id)
                || AccessibilityAction.INCREMENT.equals(id) || AccessibilityAction.DECREMENT.equals(id)
                || AccessibilityAction.FOCUS.equals(id) || AccessibilityAction.SET_TEXT.equals(id)
                || AccessibilityAction.SCROLL_FORWARD.equals(id) || AccessibilityAction.SCROLL_BACKWARD.equals(id);
    }

    private int customActionId(String id) {
        return CUSTOM_ACTION_BASE | (id.hashCode() & 0x00ffffff);
    }

    private int toVirtualId(long id) {
        return (int)id;
    }
}
