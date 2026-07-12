/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui.accessibility;

import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Immutable accessibility tree for the current form.
public final class AccessibilityTreeSnapshot {
    private final long generation;
    private final List<Long> rootIds;
    private final Map<Long, AccessibilityNodeSnapshot> nodes;

    AccessibilityTreeSnapshot(long generation, List<Long> rootIds, Map<Long, AccessibilityNodeSnapshot> nodes) {
        this.generation = generation;
        this.rootIds = Collections.unmodifiableList(new ArrayList<Long>(rootIds));
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<Long, AccessibilityNodeSnapshot>(nodes));
    }

    public long getGeneration() {
        return generation;
    }
    public List<Long> getRootIds() {
        return rootIds;
    }
    public Map<Long, AccessibilityNodeSnapshot> getNodes() {
        return nodes;
    }
    public AccessibilityNodeSnapshot getNode(long id) {
        return nodes.get(Long.valueOf(id));
    }

    /// Returns the deepest semantic node containing the screen coordinate.
    public AccessibilityNodeSnapshot getNodeAt(int x, int y) {
        AccessibilityNodeSnapshot best = null;
        for (AccessibilityNodeSnapshot node : nodes.values()) {
            Rectangle r = node.getBounds();
            if (r.getWidth() > 0 && r.getHeight() > 0 && r.contains(x, y)) {
                if (best == null || isDescendant(node, best)) {
                    best = node;
                }
            }
        }
        return best;
    }

    private boolean isDescendant(AccessibilityNodeSnapshot possibleChild, AccessibilityNodeSnapshot possibleParent) {
        long parent = possibleChild.getParentId();
        while (parent >= 0) {
            if (parent == possibleParent.getId()) {
                return true;
            }
            AccessibilityNodeSnapshot node = getNode(parent);
            if (node == null) {
                return false;
            }
            parent = node.getParentId();
        }
        return false;
    }

    public String toJson() {
        StringBuilder out = new StringBuilder();
        out.append("{\"generation\":").append(generation).append(",\"roots\":[");
        for (int i = 0; i < rootIds.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(rootIds.get(i));
        }
        out.append("],\"nodes\":[");
        boolean firstNode = true;
        for (AccessibilityNodeSnapshot node : nodes.values()) {
            if (!firstNode) {
                out.append(',');
            }
            firstNode = false;
            out.append('{');
            out.append("\"id\":").append(node.getId());
            out.append(",\"parent\":").append(node.getParentId());
            out.append(",\"role\":");
            appendJson(out, node.getRole().name());
            out.append(",\"label\":");
            appendJson(out, node.getLabel());
            out.append(",\"value\":");
            appendJson(out, node.getValue());
            out.append(",\"hint\":");
            appendJson(out, node.getHint());
            out.append(",\"description\":");
            appendJson(out, node.getDescription());
            out.append(",\"error\":");
            appendJson(out, node.getValidationError());
            out.append(",\"paneTitle\":");
            appendJson(out, node.getPaneTitle());
            out.append(",\"identifier\":");
            appendJson(out, node.getIdentifier());
            out.append(",\"roleDescription\":");
            appendJson(out, node.getRoleDescription());
            out.append(",\"checked\":");
            appendJson(out, node.getChecked().name());
            out.append(",\"liveRegion\":");
            appendJson(out, node.getLiveRegion().name());
            out.append(",\"selected\":");
            appendBoolean(out, node.getSelected());
            out.append(",\"expanded\":");
            appendBoolean(out, node.getExpanded());
            out.append(",\"enabled\":");
            appendBoolean(out, node.getEnabled());
            out.append(",\"invalid\":");
            appendBoolean(out, node.getInvalid());
            out.append(",\"busy\":");
            appendBoolean(out, node.getBusy());
            out.append(",\"readOnly\":");
            appendBoolean(out, node.getReadOnly());
            out.append(",\"required\":");
            appendBoolean(out, node.getRequired());
            out.append(",\"multiline\":");
            appendBoolean(out, node.getMultiline());
            out.append(",\"obscured\":");
            appendBoolean(out, node.getObscured());
            out.append(",\"pressed\":");
            appendBoolean(out, node.getPressed());
            out.append(",\"current\":");
            appendBoolean(out, node.getCurrent());
            out.append(",\"modal\":").append(node.isModal());
            out.append(",\"focusable\":").append(node.isFocusable());
            out.append(",\"focused\":").append(node.isFocused());
            out.append(",\"headingLevel\":").append(node.getHeadingLevel());
            if (node.getRange() != null) {
                AccessibilityRange range = node.getRange();
                out.append(",\"range\":{");
                out.append("\"min\":").append(range.getMinimum());
                out.append(",\"max\":").append(range.getMaximum());
                out.append(",\"current\":").append(range.getCurrent());
                out.append(",\"step\":").append(range.getStep());
                out.append(",\"text\":");
                appendJson(out, range.getText());
                out.append('}');
            }
            if (node.getCollectionInfo() != null) {
                AccessibilityCollectionInfo collection = node.getCollectionInfo();
                out.append(",\"collection\":{");
                out.append("\"rows\":").append(collection.getRowCount());
                out.append(",\"columns\":").append(collection.getColumnCount());
                out.append(",\"hierarchical\":").append(collection.isHierarchical());
                out.append(",\"selectionMode\":").append(collection.getSelectionMode()).append('}');
            }
            if (node.getCollectionItemInfo() != null) {
                AccessibilityCollectionItemInfo item = node.getCollectionItemInfo();
                out.append(",\"collectionItem\":{");
                out.append("\"row\":").append(item.getRowIndex());
                out.append(",\"rowSpan\":").append(item.getRowSpan());
                out.append(",\"column\":").append(item.getColumnIndex());
                out.append(",\"columnSpan\":").append(item.getColumnSpan());
                out.append(",\"position\":").append(item.getPositionInSet());
                out.append(",\"setSize\":").append(item.getSetSize());
                out.append(",\"level\":").append(item.getLevel());
                out.append(",\"heading\":").append(item.isHeading()).append('}');
            }
            Rectangle bounds = node.getBounds();
            out.append(",\"bounds\":[")
                    .append(bounds.getX())
                    .append(',')
                    .append(bounds.getY())
                    .append(',')
                    .append(bounds.getWidth())
                    .append(',')
                    .append(bounds.getHeight())
                    .append(']');
            out.append(",\"children\":[");
            for (int i = 0; i < node.getChildIds().size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(node.getChildIds().get(i));
            }
            out.append("],\"actions\":[");
            for (int i = 0; i < node.getActions().size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                AccessibilityAction action = node.getActions().get(i);
                out.append('{').append("\"id\":");
                appendJson(out, action.getId());
                out.append(",\"label\":");
                appendJson(out, action.getLabel());
                out.append(",\"enabled\":").append(action.isEnabled()).append('}');
            }
            out.append("]}");
        }
        out.append("]}");
        return out.toString();
    }

    private static void appendJson(StringBuilder out, String value) {
        if (value == null) {
            out.append("null");
            return;
        }
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\');
            }
            if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }

    private static void appendBoolean(StringBuilder out, Boolean value) {
        if (value == null) {
            out.append("null");
        } else {
            out.append(value.booleanValue());
        }
    }
}
