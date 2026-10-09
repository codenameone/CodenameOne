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
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.beans.PropertyVetoException;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.swing.event.InternalFrameEvent;
import com.codename1.desktopcompat.javax.swing.event.InternalFrameListener;

/// A window inside a window: a frame with a title bar that lives on a
/// [JDesktopPane], or in any container, and is drawn by this layer.
///
/// It has a root pane like a frame, with a content pane, a menu bar place
/// and a glass pane. The title bar shows the frame icon, the title and a
/// button each for closing, maximizing and iconifying, for those of the
/// three that are allowed. Dragging the title bar moves the frame,
/// dragging its edge resizes it when it is resizable, and a press anywhere
/// in it selects it and brings it to the front of its layer. A double
/// click on the title bar maximizes or restores a maximizable frame.
///
/// ## What differs from the desktop
///
///  - An iconified frame is not replaced by a desktop icon: the frame
///    itself shrinks to its title bar and moves to the bottom of its
///    parent, and its iconify button, or a double click, restores it.
///  - The frame is drawn with the colors of the theme. There is no UI
///    delegate, no desktop manager and no system menu; a palette
///    (`JInternalFrame.isPalette`) is drawn like any other frame.
///  - The setters that can be vetoed on the desktop are not vetoed by
///    anything here: there are no vetoable change listeners.
///  - A frame is not a focus cycle root and keeps no "most recent focus
///    owner".
public class JInternalFrame extends JComponent implements Accessible, WindowConstants, RootPaneContainer {

    public static final String CONTENT_PANE_PROPERTY = "contentPane";
    public static final String MENU_BAR_PROPERTY = "JMenuBar";
    public static final String TITLE_PROPERTY = "title";
    public static final String LAYERED_PANE_PROPERTY = "layeredPane";
    public static final String ROOT_PANE_PROPERTY = "rootPane";
    public static final String GLASS_PANE_PROPERTY = "glassPane";
    public static final String FRAME_ICON_PROPERTY = "frameIcon";
    public static final String IS_SELECTED_PROPERTY = "selected";
    public static final String IS_CLOSED_PROPERTY = "closed";
    public static final String IS_MAXIMUM_PROPERTY = "maximum";
    public static final String IS_ICON_PROPERTY = "icon";

    private static final int EDGE = 4;
    private static final int BAR = 24;
    private static final int ICON_WIDTH = 160;
    private static final int MIN_WIDTH = 100;

    private static final int NORTH = 1;
    private static final int SOUTH = 2;
    private static final int WEST = 4;
    private static final int EAST = 8;

    private static final int CLOSE = 0;
    private static final int MAXIMIZE = 1;
    private static final int ICONIFY = 2;

    protected JRootPane rootPane;
    protected boolean rootPaneCheckingEnabled;
    protected boolean closable;
    protected boolean isClosed;
    protected boolean maximizable;
    protected boolean isMaximum;
    protected boolean iconable;
    protected boolean isIcon;
    protected boolean resizable;
    protected boolean isSelected;
    protected Icon frameIcon;
    protected String title;

    private int defaultCloseOperation = DISPOSE_ON_CLOSE;
    private Rectangle normalBounds;
    private boolean opened;
    /// What a drag that began on the chrome does: nothing, move, or
    /// resize by the edges in `dragEdges`.
    private int dragKind;
    private int dragEdges;
    private int pressX;
    private int pressY;
    private int pressedButton = -1;

    public JInternalFrame() {
        this("", false, false, false, false);
    }

    public JInternalFrame(String title) {
        this(title, false, false, false, false);
    }

    public JInternalFrame(String title, boolean resizable) {
        this(title, resizable, false, false, false);
    }

    public JInternalFrame(String title, boolean resizable, boolean closable) {
        this(title, resizable, closable, false, false);
    }

