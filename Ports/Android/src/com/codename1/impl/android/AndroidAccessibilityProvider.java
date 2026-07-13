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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Android virtual-view adapter for Codename One's lightweight semantic tree. */
final class AndroidAccessibilityProvider extends AccessibilityNodeProvider {
    private static final int CUSTOM_ACTION_BASE = 0x01000000;
    private static final String ROLE_DESCRIPTION_KEY = "AccessibilityNodeInfo.roleDescription";
    private final View host;
    private final AndroidImplementation implementation;
    private final android.view.accessibility.AccessibilityManager nativeAccessibilityManager;
    private final android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener stateListener;
    private final Object touchListener;
    private final Map<Long, Integer> virtualIds = new HashMap<Long, Integer>();
    private final Map<Integer, Long> semanticIds = new HashMap<Integer, Long>();
    private final Map<String, Integer> customActionIds = new HashMap<String, Integer>();
    private final Map<Integer, String> customActionKeys = new HashMap<Integer, String>();
    private long mappedGeneration = Long.MIN_VALUE;
    private int nextVirtualId = 1;
    private int nextCustomActionId = CUSTOM_ACTION_BASE;
    private int accessibilityFocusedId = Integer.MIN_VALUE;

    AndroidAccessibilityProvider(View host, AndroidImplementation implementation) {
        this.host = host;
        this.implementation = implementation;
        nativeAccessibilityManager = (android.view.accessibility.AccessibilityManager) host.getContext()
                .getSystemService(android.content.Context.ACCESSIBILITY_SERVICE);
        stateListener = new android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener() {
            @Override
            public void onAccessibilityStateChanged(boolean enabled) {
                accessibilityConsumerChanged();
            }
        };
        touchListener = Build.VERSION.SDK_INT >= 19 && nativeAccessibilityManager != null
                ? Api19TouchExploration.register(nativeAccessibilityManager, this) : null;
        if (nativeAccessibilityManager != null) {
            nativeAccessibilityManager.addAccessibilityStateChangeListener(stateListener);
        }
        host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        accessibilityConsumerChanged();
    }

    void dispose() {
        implementation.setAccessibilityTreeUpdateRequired(false);
        if (nativeAccessibilityManager != null) {
            nativeAccessibilityManager.removeAccessibilityStateChangeListener(stateListener);
            if (Build.VERSION.SDK_INT >= 19 && touchListener != null) {
                Api19TouchExploration.unregister(nativeAccessibilityManager, touchListener);
            }
        }
        virtualIds.clear();
        semanticIds.clear();
        customActionIds.clear();
        customActionKeys.clear();
    }

    private void accessibilityConsumerChanged() {
        boolean active = implementation.isScreenReaderEnabled();
        implementation.setAccessibilityTreeUpdateRequired(active);
        if (active) {
            AccessibilityManager.getInstance().invalidateAll();
        }
    }

    private static final class Api19TouchExploration {
        private Api19TouchExploration() {
        }

        static Object register(android.view.accessibility.AccessibilityManager manager,
                final AndroidAccessibilityProvider provider) {
            android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener listener =
                    new android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener() {
                        @Override
                        public void onTouchExplorationStateChanged(boolean enabled) {
                            provider.accessibilityConsumerChanged();
                        }
                    };
            manager.addTouchExplorationStateChangeListener(listener);
            return listener;
        }

        static void unregister(android.view.accessibility.AccessibilityManager manager, Object value) {
            manager.removeTouchExplorationStateChangeListener(
                    (android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener)value);
        }
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
        AccessibilityTreeSnapshot tree = currentTree();
        if (virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
            return createHostNode(tree);
        }
        AccessibilityNodeSnapshot node = nodeForVirtualId(tree, virtualViewId);
        return node == null ? null : createVirtualNode(tree, node, virtualViewId);
    }

