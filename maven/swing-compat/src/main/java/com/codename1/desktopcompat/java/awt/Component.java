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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.rt.AwtListeners;
import com.codename1.desktopcompat.java.awt.dnd.DropTarget;
import com.codename1.desktopcompat.java.awt.event.ComponentEvent;
import com.codename1.desktopcompat.java.awt.event.ComponentListener;
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.java.awt.event.FocusListener;
import com.codename1.desktopcompat.java.awt.event.HierarchyEvent;
import com.codename1.desktopcompat.java.awt.event.HierarchyListener;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.KeyListener;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseListener;
import com.codename1.desktopcompat.java.awt.event.MouseMotionListener;
import com.codename1.desktopcompat.java.awt.event.MouseWheelEvent;
import com.codename1.desktopcompat.java.awt.event.MouseWheelListener;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import com.codename1.desktopcompat.java.awt.image.ImageObserver;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeSupport;
import com.codename1.desktopcompat.rt.CanvasPeer;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.G2D;
import com.codename1.desktopcompat.rt.Peer;
import com.codename1.desktopcompat.rt.PeerSupport;
import com.codename1.desktopcompat.rt.Units;
import java.util.ArrayList;

/// The base of every AWT and Swing component.
///
/// A component keeps its own state -- bounds in logical pixels, colors,
/// font, listeners -- and is shown through one Codename One component, its
/// peer, which is made the first time something asks for it. The members
/// whose names start with `cn1` are the contract between a component and
/// its peer; they are not part of the desktop API.
///
/// Not supported: `getGraphics()` answers `null` (paint from `paint` or
/// `paintComponent` and ask for it with `repaint`), the component
/// orientation is recorded and nothing more, and there is no drag and
/// drop and no input methods.
public abstract class Component implements ImageObserver {

    public static final float TOP_ALIGNMENT = 0.0f;
    public static final float CENTER_ALIGNMENT = 0.5f;
    public static final float BOTTOM_ALIGNMENT = 1.0f;
    public static final float LEFT_ALIGNMENT = 0.0f;
    public static final float RIGHT_ALIGNMENT = 1.0f;

    private static final Object TREE_LOCK = new Object();

    private int x;
    private int y;
    private int width;
    private int height;
    private Container parent;
    private String name;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean valid;
    private boolean displayable;
    private boolean focusable = true;
    private boolean ignoreRepaint;
    private Color foreground;
    private Color background;
    private Font font;
    private Cursor cursor;
    private boolean focusTraversalKeys = true;
    private ComponentOrientation orientation = ComponentOrientation.UNKNOWN;
    private Dimension prefSize;
    private Dimension minSize;
    private Dimension maxSize;
    private long eventMask;
    private com.codename1.ui.Component peer;
    private PropertyChangeSupport changeSupport;
    private DropTarget dropTarget;
    private ArrayList<ComponentListener> componentListeners;
    private ArrayList<FocusListener> focusListeners;
    private ArrayList<HierarchyListener> hierarchyListeners;
    /// How many hierarchy listeners all components have together: a tree
    /// is walked to tell them of a change only when there is one.
    private static int hierarchyListenerCount;
    private ArrayList<KeyListener> keyListeners;
    private ArrayList<MouseListener> mouseListeners;
    private ArrayList<MouseMotionListener> mouseMotionListeners;
    private ArrayList<MouseWheelListener> mouseWheelListeners;

    protected Component() {
    }

    // ------------------------------------------------------------ peer

    /// The Codename One component that shows this one, made on first use.
    public final com.codename1.ui.Component cn1Peer() {
        if (peer == null) {
            peer = cn1CreatePeer();
            cn1SyncPeerBounds();
            peer.setVisible(visible);
            peer.setEnabled(enabled);
            cn1PeerCreated();
        }
        return peer;
    }

