/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.impl.javase;

import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityCheckedState;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityRange;
import com.codename1.ui.accessibility.AccessibilityRole;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.JPanel;

/** Swing/Java Access Bridge projection of the lightweight semantic tree. */
final class JavaSEAccessibility {
    private final JPanel canvas;
    private final JavaSEPort implementation;
    private final RootContext root = new RootContext();
    private final Map<Long, VirtualAccessible> cache = new HashMap<Long, VirtualAccessible>();

    JavaSEAccessibility(JPanel canvas, JavaSEPort implementation) {
        this.canvas = canvas;
        this.implementation = implementation;
    }

    AccessibleContext getContext() {
        return root;
    }

    void changed(int changeType) {
        root.firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY, null,
                Integer.valueOf(changeType));
    }

    private AccessibilityTreeSnapshot tree() {
        return implementation.getAccessibilityTreeSnapshot();
    }

    private VirtualAccessible accessible(long id) {
        VirtualAccessible value = cache.get(Long.valueOf(id));
        if (value == null) {
            value = new VirtualAccessible(id);
            cache.put(Long.valueOf(id), value);
        }
        return value;
    }

    private final class RootContext extends AccessibleContext implements AccessibleComponent {
        RootContext() {
            setAccessibleParent(canvas);
        }

        public String getAccessibleName() { return "Codename One application"; }
        public String getAccessibleDescription() { return "Lightweight component accessibility tree"; }
        public AccessibleRole getAccessibleRole() { return AccessibleRole.PANEL; }
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = new AccessibleStateSet();
            if (canvas.isEnabled()) states.add(AccessibleState.ENABLED);
            if (canvas.isVisible()) states.add(AccessibleState.VISIBLE);
            if (canvas.isShowing()) states.add(AccessibleState.SHOWING);
            states.add(AccessibleState.FOCUSABLE);
            if (canvas.hasFocus()) states.add(AccessibleState.FOCUSED);
            return states;
        }
        public int getAccessibleIndexInParent() { return -1; }
        public int getAccessibleChildrenCount() { return tree().getRootIds().size(); }
        public Accessible getAccessibleChild(int i) {
            List<Long> roots = tree().getRootIds();
            return i < 0 || i >= roots.size() ? null : accessible(roots.get(i).longValue());
        }
        public Locale getLocale() { return Locale.getDefault(); }
        public AccessibleComponent getAccessibleComponent() { return this; }
        public Color getBackground() { return canvas.getBackground(); }
        public void setBackground(Color c) { canvas.setBackground(c); }
        public Color getForeground() { return canvas.getForeground(); }
        public void setForeground(Color c) { canvas.setForeground(c); }
        public Cursor getCursor() { return canvas.getCursor(); }
        public void setCursor(Cursor c) { canvas.setCursor(c); }
        public Font getFont() { return canvas.getFont(); }
        public void setFont(Font f) { canvas.setFont(f); }
        public FontMetrics getFontMetrics(Font f) { return canvas.getFontMetrics(f); }
        public boolean isEnabled() { return canvas.isEnabled(); }
        public void setEnabled(boolean b) { canvas.setEnabled(b); }
        public boolean isVisible() { return canvas.isVisible(); }
        public void setVisible(boolean b) { canvas.setVisible(b); }
        public boolean isShowing() { return canvas.isShowing(); }
        public boolean contains(Point p) { return canvas.contains(p); }
        public Point getLocationOnScreen() { return canvas.getLocationOnScreen(); }
        public Point getLocation() { return canvas.getLocation(); }
        public void setLocation(Point p) { canvas.setLocation(p); }
        public Rectangle getBounds() { return canvas.getBounds(); }
        public void setBounds(Rectangle r) { canvas.setBounds(r); }
        public Dimension getSize() { return canvas.getSize(); }
        public void setSize(Dimension d) { canvas.setSize(d); }
        public Accessible getAccessibleAt(Point p) {
            Point local = new Point(p);
            AccessibilityNodeSnapshot node = tree().getNodeAt(local.x, local.y);
            return node == null ? null : accessible(node.getId());
        }
        public boolean isFocusTraversable() { return true; }
        public void requestFocus() { canvas.requestFocus(); }
        public void addFocusListener(FocusListener l) { canvas.addFocusListener(l); }
        public void removeFocusListener(FocusListener l) { canvas.removeFocusListener(l); }
    }

    private final class VirtualAccessible implements Accessible {
        private final long id;
        private final NodeContext context;

        VirtualAccessible(long id) {
            this.id = id;
            context = new NodeContext();
        }

        public AccessibleContext getAccessibleContext() { return context; }

        private AccessibilityNodeSnapshot node() { return tree().getNode(id); }

        private final class NodeContext extends AccessibleContext
                implements AccessibleComponent, AccessibleAction, AccessibleValue {
            NodeContext() {
                setAccessibleParent(canvas);
            }

            public String getAccessibleName() { return node() == null ? null : node().getLabel(); }
            public void setAccessibleName(String name) { }
            public String getAccessibleDescription() {
                AccessibilityNodeSnapshot n = node();
                if (n == null) return null;
                String value = n.getDescription();
                if (n.getHint() != null) value = value == null ? n.getHint() : value + ". " + n.getHint();
                if (n.getValidationError() != null) value = value == null ? n.getValidationError() : value + ". " + n.getValidationError();
                return value;
            }
            public void setAccessibleDescription(String description) { }
            public AccessibleRole getAccessibleRole() { return role(node() == null ? AccessibilityRole.GENERIC : node().getRole()); }
            public AccessibleStateSet getAccessibleStateSet() { return states(node()); }
            public Accessible getAccessibleParent() {
                AccessibilityNodeSnapshot n = node();
                return n == null || n.getParentId() < 0 ? canvas : accessible(n.getParentId());
            }
            public int getAccessibleIndexInParent() {
                AccessibilityNodeSnapshot n = node();
                if (n == null) return -1;
                List<Long> siblings = n.getParentId() < 0 ? tree().getRootIds()
                        : tree().getNode(n.getParentId()).getChildIds();
                return siblings.indexOf(Long.valueOf(id));
            }
            public int getAccessibleChildrenCount() { return node() == null ? 0 : node().getChildIds().size(); }
            public Accessible getAccessibleChild(int i) {
                AccessibilityNodeSnapshot n = node();
                if (n == null || i < 0 || i >= n.getChildIds().size()) return null;
                return accessible(n.getChildIds().get(i).longValue());
            }
            public Locale getLocale() { return Locale.getDefault(); }
            public AccessibleComponent getAccessibleComponent() { return this; }
            public AccessibleAction getAccessibleAction() { return node() != null && !node().getActions().isEmpty() ? this : null; }
            public AccessibleValue getAccessibleValue() { return node() != null && node().getRange() != null ? this : null; }
            public int getAccessibleActionCount() { return node() == null ? 0 : node().getActions().size(); }
            public String getAccessibleActionDescription(int i) {
                if (node() == null || i < 0 || i >= node().getActions().size()) return null;
                AccessibilityAction action = node().getActions().get(i);
                return action.getLabel() == null ? action.getId() : action.getLabel();
            }
            public boolean doAccessibleAction(int i) {
                if (node() == null || i < 0 || i >= node().getActions().size()) return false;
                return implementation.performAccessibilityAction(id, node().getActions().get(i).getId(), null);
            }
            public Number getCurrentAccessibleValue() { return node() == null || node().getRange() == null ? null : Double.valueOf(node().getRange().getCurrent()); }
            public boolean setCurrentAccessibleValue(Number value) {
                return value != null && implementation.performAccessibilityAction(id, AccessibilityAction.SET_VALUE, value);
            }
            public Number getMinimumAccessibleValue() { return node() == null || node().getRange() == null ? null : Double.valueOf(node().getRange().getMinimum()); }
            public Number getMaximumAccessibleValue() { return node() == null || node().getRange() == null ? null : Double.valueOf(node().getRange().getMaximum()); }
            public Color getBackground() { return canvas.getBackground(); }
            public void setBackground(Color c) { }
            public Color getForeground() { return canvas.getForeground(); }
            public void setForeground(Color c) { }
            public Cursor getCursor() { return canvas.getCursor(); }
            public void setCursor(Cursor c) { }
            public Font getFont() { return canvas.getFont(); }
            public void setFont(Font f) { }
            public FontMetrics getFontMetrics(Font f) { return canvas.getFontMetrics(f); }
            public boolean isEnabled() { return node() != null && !Boolean.FALSE.equals(node().getEnabled()); }
            public void setEnabled(boolean b) { }
            public boolean isVisible() { return node() != null && node().getBounds().getWidth() > 0 && node().getBounds().getHeight() > 0; }
            public void setVisible(boolean b) { }
            public boolean isShowing() { return canvas.isShowing() && isVisible(); }
            public boolean contains(Point p) { return getBounds().contains(p); }
            public Point getLocationOnScreen() {
                Point rootLocation = canvas.getLocationOnScreen();
                Rectangle bounds = getBounds();
                return new Point(rootLocation.x + bounds.x, rootLocation.y + bounds.y);
            }
            public Point getLocation() { Rectangle b = getBounds(); return new Point(b.x, b.y); }
            public void setLocation(Point p) { }
            public Rectangle getBounds() {
                if (node() == null) return new Rectangle();
                com.codename1.ui.geom.Rectangle b = node().getBounds();
                return new Rectangle(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
            public void setBounds(Rectangle r) { }
            public Dimension getSize() { Rectangle b = getBounds(); return new Dimension(b.width, b.height); }
            public void setSize(Dimension d) { }
            public Accessible getAccessibleAt(Point p) {
                AccessibilityNodeSnapshot n = tree().getNodeAt(p.x, p.y);
                return n == null ? null : accessible(n.getId());
            }
            public boolean isFocusTraversable() { return node() != null && node().isFocusable(); }
            public void requestFocus() { implementation.performAccessibilityAction(id, AccessibilityAction.FOCUS, null); }
            public void addFocusListener(FocusListener l) { canvas.addFocusListener(l); }
            public void removeFocusListener(FocusListener l) { canvas.removeFocusListener(l); }
        }
    }

    private AccessibleStateSet states(AccessibilityNodeSnapshot node) {
        AccessibleStateSet states = new AccessibleStateSet();
        if (node == null) return states;
        if (!Boolean.FALSE.equals(node.getEnabled())) states.add(AccessibleState.ENABLED);
        if (node.getBounds().getWidth() > 0 && node.getBounds().getHeight() > 0) {
            states.add(AccessibleState.VISIBLE);
            if (canvas.isShowing()) states.add(AccessibleState.SHOWING);
        }
        if (node.isFocusable()) states.add(AccessibleState.FOCUSABLE);
        if (node.isFocused()) states.add(AccessibleState.FOCUSED);
        if (Boolean.TRUE.equals(node.getSelected())) states.add(AccessibleState.SELECTED);
        if (node.getChecked() == AccessibilityCheckedState.CHECKED) states.add(AccessibleState.CHECKED);
        if (Boolean.TRUE.equals(node.getExpanded())) states.add(AccessibleState.EXPANDED);
        if (Boolean.FALSE.equals(node.getExpanded())) states.add(AccessibleState.COLLAPSED);
        if (Boolean.TRUE.equals(node.getBusy())) states.add(AccessibleState.BUSY);
        if (!Boolean.TRUE.equals(node.getReadOnly()) && (node.getRole() == AccessibilityRole.TEXT_FIELD
                || node.getRole() == AccessibilityRole.SEARCH_FIELD)) states.add(AccessibleState.EDITABLE);
        if (Boolean.TRUE.equals(node.getMultiline())) states.add(AccessibleState.MULTI_LINE);
        return states;
    }

    private AccessibleRole role(AccessibilityRole role) {
        switch (role) {
            case BUTTON: return AccessibleRole.PUSH_BUTTON;
            case TOGGLE_BUTTON: return AccessibleRole.TOGGLE_BUTTON;
            case CHECKBOX: return AccessibleRole.CHECK_BOX;
            case RADIO_BUTTON: return AccessibleRole.RADIO_BUTTON;
            case SWITCH: return AccessibleRole.TOGGLE_BUTTON;
            case HEADING:
            case STATIC_TEXT: return AccessibleRole.LABEL;
            case LINK: return AccessibleRole.HYPERLINK;
            case IMAGE: return AccessibleRole.ICON;
            case TEXT_FIELD:
            case SEARCH_FIELD: return AccessibleRole.TEXT;
            case SLIDER: return AccessibleRole.SLIDER;
            case PROGRESS_BAR: return AccessibleRole.PROGRESS_BAR;
            case LIST: return AccessibleRole.LIST;
            case LIST_ITEM: return AccessibleRole.LIST_ITEM;
            case GRID: return AccessibleRole.TABLE;
            case CELL: return AccessibleRole.UNKNOWN;
            case TAB_LIST: return AccessibleRole.PAGE_TAB_LIST;
            case TAB: return AccessibleRole.PAGE_TAB;
            case DIALOG: return AccessibleRole.DIALOG;
            case ALERT: return AccessibleRole.ALERT;
            case MENU: return AccessibleRole.MENU;
            case MENU_ITEM: return AccessibleRole.MENU_ITEM;
            case TOOLBAR: return AccessibleRole.TOOL_BAR;
            case SCROLL_BAR: return AccessibleRole.SCROLL_BAR;
            case COMBO_BOX: return AccessibleRole.COMBO_BOX;
            case TREE: return AccessibleRole.TREE;
            case SEPARATOR: return AccessibleRole.SEPARATOR;
            default: return AccessibleRole.PANEL;
        }
    }
}
