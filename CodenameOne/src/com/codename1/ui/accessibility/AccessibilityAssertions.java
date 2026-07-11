/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui.accessibility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Automated semantic-tree assertions suitable for unit tests and CI.
public final class AccessibilityAssertions {
    private AccessibilityAssertions() {
    }

    public static List<AccessibilityIssue> audit(AccessibilityTreeSnapshot tree) {
        List<AccessibilityIssue> issues = new ArrayList<AccessibilityIssue>();
        Set<String> identifiers = new HashSet<String>();
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            auditNode(tree, node, identifiers, issues);
        }
        for (Long root : tree.getRootIds()) {
            detectCycle(tree, root.longValue(), new HashSet<Long>(), new HashSet<Long>(), issues);
        }
        return issues;
    }

    public static void assertNoErrors(AccessibilityTreeSnapshot tree) {
        List<AccessibilityIssue> issues = audit(tree);
        StringBuilder errors = new StringBuilder();
        for (AccessibilityIssue issue : issues) {
            if (issue.getSeverity() == AccessibilityIssue.Severity.ERROR) {
                if (errors.length() > 0) errors.append('\n');
                errors.append(issue.toString());
            }
        }
        if (errors.length() > 0) throw new AssertionError(errors.toString());
    }

    public static void assertNoUnlabeledInteractiveNodes(AccessibilityTreeSnapshot tree) {
        StringBuilder errors = new StringBuilder();
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            if (node.isFocusable() && empty(node.getLabel()) && empty(node.getValue())) {
                if (errors.length() > 0) errors.append('\n');
                errors.append("Interactive node ").append(node.getId()).append(" (")
                        .append(node.getRole()).append(") has no accessible name");
            }
        }
        if (errors.length() > 0) throw new AssertionError(errors.toString());
    }

    private static void auditNode(AccessibilityTreeSnapshot tree, AccessibilityNodeSnapshot node,
            Set<String> identifiers, List<AccessibilityIssue> issues) {
        if (node.isFocusable() && empty(node.getLabel()) && empty(node.getValue())) {
            error(issues, "unlabeled-interactive", "Interactive nodes require a label or value", node);
        }
        if (node.getRange() != null && !node.getRange().isValid()) {
            error(issues, "invalid-range", "Range must satisfy min <= current <= max and step >= 0", node);
        }
        if (node.getRole() == AccessibilityRole.HEADING && node.getHeadingLevel() == 0) {
            error(issues, "invalid-heading-level", "Heading levels are 1-based", node);
        }
        if (node.getChecked() != AccessibilityCheckedState.UNSPECIFIED
                && node.getRole() != AccessibilityRole.CHECKBOX
                && node.getRole() != AccessibilityRole.RADIO_BUTTON
                && node.getRole() != AccessibilityRole.SWITCH
                && node.getRole() != AccessibilityRole.TREE_ITEM
                && node.getRole() != AccessibilityRole.MENU_ITEM) {
            warning(issues, "checked-role", "Checked state is unusual for this role", node);
        }
        if (node.getExpanded() != null && node.getRole() != AccessibilityRole.TREE_ITEM
                && node.getRole() != AccessibilityRole.BUTTON && node.getRole() != AccessibilityRole.MENU_ITEM
                && node.getRole() != AccessibilityRole.COMBO_BOX && node.getRole() != AccessibilityRole.GENERIC) {
            warning(issues, "expanded-role", "Expanded state is unusual for this role", node);
        }
        if (node.getCollectionItemInfo() != null) {
            AccessibilityCollectionItemInfo item = node.getCollectionItemInfo();
            if (item.getPositionInSet() == 0 || item.getSetSize() == 0
                    || item.getPositionInSet() > 0 && item.getSetSize() > 0
                    && item.getPositionInSet() > item.getSetSize()) {
                error(issues, "invalid-collection-position", "Collection position and size must be 1-based and consistent", node);
            }
        }
        if (node.getParentId() >= 0 && tree.getNode(node.getParentId()) == null) {
            error(issues, "missing-parent", "Semantic parent does not exist", node);
        }
        if (node.getIdentifier() != null && !identifiers.add(node.getIdentifier())) {
            error(issues, "duplicate-identifier", "Accessibility identifiers must be unique", node);
        }
        if (node.getBounds().getWidth() <= 0 || node.getBounds().getHeight() <= 0) {
            warning(issues, "empty-bounds", "Node is fully clipped or has empty bounds", node);
        }
        Set<String> actions = new HashSet<String>();
        for (AccessibilityAction action : node.getActions()) {
            if (!actions.add(action.getId())) error(issues, "duplicate-action", "Action ids must be unique per node", node);
            if (!isStandardAction(action.getId()) && empty(action.getLabel())) {
                error(issues, "unlabeled-custom-action", "Custom accessibility actions require localized labels", node);
            }
        }
        if ((node.getRole() == AccessibilityRole.DIALOG || node.getRole() == AccessibilityRole.ALERT)
                && empty(node.getLabel()) && empty(node.getPaneTitle())) {
            error(issues, "unnamed-dialog", "Dialogs require a label or pane title", node);
        }
    }

    private static void detectCycle(AccessibilityTreeSnapshot tree, long id, Set<Long> visiting,
            Set<Long> visited, List<AccessibilityIssue> issues) {
        Long boxed = Long.valueOf(id);
        if (visited.contains(boxed)) return;
        if (!visiting.add(boxed)) {
            AccessibilityNodeSnapshot node = tree.getNode(id);
            if (node != null) error(issues, "tree-cycle", "Semantic tree contains a cycle", node);
            return;
        }
        AccessibilityNodeSnapshot node = tree.getNode(id);
        if (node != null) {
            for (Long child : node.getChildIds()) detectCycle(tree, child.longValue(), visiting, visited, issues);
        }
        visiting.remove(boxed);
        visited.add(boxed);
    }

    private static boolean isStandardAction(String id) {
        return AccessibilityAction.ACTIVATE.equals(id) || AccessibilityAction.LONG_PRESS.equals(id)
                || AccessibilityAction.INCREMENT.equals(id) || AccessibilityAction.DECREMENT.equals(id)
                || AccessibilityAction.SET_VALUE.equals(id) || AccessibilityAction.SET_TEXT.equals(id)
                || AccessibilityAction.FOCUS.equals(id) || AccessibilityAction.DISMISS.equals(id)
                || AccessibilityAction.EXPAND.equals(id) || AccessibilityAction.COLLAPSE.equals(id)
                || AccessibilityAction.SCROLL_FORWARD.equals(id) || AccessibilityAction.SCROLL_BACKWARD.equals(id)
                || AccessibilityAction.COPY.equals(id) || AccessibilityAction.CUT.equals(id)
                || AccessibilityAction.PASTE.equals(id) || AccessibilityAction.SHOW_ON_SCREEN.equals(id)
                || AccessibilityAction.SET_SELECTION.equals(id)
                || AccessibilityAction.MOVE_CURSOR_FORWARD_BY_CHARACTER.equals(id)
                || AccessibilityAction.MOVE_CURSOR_BACKWARD_BY_CHARACTER.equals(id)
                || AccessibilityAction.MOVE_CURSOR_FORWARD_BY_WORD.equals(id)
                || AccessibilityAction.MOVE_CURSOR_BACKWARD_BY_WORD.equals(id);
    }

    private static void error(List<AccessibilityIssue> issues, String code, String message,
            AccessibilityNodeSnapshot node) {
        issues.add(new AccessibilityIssue(AccessibilityIssue.Severity.ERROR, code, message, node.getId()));
    }

    private static void warning(List<AccessibilityIssue> issues, String code, String message,
            AccessibilityNodeSnapshot node) {
        issues.add(new AccessibilityIssue(AccessibilityIssue.Severity.WARNING, code, message, node.getId()));
    }

    private static boolean empty(String value) { return value == null || value.length() == 0; }
}