    /// Gives up the peer and makes a new one in its place, for a component
    /// whose kind of peer follows a property that changed after the first
    /// was made. Nothing happens while there is no peer yet.
    protected final void cn1RecreatePeer() {
        com.codename1.ui.Component old = peer;
        if (old == null) {
            return;
        }
        com.codename1.ui.Container host = old.getParent();
        peer = null;
        com.codename1.ui.Component now = cn1Peer();
        if (host != null) {
            int at = host.getComponentIndex(old);
            host.removeComponent(old);
            host.addComponent(Math.max(0, Math.min(at, host.getComponentCount())), now);
        }
    }

    /// The peer if it was made already, else `null`.
    public final com.codename1.ui.Component cn1PeerOrNull() {
        return peer;
    }

    /// Makes the peer. A widget overrides this to answer the Codename One
    /// widget that backs it; the answer must implement
    /// [com.codename1.desktopcompat.rt.Peer]. The default is a blank canvas.
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new CanvasPeer(this);
    }

    /// Called once, right after the peer was made and given this
    /// component's bounds, visibility and enabled state.
    protected void cn1PeerCreated() {
        cn1PeerSupportApply();
    }

    private void cn1PeerSupportApply() {
        if (peer instanceof Peer) {
            ((Peer) peer).support().applyStyle();
        }
    }

    /// Pushes this component's bounds to its peer, snapped to device pixels.
    /// A window overrides it with nothing: its host places it.
    public void cn1SyncPeerBounds() {
        if (peer != null) {
            peer.setX(Units.toDevice(x));
            peer.setY(Units.toDevice(y));
            peer.setWidth(Units.toDeviceSize(x, width));
            peer.setHeight(Units.toDeviceSize(y, height));
        }
    }

    /// Tells a component whose peer is placed by Codename One -- the root
    /// of a peer tree -- the size its peer was given, in logical pixels.
    public void cn1PeerResized(int w, int h) {
        if (w != width || h != height) {
            width = w;
            height = h;
            invalidate();
            fireComponentEvent(ComponentEvent.COMPONENT_RESIZED);
        }
    }

    /// The preferred size of the Codename One widget behind a component
    /// that has a native look, in logical pixels; `null` for the others.
    protected Dimension cn1NativePreferredSize() {
        if (!com.codename1.ui.Display.isInitialized()) {
            return null;
        }
        com.codename1.ui.Component p = cn1Peer();
        if (p instanceof Peer && ((Peer) p).nativeLook()) {
            return new Dimension(Units.toLogicalCeil(p.getPreferredW()), Units.toLogicalCeil(p.getPreferredH()));
        }
        return null;
    }

    /// Paints the Codename One widget behind this component into `g`, at
    /// the origin, and answers `true`; answers `false` and paints nothing
    /// when the component has no native look, there is no display, or `g`
    /// is not a graphics of this layer.
    public boolean cn1PaintNative(Graphics g) {
        if (!(g instanceof G2D) || !com.codename1.ui.Display.isInitialized()) {
            return false;
        }
        com.codename1.ui.Component p = cn1Peer();
        if (p instanceof Peer && ((Peer) p).nativeLook()) {
            ((Peer) p).support().paintNative((G2D) g);
            return true;
        }
        return false;
    }

    /// Whether mouse events over this component are delivered to it rather
    /// than to an ancestor: it has a mouse, mouse motion or mouse wheel
    /// listener, or enabled those events.
    public boolean cn1WantsMouse() {
        return notEmpty(mouseListeners) || notEmpty(mouseMotionListeners) || notEmpty(mouseWheelListeners)
                || (eventMask & (AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK)) != 0;
    }

    private static boolean notEmpty(ArrayList<?> l) {
        return l != null && !l.isEmpty();
    }

    /// The configuration of the one screen.
    public GraphicsConfiguration getGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    void cn1SetParent(Container p) {
        parent = p;
    }

    /// Whether the tab key stops at this component: it is shown by a
    /// Codename One widget that takes the focus, or has a key or a focus
    /// listener. Swing components add their own key bindings to that.
    public boolean cn1FocusTraversable() {
        if (notEmpty(keyListeners) || notEmpty(focusListeners)) {
            return true;
        }
        return peer instanceof Peer && ((Peer) peer).nativeLook() && peer.isFocusable();
    }

    /// Gives the peer the pointer shape this component has, its own or
    /// the one it inherits. Nothing happens on a port that cannot change
    /// the pointer's shape, and for a custom cursor.
    public void cn1ApplyCursor() {
        if (peer != null && com.codename1.ui.Display.isInitialized()
                && com.codename1.ui.Component.isSetCursorSupported()) {
            int type = getCursor().getType();
            peer.setCursor(type >= Cursor.DEFAULT_CURSOR && type <= Cursor.MOVE_CURSOR ? type
                    : Cursor.DEFAULT_CURSOR);
        }
    }

    // ------------------------------------------------------------ basics

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String old = this.name;
        this.name = name;
        firePropertyChange("name", old, name);
    }

    public Container getParent() {
        return parent;
    }

    public Toolkit getToolkit() {
        return Toolkit.getDefaultToolkit();
    }

    /// A single object shared by every component. Nothing in this layer
    /// locks it: the component tree belongs to the event dispatch thread.
    public final Object getTreeLock() {
        return TREE_LOCK;
    }

    public boolean isValid() {
        return valid && displayable;
    }

    /// Whether the component is part of a window that was shown or packed.
    public boolean isDisplayable() {
        return displayable;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isShowing() {
        return visible && displayable && parent != null && parent.isShowing();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean b) {
        if (enabled != b) {
            enabled = b;
            if (peer != null) {
                peer.setEnabled(b);
            }
            firePropertyChange("enabled", !b, b);
            repaint();
        }
    }

    public void setVisible(boolean b) {
        if (visible != b) {
            boolean was = isShowing();
            visible = b;
            if (peer != null) {
                peer.setVisible(b);
            }
            if (was != isShowing() && cn1HierarchyHeard()) {
                cn1HierarchyChanged(this, parent, HierarchyEvent.SHOWING_CHANGED);
            }
            fireComponentEvent(b ? ComponentEvent.COMPONENT_SHOWN : ComponentEvent.COMPONENT_HIDDEN);
            if (parent != null) {
                parent.invalidate();
                PeerSupport.scheduleLayout(this);
            }
        }
    }

    /// Always `true`: every component of this layer is drawn by its parent.
    public boolean isLightweight() {
        return true;
    }

    public boolean isOpaque() {
        return false;
    }

    public boolean isDoubleBuffered() {
        return false;
    }

    // ------------------------------------------------------------ look

    public Color getForeground() {
        if (foreground != null) {
            return foreground;
        }
        return parent != null ? parent.getForeground() : null;
    }

    public void setForeground(Color c) {
        Color old = foreground;
        foreground = c;
        cn1PeerSupportApply();
        firePropertyChange("foreground", old, c);
        repaint();
    }

    public boolean isForegroundSet() {
        return foreground != null;
    }

    public Color getBackground() {
        if (background != null) {
            return background;
        }
        return parent != null ? parent.getBackground() : null;
    }

    public void setBackground(Color c) {
        Color old = background;
        background = c;
        cn1PeerSupportApply();
        firePropertyChange("background", old, c);
        repaint();
    }

    public boolean isBackgroundSet() {
        return background != null;
    }

    public Font getFont() {
        if (font != null) {
            return font;
        }
        return parent != null ? parent.getFont() : null;
    }

    public void setFont(Font f) {
        Font old = font;
        font = f;
        cn1PeerSupportApply();
        firePropertyChange("font", old, f);
        if (f != old && (old == null || !old.equals(f))) {
            invalidate();
        }
    }

    public boolean isFontSet() {
        return font != null;
    }

    /// Sets the pointer's shape over this component and the children that
    /// set none of their own. The shape changes where the port can change
    /// it -- the desktop ports -- and is only recorded elsewhere; a custom
    /// cursor shows as the default one.
    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
        cn1ApplyCursor();
    }

    public ComponentOrientation getComponentOrientation() {
        return orientation;
    }

    /// Recorded only: layout does not mirror for right to left.
    public void setComponentOrientation(ComponentOrientation o) {
        ComponentOrientation old = orientation;
        orientation = o;
        firePropertyChange("componentOrientation", old, o);
    }

    public void applyComponentOrientation(ComponentOrientation orientation) {
        if (orientation == null) {
            throw new NullPointerException();
        }
        setComponentOrientation(orientation);
    }

    public Cursor getCursor() {
        if (cursor != null) {
            return cursor;
        }
        return parent != null ? parent.getCursor() : Cursor.getDefaultCursor();
    }

    public boolean isCursorSet() {
        return cursor != null;
    }

    public FontMetrics getFontMetrics(Font font) {
        return Fonts.metrics(font);
    }

    // ------------------------------------------------------------ bounds

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public Point getLocation(Point rv) {
        if (rv == null) {
            return new Point(x, y);
        }
        rv.setLocation(x, y);
        return rv;
    }

    public void setLocation(int x, int y) {
        setBounds(x, y, width, height);
    }

    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    public Dimension getSize(Dimension rv) {
        if (rv == null) {
            return new Dimension(width, height);
        }
        rv.setSize(width, height);
        return rv;
    }

    public void setSize(int width, int height) {
        setBounds(x, y, width, height);
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public Rectangle getBounds(Rectangle rv) {
        if (rv == null) {
            return new Rectangle(x, y, width, height);
        }
        rv.setBounds(x, y, width, height);
        return rv;
    }

    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void setBounds(int x, int y, int width, int height) {
        boolean resized = this.width != width || this.height != height;
        boolean moved = this.x != x || this.y != y;
        if (!resized && !moved) {
            return;
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        cn1SyncPeerBounds();
        if (parent != null && !parent.cn1InLayout() && peer != null) {
            // Moved or resized by the application, not by a layout pass
            // that repaints when it is done: a frame dragged about a
            // desktop pane, a piece of a game moved by a timer. Codename
            // One does not repaint for a peer that changed place, so
            // without this the screen keeps showing it where it was.
            parent.repaint();
        }
        if (resized) {
            invalidate();
            fireComponentEvent(ComponentEvent.COMPONENT_RESIZED);
        }
        if (moved) {
            fireComponentEvent(ComponentEvent.COMPONENT_MOVED);
        }
    }

    public Point getLocationOnScreen() {
        Point p = parent != null ? parent.getLocationOnScreen() : new Point(0, 0);
        p.translate(x, y);
        return p;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    public Component getComponentAt(int x, int y) {
        return contains(x, y) ? this : null;
    }

    public Component getComponentAt(Point p) {
        return getComponentAt(p.x, p.y);
    }

    // ------------------------------------------------------------ sizes

    public void setPreferredSize(Dimension preferredSize) {
        Dimension old = prefSize;
        prefSize = preferredSize == null ? null : new Dimension(preferredSize);
        firePropertyChange("preferredSize", old, preferredSize);
    }

    public boolean isPreferredSizeSet() {
        return prefSize != null;
    }

    /// The size set with `setPreferredSize`; else the preferred size of
    /// the Codename One widget behind a component with a native look; else
    /// the minimum size.
    public Dimension getPreferredSize() {
        if (prefSize != null) {
            return new Dimension(prefSize);
        }
        Dimension d = cn1NativePreferredSize();
        return d != null ? d : getMinimumSize();
    }

    public void setMinimumSize(Dimension minimumSize) {
        Dimension old = minSize;
        minSize = minimumSize == null ? null : new Dimension(minimumSize);
        firePropertyChange("minimumSize", old, minimumSize);
    }

    public boolean isMinimumSizeSet() {
        return minSize != null;
    }

    public Dimension getMinimumSize() {
        if (minSize != null) {
            return new Dimension(minSize);
        }
        Dimension d = cn1NativePreferredSize();
        if (d != null) {
            return d;
        }
        // The desktop's answer is the size of the moment. Asked how small
        // the window can get, that would be the size it was last given.
        return com.codename1.desktopcompat.rt.RootPan.measuring() ? new Dimension(0, 0)
                : new Dimension(width, height);
    }

    public void setMaximumSize(Dimension maximumSize) {
        Dimension old = maxSize;
        maxSize = maximumSize == null ? null : new Dimension(maximumSize);
        firePropertyChange("maximumSize", old, maximumSize);
    }

    public boolean isMaximumSizeSet() {
        return maxSize != null;
    }

    public Dimension getMaximumSize() {
        if (maxSize != null) {
            return new Dimension(maxSize);
        }
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    public float getAlignmentX() {
        return CENTER_ALIGNMENT;
    }

    public float getAlignmentY() {
        return CENTER_ALIGNMENT;
    }

    public int getBaseline(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be >= 0");
        }
        return -1;
    }

    // ------------------------------------------------------------ layout

    public void doLayout() {
    }

    public void validate() {
        valid = true;
    }

    public void invalidate() {
        valid = false;
        if (peer != null) {
            peer.setShouldCalcPreferredSize(true);
        }
        if (parent != null && parent.cn1Valid()) {
            parent.invalidate();
        }
    }

    /// Invalidates the component and has its window laid out again on the
    /// next turn of the event dispatch thread.
    public void revalidate() {
        invalidate();
        PeerSupport.scheduleLayout(this);
    }

    boolean cn1Valid() {
        return valid;
    }

    void cn1SetValid(boolean v) {
        valid = v;
    }

    /// Called when the component joins a tree that is shown.
    public void addNotify() {
        displayable = true;
        if (getCursor().getType() != Cursor.DEFAULT_CURSOR) {
            cn1ApplyCursor();
        }
    }

    public void removeNotify() {
        displayable = false;
        EventBridge.forget(this);
    }

    // ------------------------------------------------------------ paint

    /// Always `null`: a component is painted only from `paint`, with the
    /// graphics it is handed. Codename One paints a screen in one pass
    /// from the event dispatch thread and has no surface to draw on
    /// between passes, so code that draws from `getGraphics()` must move
    /// that drawing into `paint` or `paintComponent` and call `repaint()`.
    public Graphics getGraphics() {
        return null;
    }

    public void paint(Graphics g) {
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paintAll(Graphics g) {
        paint(g);
    }

    public void print(Graphics g) {
        paint(g);
    }

    public void printAll(Graphics g) {
        paintAll(g);
    }

    public void repaint() {
        repaint(0, 0, 0, width, height);
    }

    public void repaint(long tm) {
        repaint(tm, 0, 0, width, height);
    }

    public void repaint(int x, int y, int width, int height) {
        repaint(0, x, y, width, height);
    }

    /// Asks for the rectangle to be painted on the next paint cycle. The
    /// delay is ignored.
    public void repaint(long tm, int x, int y, int width, int height) {
        if (peer != null && !ignoreRepaint && width > 0 && height > 0) {
            PeerSupport.repaint(this, x, y, width, height);
        }
    }

    public void setIgnoreRepaint(boolean ignoreRepaint) {
        this.ignoreRepaint = ignoreRepaint;
    }

    public boolean getIgnoreRepaint() {
        return ignoreRepaint;
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        return false;
    }

    /// An off screen image of the given size, in image pixels.
    public Image createImage(int width, int height) {
        return new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
    }

    // ------------------------------------------------------------ focus

    public boolean isFocusable() {
        return focusable;
    }

    public void setFocusable(boolean focusable) {
        boolean old = this.focusable;
        this.focusable = focusable;
        firePropertyChange("focusable", old, focusable);
    }

    /// Makes this component the focus owner: the component key events go
    /// to. Focus is granted at once.
    public void requestFocus() {
        requestFocusInWindow();
    }

    public boolean requestFocusInWindow() {
        if (!focusable || !visible || !enabled) {
            return false;
        }
        EventBridge.setFocusOwner(this, true);
        return true;
    }

    public boolean isFocusOwner() {
        return EventBridge.focusOwner() == this;
    }

    /// Moves the focus to the next component in the traversal order of
    /// this component's window.
    public void transferFocus() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(this);
    }

    public void transferFocusBackward() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(this);
    }

    /// Whether the tab key moves the focus away from this component
    /// instead of only reaching its key listeners.
    public boolean getFocusTraversalKeysEnabled() {
        return focusTraversalKeys;
    }

    public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
        boolean old = focusTraversalKeys;
        focusTraversalKeys = focusTraversalKeysEnabled;
        firePropertyChange("focusTraversalKeysEnabled", old, focusTraversalKeysEnabled);
    }

    public boolean hasFocus() {
        return isFocusOwner();
    }

    // ------------------------------------------------------------ listeners

    /// Hears when this component or one of its ancestors is added or
    /// removed, becomes displayable or not, or starts or stops showing.
    public void addHierarchyListener(HierarchyListener l) {
        if (l != null) {
            if (hierarchyListeners == null) {
                hierarchyListeners = new ArrayList<HierarchyListener>();
            }
            hierarchyListeners.add(l);
            cn1CountHierarchyListeners(1);
        }
    }

    public void removeHierarchyListener(HierarchyListener l) {
        if (hierarchyListeners != null && hierarchyListeners.remove(l)) {
            cn1CountHierarchyListeners(-1);
        }
    }

    public HierarchyListener[] getHierarchyListeners() {
        return hierarchyListeners == null ? new HierarchyListener[0]
                : hierarchyListeners.toArray(new HierarchyListener[hierarchyListeners.size()]);
    }

    private static void cn1CountHierarchyListeners(int by) {
        hierarchyListenerCount += by;
    }

    /// Whether any component has a hierarchy listener.
    static boolean cn1HierarchyHeard() {
        return hierarchyListenerCount > 0;
    }

    /// Tells the hierarchy listeners of this component that `changed`
    /// was added to or removed from `changedParent`, or started or
    /// stopped showing there. A container passes it on to its children.
    void cn1HierarchyChanged(Component changed, Container changedParent, long flags) {
        if (hierarchyListeners == null || hierarchyListeners.isEmpty()) {
            return;
        }
        HierarchyListener[] ls = getHierarchyListeners();
        HierarchyEvent e = new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED, changed, changedParent, flags);
        for (int i = 0; i < ls.length; i++) {
            ls[i].hierarchyChanged(e);
        }
    }

    public void addComponentListener(ComponentListener l) {
        if (l != null) {
            if (componentListeners == null) {
                componentListeners = new ArrayList<ComponentListener>();
            }
            componentListeners.add(l);
        }
    }

    public void removeComponentListener(ComponentListener l) {
        if (componentListeners != null) {
            componentListeners.remove(l);
        }
    }

    public ComponentListener[] getComponentListeners() {
        return componentListeners == null ? new ComponentListener[0]
                : componentListeners.toArray(new ComponentListener[componentListeners.size()]);
    }

    public void addFocusListener(FocusListener l) {
        if (l != null) {
            if (focusListeners == null) {
                focusListeners = new ArrayList<FocusListener>();
            }
            focusListeners.add(l);
        }
    }

    public void removeFocusListener(FocusListener l) {
        if (focusListeners != null) {
            focusListeners.remove(l);
        }
    }

    public FocusListener[] getFocusListeners() {
        return focusListeners == null ? new FocusListener[0]
                : focusListeners.toArray(new FocusListener[focusListeners.size()]);
    }

    public void addKeyListener(KeyListener l) {
        if (l != null) {
            if (keyListeners == null) {
                keyListeners = new ArrayList<KeyListener>();
            }
            keyListeners.add(l);
        }
    }

    public void removeKeyListener(KeyListener l) {
        if (keyListeners != null) {
            keyListeners.remove(l);
        }
    }

    public KeyListener[] getKeyListeners() {
        return keyListeners == null ? new KeyListener[0] : keyListeners.toArray(new KeyListener[keyListeners.size()]);
    }

    public void addMouseListener(MouseListener l) {
        if (l != null) {
            if (mouseListeners == null) {
                mouseListeners = new ArrayList<MouseListener>();
            }
            mouseListeners.add(l);
        }
    }

    public void removeMouseListener(MouseListener l) {
        if (mouseListeners != null) {
            mouseListeners.remove(l);
        }
    }

    public MouseListener[] getMouseListeners() {
        return mouseListeners == null ? new MouseListener[0]
                : mouseListeners.toArray(new MouseListener[mouseListeners.size()]);
    }

    public void addMouseMotionListener(MouseMotionListener l) {
        if (l != null) {
            if (mouseMotionListeners == null) {
                mouseMotionListeners = new ArrayList<MouseMotionListener>();
            }
            mouseMotionListeners.add(l);
        }
    }

    public void removeMouseMotionListener(MouseMotionListener l) {
        if (mouseMotionListeners != null) {
            mouseMotionListeners.remove(l);
        }
    }

    public MouseMotionListener[] getMouseMotionListeners() {
        return mouseMotionListeners == null ? new MouseMotionListener[0]
                : mouseMotionListeners.toArray(new MouseMotionListener[mouseMotionListeners.size()]);
    }

    public void addMouseWheelListener(MouseWheelListener l) {
        if (l != null) {
            if (mouseWheelListeners == null) {
                mouseWheelListeners = new ArrayList<MouseWheelListener>();
            }
            mouseWheelListeners.add(l);
        }
    }

    public void removeMouseWheelListener(MouseWheelListener l) {
        if (mouseWheelListeners != null) {
            mouseWheelListeners.remove(l);
        }
    }

    public MouseWheelListener[] getMouseWheelListeners() {
        return mouseWheelListeners == null ? new MouseWheelListener[0]
                : mouseWheelListeners.toArray(new MouseWheelListener[mouseWheelListeners.size()]);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return changeSupport == null ? new PropertyChangeListener[0] : changeSupport.getPropertyChangeListeners();
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener != null) {
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return changeSupport == null ? new PropertyChangeListener[0]
                : changeSupport.getPropertyChangeListeners(propertyName);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null && (oldValue == null || newValue == null || !oldValue.equals(newValue))) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (changeSupport != null && oldValue != newValue) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (changeSupport != null && oldValue != newValue) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    // ------------------------------------------------------------ events

    protected final void enableEvents(long eventsToEnable) {
        eventMask |= eventsToEnable;
    }

    protected final void disableEvents(long eventsToDisable) {
        eventMask &= ~eventsToDisable;
    }

    private void fireComponentEvent(int id) {
        if (notEmpty(componentListeners) || (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0) {
            dispatchEvent(new ComponentEvent(this, id));
        }
    }

    /// Delivers the event to this component, at once.
    public final void dispatchEvent(AWTEvent e) {
        AwtListeners.dispatching(e);
        processEvent(e);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof FocusEvent) {
            processFocusEvent((FocusEvent) e);
        } else if (e instanceof MouseWheelEvent) {
            processMouseWheelEvent((MouseWheelEvent) e);
        } else if (e instanceof MouseEvent) {
            int id = e.getID();
            if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
                processMouseMotionEvent((MouseEvent) e);
            } else {
                processMouseEvent((MouseEvent) e);
            }
        } else if (e instanceof KeyEvent) {
            processKeyEvent((KeyEvent) e);
        } else if (e instanceof ComponentEvent) {
            processComponentEvent((ComponentEvent) e);
        }
    }

    protected void processComponentEvent(ComponentEvent e) {
        ComponentListener[] ls = getComponentListeners();
        for (int i = 0; i < ls.length; i++) {
            switch (e.getID()) {
                case ComponentEvent.COMPONENT_RESIZED:
                    ls[i].componentResized(e);
                    break;
                case ComponentEvent.COMPONENT_MOVED:
                    ls[i].componentMoved(e);
                    break;
                case ComponentEvent.COMPONENT_SHOWN:
                    ls[i].componentShown(e);
                    break;
                case ComponentEvent.COMPONENT_HIDDEN:
                    ls[i].componentHidden(e);
                    break;
                default:
                    break;
            }
        }
    }

    protected void processFocusEvent(FocusEvent e) {
        FocusListener[] ls = getFocusListeners();
        for (int i = 0; i < ls.length; i++) {
            if (e.getID() == FocusEvent.FOCUS_GAINED) {
                ls[i].focusGained(e);
            } else {
                ls[i].focusLost(e);
            }
        }
    }

    protected void processKeyEvent(KeyEvent e) {
        KeyListener[] ls = getKeyListeners();
        for (int i = 0; i < ls.length; i++) {
            switch (e.getID()) {
                case KeyEvent.KEY_TYPED:
                    ls[i].keyTyped(e);
                    break;
                case KeyEvent.KEY_PRESSED:
                    ls[i].keyPressed(e);
                    break;
                case KeyEvent.KEY_RELEASED:
                    ls[i].keyReleased(e);
                    break;
                default:
                    break;
            }
        }
    }

    protected void processMouseEvent(MouseEvent e) {
        MouseListener[] ls = getMouseListeners();
        for (int i = 0; i < ls.length; i++) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                    ls[i].mousePressed(e);
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    ls[i].mouseReleased(e);
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    ls[i].mouseClicked(e);
                    break;
                case MouseEvent.MOUSE_ENTERED:
                    ls[i].mouseEntered(e);
                    break;
                case MouseEvent.MOUSE_EXITED:
                    ls[i].mouseExited(e);
                    break;
                default:
                    break;
            }
        }
    }

    protected void processMouseMotionEvent(MouseEvent e) {
        MouseMotionListener[] ls = getMouseMotionListeners();
        for (int i = 0; i < ls.length; i++) {
            if (e.getID() == MouseEvent.MOUSE_MOVED) {
                ls[i].mouseMoved(e);
            } else {
                ls[i].mouseDragged(e);
            }
        }
    }

    protected void processMouseWheelEvent(MouseWheelEvent e) {
        MouseWheelListener[] ls = getMouseWheelListeners();
        for (int i = 0; i < ls.length; i++) {
            ls[i].mouseWheelMoved(e);
        }
    }

    protected String paramString() {
        return (name == null ? "" : name) + "," + x + "," + y + "," + width + "x" + height
                + (valid ? "" : ",invalid") + (visible ? "" : ",hidden") + (enabled ? "" : ",disabled");
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + paramString() + "]";
    }

    /// Makes this component a place to drop a drag, or with `null` stops
    /// it being one. A target that belonged to another component moves
    /// here.
    public void setDropTarget(DropTarget dt) {
        if (dt == dropTarget) {
            return;
        }
        DropTarget old = dropTarget;
        dropTarget = dt;
        if (old != null && old.getComponent() == this) {
            old.setComponent(null);
        }
        if (dt != null && dt.getComponent() != this) {
            dt.setComponent(this);
        }
    }

    public DropTarget getDropTarget() {
        return dropTarget;
    }
}