    public JInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable) {
        this(title, resizable, closable, maximizable, false);
    }

    public JInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable,
            boolean iconifiable) {
        this.title = title;
        this.resizable = resizable;
        this.closable = closable;
        this.maximizable = maximizable;
        this.iconable = iconifiable;
        setRootPane(createRootPane());
        setLayout(null);
        setRootPaneCheckingEnabled(true);
        setVisible(false);
        setOpaque(true);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    // ------------------------------------------------------------ root pane

    protected JRootPane createRootPane() {
        return new JRootPane();
    }

    @Override
    public JRootPane getRootPane() {
        return rootPane;
    }

    protected void setRootPane(JRootPane root) {
        boolean checking = rootPaneCheckingEnabled;
        rootPaneCheckingEnabled = false;
        JRootPane old = rootPane;
        try {
            if (old != null) {
                remove(old);
            }
            rootPane = root;
            if (root != null) {
                add(root);
            }
        } finally {
            rootPaneCheckingEnabled = checking;
        }
        firePropertyChange(ROOT_PANE_PROPERTY, old, root);
    }

    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (rootPaneCheckingEnabled) {
            getContentPane().add(comp, constraints, index);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    @Override
    public void remove(Component comp) {
        if (comp == rootPane || !rootPaneCheckingEnabled) {
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }

    @Override
    public void setLayout(LayoutManager manager) {
        if (rootPaneCheckingEnabled) {
            getContentPane().setLayout(manager);
        } else {
            super.setLayout(manager);
        }
    }

    @Override
    public Container getContentPane() {
        return rootPane.getContentPane();
    }

    @Override
    public void setContentPane(Container c) {
        Container old = getContentPane();
        rootPane.setContentPane(c);
        firePropertyChange(CONTENT_PANE_PROPERTY, old, c);
        revalidate();
        repaint();
    }

    @Override
    public JLayeredPane getLayeredPane() {
        return rootPane.getLayeredPane();
    }

    @Override
    public void setLayeredPane(JLayeredPane layered) {
        JLayeredPane old = getLayeredPane();
        rootPane.setLayeredPane(layered);
        firePropertyChange(LAYERED_PANE_PROPERTY, old, layered);
    }

    @Override
    public Component getGlassPane() {
        return rootPane.getGlassPane();
    }

    @Override
    public void setGlassPane(Component glass) {
        Component old = getGlassPane();
        rootPane.setGlassPane(glass);
        firePropertyChange(GLASS_PANE_PROPERTY, old, glass);
    }

    public JMenuBar getJMenuBar() {
        return rootPane.getJMenuBar();
    }

    public void setJMenuBar(JMenuBar m) {
        JMenuBar old = getJMenuBar();
        rootPane.setJMenuBar(m);
        firePropertyChange(MENU_BAR_PROPERTY, old, m);
    }

    // ------------------------------------------------------------ properties

    public boolean isClosable() {
        return closable;
    }

    public void setClosable(boolean b) {
        boolean old = closable;
        closable = b;
        firePropertyChange("closable", old, b);
        repaint();
    }

    public boolean isClosed() {
        return isClosed;
    }

    /// Closes the frame: it is told it is closing, taken off its parent
    /// and told it is closed. `false` does nothing to a closed frame.
    ///
    /// #### Throws
    ///
    /// - `PropertyVetoException`: never; see the class description
    public void setClosed(boolean b) throws PropertyVetoException {
        if (isClosed == b) {
            return;
        }
        if (b) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        }
        isClosed = b;
        firePropertyChange(IS_CLOSED_PROPERTY, !b, b);
        if (b) {
            dispose();
        }
    }

    public boolean isResizable() {
        return !isMaximum && resizable;
    }

    public void setResizable(boolean b) {
        boolean old = resizable;
        resizable = b;
        firePropertyChange("resizable", old, b);
    }

    public boolean isIconifiable() {
        return iconable;
    }

    public void setIconifiable(boolean b) {
        boolean old = iconable;
        iconable = b;
        firePropertyChange("iconable", old, b);
        repaint();
    }

    public boolean isIcon() {
        return isIcon;
    }

    /// Iconifies the frame or restores it; see the class description for
    /// what an iconified frame looks like.
    ///
    /// #### Throws
    ///
    /// - `PropertyVetoException`: never; see the class description
    public void setIcon(boolean b) throws PropertyVetoException {
        if (isIcon == b) {
            return;
        }
        if (b) {
            if (!isMaximum) {
                normalBounds = getBounds();
            }
            isIcon = true;
            Container p = getParent();
            int at = 0;
            int bottom = 0;
            if (p != null) {
                bottom = Math.max(0, p.getHeight() - (BAR + 2 * EDGE));
                for (int i = 0; i < p.getComponentCount(); i++) {
                    Component c = p.getComponent(i);
                    if (c != this && c instanceof JInternalFrame && ((JInternalFrame) c).isIcon()) {
                        at++;
                    }
                }
            }
            setBounds(at * ICON_WIDTH, bottom, ICON_WIDTH, BAR + 2 * EDGE);
        } else {
            isIcon = false;
            if (isMaximum) {
                cn1FillParent();
            } else if (normalBounds != null) {
                setBounds(normalBounds);
            }
        }
        firePropertyChange(IS_ICON_PROPERTY, !b, b);
        fireInternalFrameEvent(b ? InternalFrameEvent.INTERNAL_FRAME_ICONIFIED
                : InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED);
        revalidate();
        repaint();
    }

    public boolean isMaximizable() {
        return maximizable;
    }

    public void setMaximizable(boolean b) {
        boolean old = maximizable;
        maximizable = b;
        firePropertyChange("maximizable", old, b);
        repaint();
    }

    public boolean isMaximum() {
        return isMaximum;
    }

    /// Makes the frame as large as its parent, or gives it back the
    /// bounds it had before.
    ///
    /// #### Throws
    ///
    /// - `PropertyVetoException`: never; see the class description
    public void setMaximum(boolean b) throws PropertyVetoException {
        if (isMaximum == b) {
            return;
        }
        if (b) {
            if (!isIcon) {
                normalBounds = getBounds();
            }
            isMaximum = true;
            if (!isIcon) {
                cn1FillParent();
            }
        } else {
            isMaximum = false;
            if (!isIcon && normalBounds != null) {
                setBounds(normalBounds);
            }
        }
        firePropertyChange(IS_MAXIMUM_PROPERTY, !b, b);
        revalidate();
        repaint();
    }

    private void cn1FillParent() {
        Container p = getParent();
        if (p != null) {
            setBounds(0, 0, p.getWidth(), p.getHeight());
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String old = this.title;
        this.title = title;
        firePropertyChange(TITLE_PROPERTY, old, title);
        repaint();
    }

    public boolean isSelected() {
        return isSelected;
    }

    /// Selects the frame, which takes the selection from the frame of
    /// its desk that had it and brings this one to the front, or takes
    /// the selection from it. A frame that is not showing, or is closed
    /// or iconified, is not selected.
    ///
    /// #### Throws
    ///
    /// - `PropertyVetoException`: never; see the class description
    public void setSelected(boolean selected) throws PropertyVetoException {
        if (selected && (isIcon || isClosed || !isShowing())) {
            return;
        }
        if (isSelected == selected) {
            return;
        }
        JDesktopPane desk = getDesktopPane();
        if (selected) {
            Container p = getParent();
            if (p != null) {
                for (int i = 0; i < p.getComponentCount(); i++) {
                    Component c = p.getComponent(i);
                    if (c != this && c instanceof JInternalFrame && ((JInternalFrame) c).isSelected()) {
                        ((JInternalFrame) c).setSelected(false);
                    }
                }
            }
        }
        isSelected = selected;
        firePropertyChange(IS_SELECTED_PROPERTY, !selected, selected);
        if (selected) {
            if (desk != null) {
                desk.setSelectedFrame(this);
            }
            moveToFront();
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ACTIVATED);
        } else {
            if (desk != null && desk.getSelectedFrame() == this) {
                desk.setSelectedFrame(null);
            }
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED);
        }
        repaint();
    }

    public void setFrameIcon(Icon icon) {
        Icon old = frameIcon;
        frameIcon = icon;
        firePropertyChange(FRAME_ICON_PROPERTY, old, icon);
        repaint();
    }

    public Icon getFrameIcon() {
        return frameIcon;
    }

    /// The bounds the frame has when it is neither maximized nor
    /// iconified.
    public Rectangle getNormalBounds() {
        if (normalBounds != null && (isMaximum || isIcon)) {
            return new Rectangle(normalBounds.x, normalBounds.y, normalBounds.width, normalBounds.height);
        }
        return getBounds();
    }

    public void setNormalBounds(Rectangle r) {
        normalBounds = r == null ? null : new Rectangle(r.x, r.y, r.width, r.height);
    }

    public void setDefaultCloseOperation(int operation) {
        defaultCloseOperation = operation;
    }

    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    // ------------------------------------------------------------ layers

    public void moveToFront() {
        Container p = getParent();
        if (p instanceof JLayeredPane) {
            ((JLayeredPane) p).moveToFront(this);
        }
    }

    public void moveToBack() {
        Container p = getParent();
        if (p instanceof JLayeredPane) {
            ((JLayeredPane) p).moveToBack(this);
        }
    }

    public void toFront() {
        moveToFront();
    }

    public void toBack() {
        moveToBack();
    }

    public void setLayer(Integer layer) {
        setLayer(layer.intValue());
    }

    public void setLayer(int layer) {
        Container p = getParent();
        if (p instanceof JLayeredPane) {
            ((JLayeredPane) p).setLayer(this, layer, ((JLayeredPane) p).getPosition(this));
        } else {
            JLayeredPane.putLayer(this, layer);
        }
    }

    public int getLayer() {
        return JLayeredPane.getLayer(this);
    }

    /// The desk the frame lies on, or `null` when its parent is not one.
    public JDesktopPane getDesktopPane() {
        for (Container p = getParent(); p != null; p = p.getParent()) {
            if (p instanceof JDesktopPane) {
                return (JDesktopPane) p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ showing

    /// Shows the frame: the first time it is told it was opened, and it
    /// comes to the front and is selected unless another frame is.
    public void show() {
        if (isVisible()) {
            return;
        }
        if (!opened) {
            opened = true;
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
        }
        setVisible(true);
        toFront();
        JDesktopPane desk = getDesktopPane();
        if (!isIcon && !isClosed && (desk == null || desk.getSelectedFrame() == null)) {
            try {
                setSelected(true);
            } catch (PropertyVetoException notVetoedHere) {
                repaint();
            }
        }
    }

    public void hide() {
        if (isIcon && isVisible()) {
            repaint();
        }
        setVisible(false);
    }

    /// Takes the frame off its parent for good and tells it it is
    /// closed.
    public void dispose() {
        if (isVisible()) {
            setVisible(false);
        }
        if (isSelected) {
            try {
                setSelected(false);
            } catch (PropertyVetoException notVetoedHere) {
                isSelected = false;
            }
        }
        if (!isClosed) {
            isClosed = true;
            firePropertyChange(IS_CLOSED_PROPERTY, false, true);
        }
        Container p = getParent();
        if (p != null) {
            p.remove(this);
            p.repaint();
        }
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
    }

    /// What the close button does: tells the frame it is closing, then
    /// hides it, disposes of it or leaves it, as the default close
    /// operation says.
    public void doDefaultCloseAction() {
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        switch (defaultCloseOperation) {
            case HIDE_ON_CLOSE:
                setVisible(false);
                if (isSelected) {
                    try {
                        setSelected(false);
                    } catch (PropertyVetoException notVetoedHere) {
                        isSelected = false;
                    }
                }
                break;
            case DISPOSE_ON_CLOSE:
                dispose();
                break;
            default:
                break;
        }
    }

    /// Sizes the frame to the preferred size of what it holds.
    public void pack() {
        Dimension d = getPreferredSize();
        setSize(d.width, d.height);
        validate();
    }

    // ------------------------------------------------------------ events

    public void addInternalFrameListener(InternalFrameListener l) {
        listenerList.add(InternalFrameListener.class, l);
    }

    public void removeInternalFrameListener(InternalFrameListener l) {
        listenerList.remove(InternalFrameListener.class, l);
    }

    public InternalFrameListener[] getInternalFrameListeners() {
        return listenerList.getListeners(InternalFrameListener.class);
    }

    protected void fireInternalFrameEvent(int id) {
        InternalFrameListener[] ls = getInternalFrameListeners();
        if (ls.length == 0) {
            return;
        }
        InternalFrameEvent e = new InternalFrameEvent(this, id);
        for (int i = ls.length - 1; i >= 0; i--) {
            switch (id) {
                case InternalFrameEvent.INTERNAL_FRAME_OPENED:
                    ls[i].internalFrameOpened(e);
                    break;
                case InternalFrameEvent.INTERNAL_FRAME_CLOSING:
                    ls[i].internalFrameClosing(e);
                    break;
                case InternalFrameEvent.INTERNAL_FRAME_CLOSED:
                    ls[i].internalFrameClosed(e);
                    break;
                case InternalFrameEvent.INTERNAL_FRAME_ICONIFIED:
                    ls[i].internalFrameIconified(e);
                    break;
                case InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED:
                    ls[i].internalFrameDeiconified(e);
                    break;
                case InternalFrameEvent.INTERNAL_FRAME_ACTIVATED:
                    ls[i].internalFrameActivated(e);
                    break;
                case InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED:
                    ls[i].internalFrameDeactivated(e);
                    break;
                default:
                    break;
            }
        }
    }

    // ------------------------------------------------------------ layout

    @Override
    public void doLayout() {
        if (rootPane != null) {
            rootPane.setBounds(EDGE, EDGE + BAR, Math.max(0, getWidth() - 2 * EDGE),
                    Math.max(0, getHeight() - 2 * EDGE - BAR));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet() || rootPane == null) {
            return super.getPreferredSize();
        }
        Dimension d = rootPane.getPreferredSize();
        return new Dimension(Math.max(MIN_WIDTH, d.width + 2 * EDGE), d.height + 2 * EDGE + BAR);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return new Dimension(MIN_WIDTH, BAR + 2 * EDGE);
    }

    // ------------------------------------------------------------ paint

    /// `c` moved toward black by `by` in each channel; a middle gray for
    /// no color.
    static Color cn1Shade(Color c, int by) {
        if (c == null) {
            return new Color(160, 160, 160);
        }
        return new Color(Math.max(0, c.getRed() - by), Math.max(0, c.getGreen() - by),
                Math.max(0, c.getBlue() - by));
    }

    /// Where the title bar button `which` is, or `null` when the frame
    /// has none: the buttons run from the right, close first.
    private Rectangle cn1Button(int which) {
        int size = BAR - 6;
        int x = getWidth() - EDGE - 3 - size;
        int y = EDGE + 3;
        if (closable) {
            if (which == CLOSE) {
                return new Rectangle(x, y, size, size);
            }
            x -= size + 3;
        }
        if (maximizable && !isIcon) {
            if (which == MAXIMIZE) {
                return new Rectangle(x, y, size, size);
            }
            x -= size + 3;
        }
        if (iconable && which == ICONIFY) {
            return new Rectangle(x, y, size, size);
        }
        return null;
    }

    private int cn1ButtonAt(int x, int y) {
        for (int i = CLOSE; i <= ICONIFY; i++) {
            Rectangle r = cn1Button(i);
            if (r != null && r.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Color face = isBackgroundSet() ? getBackground() : UIManager.getColor("control");
        if (face == null) {
            face = new Color(238, 238, 238);
        }
        Color line = cn1Shade(face, 96);
        g.setColor(face);
        g.fillRect(0, 0, w, h);
        g.setColor(line);
        g.drawRect(0, 0, w - 1, h - 1);

        Color bar = isSelected ? UIManager.getColor("textHighlight") : null;
        Color ink = isSelected ? UIManager.getColor("textHighlightText") : UIManager.getColor("controlText");
        if (bar == null) {
            bar = cn1Shade(face, 40);
        }
        if (ink == null) {
            ink = Color.BLACK;
        }
        g.setColor(bar);
        g.fillRect(EDGE, EDGE, Math.max(0, w - 2 * EDGE), BAR);

        int right = w - EDGE - 3;
        for (int i = CLOSE; i <= ICONIFY; i++) {
            Rectangle r = cn1Button(i);
            if (r == null) {
                continue;
            }
            right = Math.min(right, r.x - 3);
            g.setColor(face);
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(line);
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            int a = 4;
            int x0 = r.x + a;
            int y0 = r.y + a;
            int x1 = r.x + r.width - 1 - a;
            int y1 = r.y + r.height - 1 - a;
            if (i == CLOSE) {
                g.drawLine(x0, y0, x1, y1);
                g.drawLine(x0, y1, x1, y0);
            } else if (i == MAXIMIZE) {
                g.drawRect(x0, y0, x1 - x0, y1 - y0);
                g.drawLine(x0, y0 + 1, x1, y0 + 1);
                if (isMaximum) {
                    g.drawLine(x0 + 3, y1 - 3, x1 - 3, y1 - 3);
                }
            } else {
                g.drawLine(x0, y1, x1, y1);
                g.drawLine(x0, y1 - 1, x1, y1 - 1);
            }
        }

        int x = EDGE + 4;
        if (frameIcon != null) {
            int ih = frameIcon.getIconHeight();
            if (ih > 0 && ih <= BAR) {
                frameIcon.paintIcon(this, g, x, EDGE + (BAR - ih) / 2);
                x += frameIcon.getIconWidth() + 4;
            }
        }
        if (title != null && title.length() > 0 && right > x) {
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            String shown = title;
            while (shown.length() > 1 && fm.stringWidth(shown) > right - x) {
                shown = shown.substring(0, shown.length() - 1);
            }
            if (fm.stringWidth(shown) <= right - x) {
                g.setColor(ink);
                g.drawString(shown, x, EDGE + (BAR - fm.getHeight()) / 2 + fm.getAscent());
            }
        }
    }

    // ------------------------------------------------------------ pointer

    /// A press landed on `hit`: the internal frame it is in, if any and
    /// not selected yet, becomes the selected one. Called for every
    /// press, before the press is delivered.
    public static void cn1PressedIn(Component hit) {
        for (Component c = hit; c != null; c = c.getParent()) {
            if (c instanceof JInternalFrame) {
                JInternalFrame f = (JInternalFrame) c;
                if (!f.isSelected() && !f.isIcon()) {
                    try {
                        f.setSelected(true);
                    } catch (PropertyVetoException notVetoedHere) {
                        f.repaint();
                    }
                }
                return;
            }
        }
    }

    private int cn1EdgesAt(int x, int y) {
        if (!isResizable() || isIcon) {
            return 0;
        }
        int grip = EDGE + 4;
        int edges = 0;
        if (y < EDGE) {
            edges |= NORTH;
        } else if (y >= getHeight() - EDGE) {
            edges |= SOUTH;
        }
        if (x < EDGE) {
            edges |= WEST;
        } else if (x >= getWidth() - EDGE) {
            edges |= EAST;
        }
        if (edges != 0) {
            // Near a corner an edge is the corner.
            if (y < grip) {
                edges |= NORTH;
            } else if (y >= getHeight() - grip) {
                edges |= SOUTH;
            }
            if (x < grip) {
                edges |= WEST;
            } else if (x >= getWidth() - grip) {
                edges |= EAST;
            }
        }
        return edges;
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.isConsumed() || e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        int x = e.getX();
        int y = e.getY();
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            dragKind = 0;
            pressedButton = cn1ButtonAt(x, y);
            if (pressedButton >= 0) {
                return;
            }
            pressX = x;
            pressY = y;
            dragEdges = cn1EdgesAt(x, y);
            if (dragEdges != 0) {
                dragKind = 2;
            } else if (y < EDGE + BAR && !isMaximum) {
                dragKind = 1;
            }
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            int was = pressedButton;
            pressedButton = -1;
            dragKind = 0;
            if (was >= 0 && was == cn1ButtonAt(x, y)) {
                cn1Press(was);
            }
        } else if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getClickCount() == 2 && y < EDGE + BAR
                && cn1ButtonAt(x, y) < 0) {
            if (isIcon) {
                cn1Press(ICONIFY);
            } else if (maximizable) {
                cn1Press(MAXIMIZE);
            }
        }
    }

    private void cn1Press(int which) {
        try {
            if (which == CLOSE) {
                doDefaultCloseAction();
            } else if (which == MAXIMIZE) {
                setMaximum(!isMaximum);
            } else if (which == ICONIFY) {
                setIcon(!isIcon);
                if (!isIcon) {
                    setSelected(true);
                }
            }
        } catch (PropertyVetoException notVetoedHere) {
            repaint();
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
        if (e.getID() != MouseEvent.MOUSE_DRAGGED || dragKind == 0 || e.isConsumed()) {
            return;
        }
        int dx = e.getX() - pressX;
        int dy = e.getY() - pressY;
        if (dragKind == 1) {
            if (dx != 0 || dy != 0) {
                setLocation(getX() + dx, getY() + dy);
            }
            return;
        }
        int nx = getX();
        int ny = getY();
        int nw = getWidth();
        int nh = getHeight();
        int minH = BAR + 2 * EDGE;
        if ((dragEdges & EAST) != 0) {
            nw = Math.max(MIN_WIDTH, nw + dx);
            pressX += nw - getWidth();
        } else if ((dragEdges & WEST) != 0) {
            int w = Math.max(MIN_WIDTH, nw - dx);
            nx += nw - w;
            nw = w;
        }
        if ((dragEdges & SOUTH) != 0) {
            nh = Math.max(minH, nh + dy);
            pressY += nh - getHeight();
        } else if ((dragEdges & NORTH) != 0) {
            int h = Math.max(minH, nh - dy);
            ny += nh - h;
            nh = h;
        }
        if (nx != getX() || ny != getY() || nw != getWidth() || nh != getHeight()) {
            setBounds(nx, ny, nw, nh);
            revalidate();
            Container p = getParent();
            if (p != null) {
                p.repaint();
            }
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",title=" + title;
    }
}
