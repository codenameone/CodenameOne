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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;

/// Two components side by side, or one above the other, with a divider
/// between them that the user drags.
///
/// The divider is drawn and dragged by this component itself. Layout
/// follows the desktop: until a location is set the first component gets
/// its preferred size, a resize hands the extra space out by the resize
/// weight, a location set by the program is limited only by the pane, and
/// a drag stops at the components' minimum sizes. After a layout
/// `getDividerLocation()` answers where the divider is. Without continuous
/// layout a drag shows a line and the components move on release.
///
/// The divider is as wide as `getDividerSize()` says; a press within a
/// few pixels of it also grabs it when no component there takes the
/// press, since a finger is wider than a mouse pointer. With one touch
/// expansion two arrows are drawn on the divider, and pressing one
/// collapses a side or brings the divider back.
///
/// Not supported: a look and feel divider component, key bindings, and
/// nested split panes sharing one border.
public class JSplitPane extends JComponent {

    public static final int VERTICAL_SPLIT = 0;
    public static final int HORIZONTAL_SPLIT = 1;
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";
    public static final String DIVIDER = "divider";
    public static final String ORIENTATION_PROPERTY = "orientation";
    public static final String CONTINUOUS_LAYOUT_PROPERTY = "continuousLayout";
    public static final String DIVIDER_SIZE_PROPERTY = "dividerSize";
    public static final String ONE_TOUCH_EXPANDABLE_PROPERTY = "oneTouchExpandable";
    public static final String LAST_DIVIDER_LOCATION_PROPERTY = "lastDividerLocation";
    public static final String DIVIDER_LOCATION_PROPERTY = "dividerLocation";
    public static final String RESIZE_WEIGHT_PROPERTY = "resizeWeight";

    private static final int GRAB_SLOP = 6;
    private static final int ARROW = 6;

    protected int orientation;
    protected boolean continuousLayout;
    protected Component leftComponent;
    protected Component rightComponent;
    protected int dividerSize = 10;
    protected boolean oneTouchExpandable;
    protected int lastDividerLocation;

    private double resizeWeight;
    private int dividerLocation = -1;

    /// Whether the location is to be applied by the next layout.
    private boolean locationPending;
    /// The size of the first component and the space both shared in the
    /// last layout; -1 before the first one.
    private int firstSize = -1;
    private int lastAvailable = -1;
    /// Where the divider is drawn: its offset along the split axis.
    private int dividerAt;

    private boolean dragging;
    private int dragOffset;
    private int dragLocation = -1;

    public JSplitPane() {
        this(HORIZONTAL_SPLIT, false, new JButton("left button"), new JButton("right button"));
    }

    public JSplitPane(int newOrientation) {
        this(newOrientation, false);
    }

    public JSplitPane(int newOrientation, boolean newContinuousLayout) {
        this(newOrientation, newContinuousLayout, null, null);
    }