    private AccessibilityNodeInfo createHostNode(AccessibilityTreeSnapshot tree) {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(host);
        host.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("com.codename1.ui.Form");
        info.setPackageName(host.getContext().getPackageName());
        info.setScrollable(false);
        for (Long rootId : tree.getRootIds()) {
            info.addChild(host, virtualIdFor(rootId.longValue()));
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
        else info.setParent(host, virtualIdFor(node.getParentId()));
        for (Long childId : node.getChildIds()) info.addChild(host, virtualIdFor(childId.longValue()));

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
            setApi28Semantics(info,
                    node.getHeadingLevel() > 0
                            || node.getRole() == AccessibilityRole.HEADING
                            || node.getCollectionItemInfo() != null && node.getCollectionItemInfo().isHeading(),
                    node.isFocusable() || label != null, node.getPaneTitle());
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

    /**
     * Invokes API 28 node properties without requiring the Android port's
     * compile-time SDK stub to expose them.  Release builds may compile the
     * port against an older android.jar even though these calls run only on
     * Android 9 and newer.
     */
    private static void setApi28Semantics(AccessibilityNodeInfo info, boolean heading,
            boolean screenReaderFocusable, String paneTitle) {
        try {
            Class type = AccessibilityNodeInfo.class;
            type.getMethod("setHeading", new Class[]{Boolean.TYPE})
                    .invoke(info, new Object[]{Boolean.valueOf(heading)});
            type.getMethod("setScreenReaderFocusable", new Class[]{Boolean.TYPE})
                    .invoke(info, new Object[]{Boolean.valueOf(screenReaderFocusable)});
            if (paneTitle != null) {
                type.getMethod("setPaneTitle", new Class[]{CharSequence.class})
                        .invoke(info, new Object[]{paneTitle});
            }
        } catch (Throwable ignored) {
            // The runtime API is authoritative; gracefully omit these optional
            // properties on vendor builds that don't expose the API 28 methods.
        }
    }

    private void addActions(AccessibilityNodeInfo info, AccessibilityNodeSnapshot node) {
        if (node.isFocusable()) info.addAction(AccessibilityNodeInfo.ACTION_FOCUS);
        if (hasAction(node, AccessibilityAction.ACTIVATE)) info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (hasAction(node, AccessibilityAction.LONG_PRESS)) info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        if (hasAction(node, AccessibilityAction.INCREMENT)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        if (hasAction(node, AccessibilityAction.DECREMENT)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        if (hasAction(node, AccessibilityAction.SCROLL_FORWARD)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        if (hasAction(node, AccessibilityAction.SCROLL_BACKWARD)) info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        info.addAction(accessibilityFocusedId == virtualIdFor(node.getId())
                ? AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS
                : AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
        if (Build.VERSION.SDK_INT >= 21) {
            for (AccessibilityAction action : node.getActions()) {
                if (isStandard(action.getId())) continue;
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
                        customActionId(node.getId(), action.getId()),
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
        AccessibilityTreeSnapshot tree = currentTree();
        AccessibilityNodeSnapshot node = nodeForVirtualId(tree, virtualViewId);
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
                if (!isStandard(candidate.getId())
                        && customActionId(node.getId(), candidate.getId()) == action) {
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
        AccessibilityTreeSnapshot tree = currentTree();
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            if (contains(node.getLabel(), lower) || contains(node.getValue(), lower)
                    || contains(node.getDescription(), lower)) {
                result.add(createVirtualNode(tree, node, virtualIdFor(node.getId())));
            }
        }
        return result;
    }

    @Override
    public AccessibilityNodeInfo findFocus(int focus) {
        AccessibilityTreeSnapshot tree = currentTree();
        if (focus == AccessibilityNodeInfo.FOCUS_ACCESSIBILITY && accessibilityFocusedId != Integer.MIN_VALUE) {
            AccessibilityNodeSnapshot node = nodeForVirtualId(tree, accessibilityFocusedId);
            return node == null ? null : createVirtualNode(tree, node, accessibilityFocusedId);
        }
        if (focus == AccessibilityNodeInfo.FOCUS_INPUT) {
            for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
                if (node.isFocused()) return createVirtualNode(tree, node, virtualIdFor(node.getId()));
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
        if (nativeAccessibilityManager == null || !nativeAccessibilityManager.isEnabled()) {
            event.recycle();
            return;
        }
        ViewParent parent = host.getParent();
        if (parent != null) {
            parent.requestSendAccessibilityEvent(host, event);
        } else {
            event.recycle();
        }
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

    private AccessibilityTreeSnapshot currentTree() {
        AccessibilityTreeSnapshot tree = implementation.getAccessibilityTreeSnapshot();
        synchronizeIds(tree);
        return tree;
    }

    private void synchronizeIds(AccessibilityTreeSnapshot tree) {
        if (mappedGeneration == tree.getGeneration()) {
            return;
        }
        Set<Long> liveSemanticIds = tree.getNodes().keySet();
        Iterator<Map.Entry<Long, Integer>> virtualIterator = virtualIds.entrySet().iterator();
        while (virtualIterator.hasNext()) {
            Map.Entry<Long, Integer> entry = virtualIterator.next();
            if (!liveSemanticIds.contains(entry.getKey())) {
                semanticIds.remove(entry.getValue());
                virtualIterator.remove();
            }
        }
        for (Long semanticId : liveSemanticIds) {
            virtualIdFor(semanticId.longValue());
        }

        Set<String> liveActionKeys = new HashSet<String>();
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            for (AccessibilityAction action : node.getActions()) {
                if (!isStandard(action.getId())) {
                    liveActionKeys.add(actionKey(node.getId(), action.getId()));
                }
            }
        }
        Iterator<Map.Entry<String, Integer>> actionIterator = customActionIds.entrySet().iterator();
        while (actionIterator.hasNext()) {
            Map.Entry<String, Integer> entry = actionIterator.next();
            if (!liveActionKeys.contains(entry.getKey())) {
                customActionKeys.remove(entry.getValue());
                actionIterator.remove();
            }
        }
        if (accessibilityFocusedId != Integer.MIN_VALUE && !semanticIds.containsKey(
                Integer.valueOf(accessibilityFocusedId))) {
            accessibilityFocusedId = Integer.MIN_VALUE;
        }
        mappedGeneration = tree.getGeneration();
    }

    private AccessibilityNodeSnapshot nodeForVirtualId(AccessibilityTreeSnapshot tree, int virtualId) {
        Long semanticId = semanticIds.get(Integer.valueOf(virtualId));
        return semanticId == null ? null : tree.getNode(semanticId.longValue());
    }

    private int virtualIdFor(long semanticId) {
        Long key = Long.valueOf(semanticId);
        Integer existing = virtualIds.get(key);
        if (existing != null) {
            return existing.intValue();
        }
        int candidate = nextVirtualId;
        while (candidate == AccessibilityNodeProvider.HOST_VIEW_ID
                || semanticIds.containsKey(Integer.valueOf(candidate))) {
            candidate = nextVirtualId(candidate);
        }
        nextVirtualId = nextVirtualId(candidate);
        Integer virtualId = Integer.valueOf(candidate);
        virtualIds.put(key, virtualId);
        semanticIds.put(virtualId, key);
        return candidate;
    }

    private int nextVirtualId(int current) {
        return current == Integer.MAX_VALUE ? 1 : current + 1;
    }

    private int customActionId(long nodeId, String id) {
        String key = actionKey(nodeId, id);
        Integer existing = customActionIds.get(key);
        if (existing != null) {
            return existing.intValue();
        }
        int candidate = nextCustomActionId;
        while (customActionKeys.containsKey(Integer.valueOf(candidate))) {
            candidate = nextCustomActionId(candidate);
        }
        nextCustomActionId = nextCustomActionId(candidate);
        Integer actionId = Integer.valueOf(candidate);
        customActionIds.put(key, actionId);
        customActionKeys.put(actionId, key);
        return candidate;
    }

    private int nextCustomActionId(int current) {
        return current == Integer.MAX_VALUE ? CUSTOM_ACTION_BASE : current + 1;
    }

    private String actionKey(long nodeId, String actionId) {
        return nodeId + "\n" + actionId;
    }
}