    public JSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        this(newOrientation, false, newLeftComponent, newRightComponent);
    }

    public JSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent,
            Component newRightComponent) {
        if (newOrientation != HORIZONTAL_SPLIT && newOrientation != VERTICAL_SPLIT) {
            throw new IllegalArgumentException("cannot create JSplitPane, orientation must be one of "
                    + "JSplitPane.HORIZONTAL_SPLIT or JSplitPane.VERTICAL_SPLIT");
        }
        orientation = newOrientation;
        continuousLayout = newContinuousLayout;
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        if (newLeftComponent != null) {
            setLeftComponent(newLeftComponent);
        }
        if (newRightComponent != null) {
            setRightComponent(newRightComponent);
        }
        bind(KeyEvent.VK_LEFT, "negativeIncrement", Nudge.BACK);
        bind(KeyEvent.VK_UP, "negativeIncrement", Nudge.BACK);
        bind(KeyEvent.VK_RIGHT, "positiveIncrement", Nudge.FORWARD);
        bind(KeyEvent.VK_DOWN, "positiveIncrement", Nudge.FORWARD);
        bind(KeyEvent.VK_HOME, "selectMin", Nudge.MIN);
        bind(KeyEvent.VK_END, "selectMax", Nudge.MAX);
    }

    private void bind(int key, String name, int kind) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), name);
        if (getActionMap().get(name) == null) {
            getActionMap().put(name, new Nudge(this, kind));
        }
    }

    /// Moves the divider with the keyboard. As on the desktop the keys
    /// are the pane's only while the pane itself has the focus, which it
    /// gets from `requestFocusInWindow()`; a focused child keeps its
    /// arrow keys.
    private static final class Nudge extends AbstractAction {

        static final int BACK = 0;
        static final int FORWARD = 1;
        static final int MIN = 2;
        static final int MAX = 3;
        private static final int STEP = 10;

        private final JSplitPane pane;
        private final int kind;

        Nudge(JSplitPane pane, int kind) {
            this.pane = pane;
            this.kind = kind;
        }

        @Override
        public boolean isEnabled() {
            return pane.isFocusOwner();
        }

        @Override
        public void actionPerformed(com.codename1.desktopcompat.java.awt.event.ActionEvent e) {
            int min = pane.getMinimumDividerLocation();
            int max = pane.getMaximumDividerLocation();
            int at = pane.getDividerLocation();
            int to = kind == MIN ? min : kind == MAX ? max : kind == BACK ? at - STEP : at + STEP;
            pane.setDividerLocation(Math.max(min, Math.min(max, to)));
        }
    }

    // ------------------------------------------------------------ children

    public void setLeftComponent(Component comp) {
        if (comp == null) {
            if (leftComponent != null) {
                remove(leftComponent);
                leftComponent = null;
            }
        } else {
            add(comp, LEFT);
        }
    }

    public Component getLeftComponent() {
        return leftComponent;
    }

    public void setTopComponent(Component comp) {
        setLeftComponent(comp);
    }

    public Component getTopComponent() {
        return leftComponent;
    }

    public void setRightComponent(Component comp) {
        if (comp == null) {
            if (rightComponent != null) {
                remove(rightComponent);
                rightComponent = null;
            }
        } else {
            add(comp, RIGHT);
        }
    }

    public Component getRightComponent() {
        return rightComponent;
    }

    public void setBottomComponent(Component comp) {
        setRightComponent(comp);
    }

    public Component getBottomComponent() {
        return rightComponent;
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (constraints != null && !(constraints instanceof String)) {
            throw new IllegalArgumentException("cannot add to layout: constraint must be a string (or null)");
        }
        Object where = constraints;
        if (where == null) {
            if (leftComponent == null) {
                where = LEFT;
            } else if (rightComponent == null) {
                where = RIGHT;
            }
        }
        int at = index;
        if (LEFT.equals(where) || TOP.equals(where)) {
            Component toRemove = leftComponent;
            if (toRemove != null) {
                remove(toRemove);
            }
            leftComponent = comp;
            at = -1;
        } else if (RIGHT.equals(where) || BOTTOM.equals(where)) {
            Component toRemove = rightComponent;
            if (toRemove != null) {
                remove(toRemove);
            }
            rightComponent = comp;
            at = -1;
        }
        super.addImpl(comp, where, at);
        revalidate();
        repaint();
    }

    @Override
    public void remove(Component component) {
        if (component == leftComponent) {
            leftComponent = null;
        } else if (component == rightComponent) {
            rightComponent = null;
        }
        super.remove(component);
        revalidate();
        repaint();
    }

    @Override
    public void remove(int index) {
        Component comp = getComponent(index);
        if (comp == leftComponent) {
            leftComponent = null;
        } else if (comp == rightComponent) {
            rightComponent = null;
        }
        super.remove(index);
        revalidate();
        repaint();
    }

    @Override
    public void removeAll() {
        leftComponent = null;
        rightComponent = null;
        super.removeAll();
        revalidate();
        repaint();
    }

    @Override
    public boolean isValidateRoot() {
        return true;
    }

    // ------------------------------------------------------------ properties

    public void setDividerSize(int newSize) {
        int old = dividerSize;
        if (old != newSize) {
            dividerSize = newSize;
            firePropertyChange(DIVIDER_SIZE_PROPERTY, old, newSize);
            revalidate();
            repaint();
        }
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setOneTouchExpandable(boolean newValue) {
        boolean old = oneTouchExpandable;
        oneTouchExpandable = newValue;
        firePropertyChange(ONE_TOUCH_EXPANDABLE_PROPERTY, old, newValue);
        repaint();
    }

    public boolean isOneTouchExpandable() {
        return oneTouchExpandable;
    }

    public void setLastDividerLocation(int newLastLocation) {
        int old = lastDividerLocation;
        lastDividerLocation = newLastLocation;
        firePropertyChange(LAST_DIVIDER_LOCATION_PROPERTY, old, newLastLocation);
    }

    public int getLastDividerLocation() {
        return lastDividerLocation;
    }

    public void setOrientation(int orientation) {
        if (orientation != VERTICAL_SPLIT && orientation != HORIZONTAL_SPLIT) {
            throw new IllegalArgumentException("JSplitPane: orientation must be one of "
                    + "JSplitPane.VERTICAL_SPLIT or JSplitPane.HORIZONTAL_SPLIT");
        }
        int old = this.orientation;
        this.orientation = orientation;
        firePropertyChange(ORIENTATION_PROPERTY, old, orientation);
        if (old != orientation) {
            firstSize = -1;
            lastAvailable = -1;
            locationPending = dividerLocation >= 0;
            revalidate();
            repaint();
        }
    }

    public int getOrientation() {
        return orientation;
    }

    public void setContinuousLayout(boolean newContinuousLayout) {
        boolean old = continuousLayout;
        continuousLayout = newContinuousLayout;
        firePropertyChange(CONTINUOUS_LAYOUT_PROPERTY, old, newContinuousLayout);
    }

    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    public void setResizeWeight(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("JSplitPane weight must be between 0 and 1");
        }
        double old = resizeWeight;
        resizeWeight = value;
        firePropertyChange(RESIZE_WEIGHT_PROPERTY, Double.valueOf(old), Double.valueOf(value));
    }

    public double getResizeWeight() {
        return resizeWeight;
    }

    /// Forgets the divider location: the next layout gives the components
    /// their preferred sizes again.
    public void resetToPreferredSizes() {
        firstSize = -1;
        lastAvailable = -1;
        locationPending = false;
        dividerLocation = -1;
        revalidate();
        repaint();
    }

    public void setDividerLocation(double proportionalLocation) {
        if (proportionalLocation < 0.0 || proportionalLocation > 1.0) {
            throw new IllegalArgumentException("proportional location must be between 0.0 and 1.0.");
        }
        if (orientation == VERTICAL_SPLIT) {
            setDividerLocation((int) ((double) (getHeight() - getDividerSize()) * proportionalLocation));
        } else {
            setDividerLocation((int) ((double) (getWidth() - getDividerSize()) * proportionalLocation));
        }
    }

    public void setDividerLocation(int location) {
        int old = dividerLocation;
        dividerLocation = location;
        locationPending = location >= 0;
        if (location < 0) {
            firstSize = -1;
            lastAvailable = -1;
        }
        revalidate();
        repaint();
        firePropertyChange(DIVIDER_LOCATION_PROPERTY, old, location);
        setLastDividerLocation(old);
    }

    public int getDividerLocation() {
        return dividerLocation;
    }

    public int getMinimumDividerLocation() {
        int min = 0;
        if (leftComponent != null && leftComponent.isVisible()) {
            Insets in = getInsets();
            Dimension d = leftComponent.getMinimumSize();
            min = horizontal() ? d.width + in.left : d.height + in.top;
        }
        return min;
    }

    public int getMaximumDividerLocation() {
        int max = 0;
        if (rightComponent != null) {
            Insets in = getInsets();
            Dimension d = rightComponent.isVisible() ? rightComponent.getMinimumSize() : new Dimension(0, 0);
            if (horizontal()) {
                max = getWidth() - d.width - dividerSize - in.right;
            } else {
                max = getHeight() - d.height - dividerSize - in.bottom;
            }
        }
        return Math.max(getMinimumDividerLocation(), max);
    }

    // ------------------------------------------------------------ layout

    private boolean horizontal() {
        return orientation == HORIZONTAL_SPLIT;
    }

    private static int along(Dimension d, boolean horizontal) {
        return horizontal ? d.width : d.height;
    }

    private static boolean shown(Component c) {
        return c != null && c.isVisible();
    }

    /// Hands `space`, which may be negative, to the two sizes by the
    /// resize weight, keeping each at its minimum where both can be.
    private int[] distribute(int first, int second, int space) {
        boolean h = horizontal();
        boolean lv = shown(leftComponent);
        boolean rv = shown(rightComponent);
        int[] sizes = {first, second};
        if (lv && rv) {
            int lExtra = (int) (resizeWeight * (double) space);
            int rExtra = space - lExtra;
            sizes[0] += lExtra;
            sizes[1] += rExtra;
            int lMin = along(leftComponent.getMinimumSize(), h);
            int rMin = along(rightComponent.getMinimumSize(), h);
            boolean lOk = sizes[0] >= lMin;
            boolean rOk = sizes[1] >= rMin;
            if (!lOk && !rOk) {
                if (sizes[0] < 0) {
                    sizes[1] += sizes[0];
                    sizes[0] = 0;
                } else if (sizes[1] < 0) {
                    sizes[0] += sizes[1];
                    sizes[1] = 0;
                }
            } else if (!lOk) {
                if (sizes[1] - (lMin - sizes[0]) < rMin) {
                    if (sizes[0] < 0) {
                        sizes[1] += sizes[0];
                        sizes[0] = 0;
                    }
                } else {
                    sizes[1] -= lMin - sizes[0];
                    sizes[0] = lMin;
                }
            } else if (!rOk) {
                if (sizes[0] - (rMin - sizes[1]) < lMin) {
                    if (sizes[1] < 0) {
                        sizes[0] += sizes[1];
                        sizes[1] = 0;
                    }
                } else {
                    sizes[0] -= rMin - sizes[1];
                    sizes[1] = rMin;
                }
            }
            if (sizes[0] < 0) {
                sizes[0] = 0;
            }
            if (sizes[1] < 0) {
                sizes[1] = 0;
            }
        } else if (lv) {
            sizes[0] = Math.max(0, first + space);
        } else if (rv) {
            sizes[1] = Math.max(0, second + space);
        }
        return sizes;
    }

    @Override
    public void doLayout() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            // Not sized yet: nothing to share out, and nothing to remember.
            return;
        }
        boolean h = horizontal();
        Insets in = getInsets();
        int start = h ? in.left : in.top;
        int total = h ? getWidth() - in.left - in.right : getHeight() - in.top - in.bottom;
        int across = h ? getHeight() - in.top - in.bottom : getWidth() - in.left - in.right;
        int available = Math.max(0, total - dividerSize);
        boolean lv = shown(leftComponent);
        boolean rv = shown(rightComponent);
        int first;
        int second;
        if (locationPending) {
            int want = Math.max(0, Math.min(dividerLocation - start, available));
            if (lv && rv) {
                first = want;
                second = available - want;
            } else if (lv) {
                first = available;
                second = 0;
            } else {
                first = 0;
                second = rv ? available : 0;
            }
        } else if (firstSize < 0) {
            first = lv ? along(leftComponent.getPreferredSize(), h) : 0;
            second = rv ? along(rightComponent.getPreferredSize(), h) : 0;
            int[] sizes = distribute(first, second, available - first - second);
            first = sizes[0];
            second = sizes[1];
        } else if (available != lastAvailable) {
            int[] sizes = distribute(firstSize, Math.max(0, lastAvailable - firstSize), available - lastAvailable);
            first = sizes[0];
            second = sizes[1];
        } else {
            first = firstSize;
            second = Math.max(0, available - first);
        }
        locationPending = false;
        firstSize = first;
        lastAvailable = available;
        dividerAt = start + first;
        int secondAt = dividerAt + dividerSize;
        if (leftComponent != null) {
            if (h) {
                leftComponent.setBounds(start, in.top, first, Math.max(0, across));
            } else {
                leftComponent.setBounds(in.left, start, Math.max(0, across), first);
            }
        }
        if (rightComponent != null) {
            if (h) {
                rightComponent.setBounds(secondAt, in.top, second, Math.max(0, across));
            } else {
                rightComponent.setBounds(in.left, secondAt, Math.max(0, across), second);
            }
        }
        if (dividerLocation != dividerAt) {
            int old = dividerLocation;
            dividerLocation = dividerAt;
            firePropertyChange(DIVIDER_LOCATION_PROPERTY, old, dividerAt);
        }
    }

    private Dimension size(boolean minimum) {
        boolean h = horizontal();
        int main = 0;
        int cross = 0;
        Component[] both = {leftComponent, rightComponent};
        for (int i = 0; i < both.length; i++) {
            if (shown(both[i])) {
                Dimension d = minimum ? both[i].getMinimumSize() : both[i].getPreferredSize();
                main += along(d, h);
                cross = Math.max(cross, along(d, !h));
            }
        }
        main += dividerSize;
        Insets in = getInsets();
        if (h) {
            return new Dimension(main + in.left + in.right, cross + in.top + in.bottom);
        }
        return new Dimension(cross + in.left + in.right, main + in.top + in.bottom);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return size(false);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return size(true);
    }

    // ------------------------------------------------------------ painting

    private static void triangle(Graphics g, int cx, int cy, int dx, int dy) {
        int s = ARROW / 2;
        int[] xs;
        int[] ys;
        if (dx != 0) {
            xs = new int[]{cx + dx * s, cx - dx * s, cx - dx * s};
            ys = new int[]{cy, cy - s, cy + s};
        } else {
            xs = new int[]{cx, cx - s, cx + s};
            ys = new int[]{cy + dy * s, cy - dy * s, cy - dy * s};
        }
        g.fillPolygon(xs, ys, 3);
    }

    private Color dividerColor() {
        Color bg = getBackground();
        if (bg == null) {
            return Color.LIGHT_GRAY;
        }
        int lum = (bg.getRed() * 3 + bg.getGreen() * 6 + bg.getBlue()) / 10;
        return lum > 128 ? bg.darker() : bg.brighter();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dividerSize <= 0) {
            return;
        }
        boolean h = horizontal();
        Insets in = getInsets();
        Color c = dividerColor();
        g.setColor(c);
        int x = h ? dividerAt : in.left;
        int y = h ? in.top : dividerAt;
        int w = h ? dividerSize : getWidth() - in.left - in.right;
        int hh = h ? getHeight() - in.top - in.bottom : dividerSize;
        g.fillRect(x, y, w, hh);
        int lum = (c.getRed() * 3 + c.getGreen() * 6 + c.getBlue()) / 10;
        g.setColor(lum > 128 ? c.darker().darker() : c.brighter().brighter());
        // A grip in the middle of the divider.
        int mx = x + w / 2;
        int my = y + hh / 2;
        for (int i = -1; i <= 1; i++) {
            if (h) {
                g.fillRect(mx - 1, my + i * 6 - 1, 2, 2);
            } else {
                g.fillRect(mx + i * 6 - 1, my - 1, 2, 2);
            }
        }
        if (oneTouchExpandable) {
            if (h) {
                triangle(g, mx, y + 2 + ARROW / 2, -1, 0);
                triangle(g, mx, y + 4 + ARROW + ARROW / 2, 1, 0);
            } else {
                triangle(g, x + 2 + ARROW / 2, my, 0, -1);
                triangle(g, x + 4 + ARROW + ARROW / 2, my, 0, 1);
            }
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (dragging && !continuousLayout && dragLocation >= 0) {
            g.setColor(Color.DARK_GRAY);
            if (horizontal()) {
                g.fillRect(dragLocation, 0, Math.max(1, dividerSize), getHeight());
            } else {
                g.fillRect(0, dragLocation, getWidth(), Math.max(1, dividerSize));
            }
        }
    }

    // ------------------------------------------------------------ dragging

    /// Which one touch arrow is at a point of the divider: -1 for the one
    /// toward the first component, 1 for the other, 0 for neither.
    private int arrowAt(int x, int y) {
        if (!oneTouchExpandable) {
            return 0;
        }
        Insets in = getInsets();
        int p = horizontal() ? y - in.top : x - in.left;
        if (p >= 0 && p < 3 + ARROW) {
            return -1;
        }
        if (p >= 3 + ARROW && p < 6 + 2 * ARROW) {
            return 1;
        }
        return 0;
    }

    private void oneTouch(int arrow) {
        Insets in = getInsets();
        boolean h = horizontal();
        int start = h ? in.left : in.top;
        int end = (h ? getWidth() - in.right : getHeight() - in.bottom) - dividerSize;
        int current = dividerAt;
        int max = getMaximumDividerLocation();
        int to;
        if (arrow < 0) {
            to = current >= end ? Math.min(lastDividerLocation, max) : start;
        } else {
            to = current <= start ? Math.min(lastDividerLocation, max) : end;
        }
        if (to != current) {
            setDividerLocation(Math.max(start, to));
            validate();
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        boolean h = horizontal();
        int p = h ? e.getX() : e.getY();
        if (e.getID() == MouseEvent.MOUSE_PRESSED && isEnabled()) {
            if (p >= dividerAt - GRAB_SLOP && p < dividerAt + dividerSize + GRAB_SLOP) {
                int arrow = p >= dividerAt && p < dividerAt + dividerSize ? arrowAt(e.getX(), e.getY()) : 0;
                if (arrow != 0) {
                    oneTouch(arrow);
                } else {
                    dragging = true;
                    dragOffset = p - dividerAt;
                    dragLocation = dividerAt;
                }
            }
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED && dragging) {
            dragging = false;
            int to = dragLocation;
            dragLocation = -1;
            if (!continuousLayout && to >= 0 && to != dividerAt) {
                setDividerLocation(to);
                validate();
            }
            repaint();
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_DRAGGED && dragging) {
            int p = horizontal() ? e.getX() : e.getY();
            int to = Math.max(getMinimumDividerLocation(), Math.min(getMaximumDividerLocation(), p - dragOffset));
            if (to != dragLocation) {
                dragLocation = to;
                if (continuousLayout) {
                    setDividerLocation(to);
                    validate();
                } else {
                    repaint();
                }
            }
        }
        super.processMouseMotionEvent(e);
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",orientation=" + (horizontal() ? "HORIZONTAL_SPLIT" : "VERTICAL_SPLIT")
                + ",dividerSize=" + dividerSize;
    }
}
