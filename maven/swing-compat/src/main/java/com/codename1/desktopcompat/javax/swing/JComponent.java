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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.accessibility.AccessibleContext;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import com.codename1.desktopcompat.rt.EventBridge;
import java.util.ArrayList;
import java.util.HashMap;

/// The base of the Swing components.
///
/// Painting follows Swing: `paint` calls `paintComponent`, `paintBorder`
/// and `paintChildren`. The default `paintComponent` draws the Codename
/// One widget behind a component that has one (a button, a label) and
/// otherwise fills the background of an opaque component; an override that
/// does not call `super.paintComponent` therefore starts from what its
/// parent painted. There are no UI delegates: `updateUI` does nothing.
///
/// Key bindings work as in Swing. A key event that no key listener
/// consumed is looked up in the input map of the focused component
/// (`WHEN_FOCUSED`), then in those of it and its ancestors
/// (`WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`), then in those every visible,
/// enabled component of the window registered for the whole window
/// (`WHEN_IN_FOCUSED_WINDOW`); the first enabled action found runs and
/// the event is consumed. A component with a `WHEN_FOCUSED` binding is a
/// stop of the tab key.
///
/// The tool tip is the tool tip of the Codename One widget behind the
/// component, so a component without one shows none, and it appears
/// only where the port has a pointer that hovers.
///
/// Every component has an accessible context. Its name becomes the
/// accessibility text of the widget behind the component; see
/// `javax.accessibility` for what else is kept.
public abstract class JComponent extends Container {

    public static final int WHEN_FOCUSED = 0;
    public static final int WHEN_ANCESTOR_OF_FOCUSED_COMPONENT = 1;
    public static final int WHEN_IN_FOCUSED_WINDOW = 2;
    public static final int UNDEFINED_CONDITION = -1;
    public static final String TOOL_TIP_TEXT_KEY = "ToolTipText";

    protected EventListenerList listenerList = new EventListenerList();

    private Border border;
    private boolean opaque;
    private float alignmentX = -1;
    private float alignmentY = -1;
    private String toolTipText;
    private AccessibleContext cn1Accessible;
    private HashMap<Object, Object> clientProperties;
    private InputMap focusInputMap;
    private InputMap ancestorInputMap;
    private ComponentInputMap windowInputMap;
    private ActionMap actionMap;
    private JPopupMenu popupMenu;
    private boolean inheritsPopupMenu;
    private InputVerifier inputVerifier;
    private boolean verifyInputWhenFocusTarget = true;
    /// Whether this is a component a look and feel gives colors to: 0 not
    /// asked yet, 1 yes, 2 no.
    private byte lookAndFeelColors;

    public JComponent() {
    }

    /// Does nothing: there are no UI delegates to refresh.
    public void updateUI() {
    }

    // ------------------------------------------------------------ paint

    @Override
    public void paint(Graphics g) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics co = g.create();
        try {
            paintComponent(co);
        } finally {
            co.dispose();
        }
        co = g.create();
        try {
            paintBorder(co);
        } finally {
            co.dispose();
        }
        paintChildren(g);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    /// Draws the Codename One widget behind this component if it has one;
    /// otherwise fills the background when the component is opaque.
    protected void paintComponent(Graphics g) {
        if (cn1PaintNative(g)) {
            return;
        }
        if (isOpaque()) {
            Color bg = getBackground();
            if (bg != null) {
                g.setColor(bg);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    protected void paintBorder(Graphics g) {
        if (border != null) {
            border.paintBorder(this, g, 0, 0, getWidth(), getHeight());
        }
    }

    protected void paintChildren(Graphics g) {
        cn1PaintChildren(g);
    }

    public void repaint(Rectangle r) {
        repaint(0, r.x, r.y, r.width, r.height);
    }

    /// Asks for a repaint; nothing is painted before this returns.
    public void paintImmediately(int x, int y, int w, int h) {
        repaint(0, x, y, w, h);
    }

    public void paintImmediately(Rectangle r) {
        repaint(0, r.x, r.y, r.width, r.height);
    }

    /// The font this component draws with, which is never null. On a desktop
    /// every Swing component has the font its look and feel installed, so
    /// `getFont().deriveFont(Font.BOLD)` is written everywhere, a cell
    /// renderer included -- and a renderer is in no window to inherit a font
    /// from. One that was given none answers the theme's default, which is
    /// what a window answers for everything shown in it.
    @Override
    public com.codename1.desktopcompat.java.awt.Font getFont() {
        com.codename1.desktopcompat.java.awt.Font f = super.getFont();
        return f != null ? f : com.codename1.desktopcompat.rt.Fonts.defaultFont();
    }

    /// Whether this is one of the components whose look and feel installs
    /// a background and a foreground. On a desktop every Swing widget is,
    /// so none of them takes its colors from the container around it; a
    /// class that extends `JComponent` directly, a `Box` or a layered pane
    /// has no look and feel and inherits them, as any AWT component does.
    private boolean cn1LookAndFeelColors() {
        if (lookAndFeelColors == 0) {
            Object o = this;
            boolean has = o instanceof JPanel || o instanceof JLabel || o instanceof AbstractButton
                    || o instanceof JScrollPane || o instanceof JViewport || o instanceof JScrollBar
                    || o instanceof JSplitPane || o instanceof JTabbedPane || o instanceof JToolBar
                    || o instanceof JMenuBar || o instanceof JPopupMenu || o instanceof JSlider
                    || o instanceof JProgressBar || o instanceof JComboBox || o instanceof JInternalFrame
                    || o instanceof JRootPane || o instanceof JOptionPane || o instanceof JSeparator
                    || o instanceof JSpinner || o instanceof JColorChooser || o instanceof JFileChooser
                    || o instanceof JList || o instanceof JTable || o instanceof JTree || o instanceof JDesktopPane
                    || o instanceof com.codename1.desktopcompat.javax.swing.text.JTextComponent
                    || o instanceof com.codename1.desktopcompat.javax.swing.table.JTableHeader;
            lookAndFeelColors = (byte) (has ? 1 : 2);
        }
        return lookAndFeelColors == 1;
    }

    /// The background that was set, and otherwise the one the look and
    /// feel gives a widget -- the `control` color of [UIManager], which is
    /// the theme's window background unless the application put another.
    /// Only a component without a look and feel answers the background of
    /// its parent.
    @Override
    public Color getBackground() {
        if (!isBackgroundSet() && cn1LookAndFeelColors()) {
            Color c = UIManager.getColor("control");
            if (c != null) {
                return c;
            }
        }
        return super.getBackground();
    }

    /// The foreground that was set, and otherwise the `controlText` color
    /// of [UIManager] for a widget; see [#getBackground].
    @Override
    public Color getForeground() {
        if (!isForegroundSet() && cn1LookAndFeelColors()) {
            Color c = UIManager.getColor("controlText");
            if (c != null) {
                return c;
            }
        }
        return super.getForeground();
    }

    @Override
    public boolean isOpaque() {
        return opaque;
    }

    public void setOpaque(boolean isOpaque) {
        boolean old = opaque;
        opaque = isOpaque;
        firePropertyChange("opaque", old, isOpaque);
        repaint();
    }

    @Override
    public boolean isDoubleBuffered() {
        return true;
    }

    public void setDoubleBuffered(boolean aFlag) {
    }

    public Rectangle getVisibleRect() {
        return new Rectangle(0, 0, getWidth(), getHeight());
    }

    // ------------------------------------------------------------ border

    public void setBorder(Border border) {
        Border old = this.border;
        this.border = border;
        firePropertyChange("border", old, border);
        if (border != old) {
            revalidate();
            repaint();
        }
    }

    public Border getBorder() {
        return border;
    }

    @Override
    public Insets getInsets() {
        if (border != null) {
            return border.getBorderInsets(this);
        }
        return super.getInsets();
    }

    public Insets getInsets(Insets insets) {
        Insets i = getInsets();
        if (insets == null) {
            return i;
        }
        insets.top = i.top;
        insets.left = i.left;
        insets.bottom = i.bottom;
        insets.right = i.right;
        return insets;
    }

    // ------------------------------------------------------------ sizes

    @Override
    public Dimension getMaximumSize() {
        if (!isMaximumSizeSet()) {
            Dimension d = cn1NativePreferredSize();
            if (d != null) {
                return d;
            }
        }
        return super.getMaximumSize();
    }

    @Override
    public float getAlignmentX() {
        return alignmentX >= 0 ? alignmentX : super.getAlignmentX();
    }

    public void setAlignmentX(float alignmentX) {
        this.alignmentX = alignmentX > 1.0f ? 1.0f : alignmentX < 0.0f ? 0.0f : alignmentX;
    }

    @Override
    public float getAlignmentY() {
        return alignmentY >= 0 ? alignmentY : super.getAlignmentY();
    }

    public void setAlignmentY(float alignmentY) {
        this.alignmentY = alignmentY > 1.0f ? 1.0f : alignmentY < 0.0f ? 0.0f : alignmentY;
    }

    // ------------------------------------------------------------ misc

    /// Sets the tool tip of the Codename One widget behind this
    /// component.
    public void setToolTipText(String text) {
        String old = toolTipText;
        toolTipText = text;
        cn1ApplyToolTip();
        firePropertyChange(TOOL_TIP_TEXT_KEY, old, text);
    }

    /// Gives the widget the tool tip, or none while the tool tip manager
    /// is disabled.
    void cn1ApplyToolTip() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p != null) {
            p.setTooltip(ToolTipManager.sharedInstance().isEnabled() ? toolTipText : null);
        }
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        if (toolTipText != null) {
            cn1ApplyToolTip();
        }
        cn1ApplyAccessibleName();
    }

    /// What this component tells assistive technology about itself.
    public AccessibleContext getAccessibleContext() {
        if (cn1Accessible == null) {
            cn1Accessible = new Cn1Accessible(this);
        }
        return cn1Accessible;
    }

    /// Gives the widget the accessible name as its accessibility text.
    void cn1ApplyAccessibleName() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        String name = cn1Accessible == null ? null : cn1Accessible.getAccessibleName();
        if (p != null && name != null) {
            p.setAccessibilityText(name);
        }
    }

    /// The context of a component: the name goes on to the widget.
    private static final class Cn1Accessible extends AccessibleContext {

        private final JComponent owner;

        Cn1Accessible(JComponent owner) {
            this.owner = owner;
        }

        @Override
        public void setAccessibleName(String s) {
            super.setAccessibleName(s);
            owner.cn1ApplyAccessibleName();
        }
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public final Object getClientProperty(Object key) {
        return clientProperties == null ? null : clientProperties.get(key);
    }

    public final void putClientProperty(Object key, Object value) {
        if (clientProperties == null) {
            if (value == null) {
                return;
            }
            clientProperties = new HashMap<Object, Object>();
        }
        Object old = clientProperties.get(key);
        if (value == null) {
            clientProperties.remove(key);
        } else {
            clientProperties.put(key, value);
        }
        firePropertyChange(String.valueOf(key), old, value);
    }

    public void grabFocus() {
        requestFocus();
    }

    public void setRequestFocusEnabled(boolean requestFocusEnabled) {
    }

    public JRootPane getRootPane() {
        return SwingUtilities.getRootPane(this);
    }

    public Container getTopLevelAncestor() {
        for (Container p = this; p != null; p = p.getParent()) {
            if (p instanceof com.codename1.desktopcompat.java.awt.Window) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    // ------------------------------------------------------------ scroll

    /// Scrolls the viewports this component is in, innermost first, so
    /// that a rectangle of it is visible. Nothing happens outside a
    /// viewport.
    public void scrollRectToVisible(Rectangle aRect) {
        JViewport.cn1ScrollRectToVisible(this, aRect);
    }

    // ------------------------------------------------------------ verifier

    /// Sets what decides whether the focus may leave this component.
    ///
    /// It is asked when another Swing component requests the focus, and
    /// the request is refused when it does not yield. It cannot hold the
    /// focus against the platform: when the Codename One widget behind
    /// this component loses the focus by itself, it is gone.
    public void setInputVerifier(InputVerifier inputVerifier) {
        InputVerifier old = this.inputVerifier;
        this.inputVerifier = inputVerifier;
        firePropertyChange("inputVerifier", old, inputVerifier);
    }

    public InputVerifier getInputVerifier() {
        return inputVerifier;
    }

    /// Whether the input verifier of the focus owner is asked before
    /// this component takes the focus; true unless set otherwise, and
    /// false is what a cancel button wants.
    public void setVerifyInputWhenFocusTarget(boolean verifyInputWhenFocusTarget) {
        boolean old = this.verifyInputWhenFocusTarget;
        this.verifyInputWhenFocusTarget = verifyInputWhenFocusTarget;
        firePropertyChange("verifyInputWhenFocusTarget", old, verifyInputWhenFocusTarget);
    }

    public boolean getVerifyInputWhenFocusTarget() {
        return verifyInputWhenFocusTarget;
    }

    /// Takes the focus unless the input verifier of the component that
    /// has it objects.
    @Override
    public boolean requestFocusInWindow() {
        Component owner = EventBridge.focusOwner();
        if (verifyInputWhenFocusTarget && owner != this && owner instanceof JComponent) {
            JComponent from = (JComponent) owner;
            InputVerifier v = from.getInputVerifier();
            if (v != null && !v.shouldYieldFocus(from)) {
                return false;
            }
        }
        return super.requestFocusInWindow();
    }

    // ------------------------------------------------------------ popup

    /// Sets the popup menu that opens on the popup trigger over this
    /// component: a press of the secondary mouse button, or a long press
    /// of a finger.
    public void setComponentPopupMenu(JPopupMenu popup) {
        JPopupMenu old = popupMenu;
        popupMenu = popup;
        firePropertyChange("componentPopupMenu", old, popup);
    }

    public JPopupMenu getComponentPopupMenu() {
        if (!inheritsPopupMenu || popupMenu != null) {
            return popupMenu;
        }
        for (Container p = getParent(); p != null; p = p.getParent()) {
            if (p instanceof JComponent) {
                return ((JComponent) p).getComponentPopupMenu();
            }
            if (p instanceof Window) {
                break;
            }
        }
        return null;
    }

    public void setInheritsPopupMenu(boolean value) {
        boolean old = inheritsPopupMenu;
        inheritsPopupMenu = value;
        firePropertyChange("inheritsPopupMenu", old, value);
    }

    public boolean getInheritsPopupMenu() {
        return inheritsPopupMenu;
    }

    // ------------------------------------------------------------ keys

    public final InputMap getInputMap() {
        return getInputMap(WHEN_FOCUSED);
    }

    /// The input map of a condition, created on first use.
    public final InputMap getInputMap(int condition) {
        switch (condition) {
            case WHEN_FOCUSED:
                if (focusInputMap == null) {
                    focusInputMap = new InputMap();
                }
                return focusInputMap;
            case WHEN_ANCESTOR_OF_FOCUSED_COMPONENT:
                if (ancestorInputMap == null) {
                    ancestorInputMap = new InputMap();
                }
                return ancestorInputMap;
            case WHEN_IN_FOCUSED_WINDOW:
                if (windowInputMap == null) {
                    windowInputMap = new ComponentInputMap(this);
                }
                return windowInputMap;
            default:
                throw new IllegalArgumentException("condition must be one of JComponent.WHEN_IN_FOCUSED_WINDOW, "
                        + "JComponent.WHEN_FOCUSED or JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT");
        }
    }

    public final void setInputMap(int condition, InputMap map) {
        switch (condition) {
            case WHEN_FOCUSED:
                focusInputMap = map;
                break;
            case WHEN_ANCESTOR_OF_FOCUSED_COMPONENT:
                ancestorInputMap = map;
                break;
            case WHEN_IN_FOCUSED_WINDOW:
                if (map == null) {
                    windowInputMap = null;
                } else if (map instanceof ComponentInputMap) {
                    windowInputMap = (ComponentInputMap) map;
                } else {
                    throw new IllegalArgumentException(
                            "WHEN_IN_FOCUSED_WINDOW InputMaps must be of type ComponentInputMap");
                }
                break;
            default:
                throw new IllegalArgumentException("condition must be one of JComponent.WHEN_IN_FOCUSED_WINDOW, "
                        + "JComponent.WHEN_FOCUSED or JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT");
        }
    }

    private InputMap inputMapOrNull(int condition) {
        return condition == WHEN_FOCUSED ? focusInputMap
                : condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ? ancestorInputMap
                : condition == WHEN_IN_FOCUSED_WINDOW ? windowInputMap : null;
    }

    public final ActionMap getActionMap() {
        if (actionMap == null) {
            actionMap = new ActionMap();
        }
        return actionMap;
    }

    public final void setActionMap(ActionMap am) {
        actionMap = am;
    }

    public void registerKeyboardAction(ActionListener anAction, String aCommand, KeyStroke aKeyStroke,
            int aCondition) {
        InputMap im = getInputMap(aCondition);
        Standin standin = new Standin(anAction, aCommand);
        im.put(aKeyStroke, standin);
        getActionMap().put(standin, standin);
    }

    public void registerKeyboardAction(ActionListener anAction, KeyStroke aKeyStroke, int aCondition) {
        registerKeyboardAction(anAction, null, aKeyStroke, aCondition);
    }

    public void unregisterKeyboardAction(KeyStroke aKeyStroke) {
        for (int c = WHEN_FOCUSED; c <= WHEN_IN_FOCUSED_WINDOW; c++) {
            InputMap im = inputMapOrNull(c);
            if (im != null) {
                Object key = im.get(aKeyStroke);
                if (key != null && actionMap != null) {
                    actionMap.remove(key);
                }
                im.remove(aKeyStroke);
            }
        }
    }

    public KeyStroke[] getRegisteredKeyStrokes() {
        ArrayList<KeyStroke> l = new ArrayList<KeyStroke>();
        for (int c = WHEN_FOCUSED; c <= WHEN_IN_FOCUSED_WINDOW; c++) {
            InputMap im = inputMapOrNull(c);
            KeyStroke[] ks = im == null ? null : im.allKeys();
            if (ks != null) {
                for (int i = 0; i < ks.length; i++) {
                    l.add(ks[i]);
                }
            }
        }
        return l.toArray(new KeyStroke[l.size()]);
    }

    public int getConditionForKeyStroke(KeyStroke aKeyStroke) {
        for (int c = WHEN_FOCUSED; c <= WHEN_IN_FOCUSED_WINDOW; c++) {
            InputMap im = inputMapOrNull(c);
            if (im != null && im.get(aKeyStroke) != null) {
                return c;
            }
        }
        return UNDEFINED_CONDITION;
    }

    public ActionListener getActionForKeyStroke(KeyStroke aKeyStroke) {
        if (actionMap == null) {
            return null;
        }
        for (int c = WHEN_FOCUSED; c <= WHEN_IN_FOCUSED_WINDOW; c++) {
            InputMap im = inputMapOrNull(c);
            Object key = im == null ? null : im.get(aKeyStroke);
            Action a = key == null ? null : actionMap.get(key);
            if (a != null) {
                return a instanceof Standin ? ((Standin) a).listener : a;
            }
        }
        return null;
    }

    public void resetKeyboardActions() {
        for (int c = WHEN_FOCUSED; c <= WHEN_IN_FOCUSED_WINDOW; c++) {
            InputMap im = inputMapOrNull(c);
            if (im != null) {
                im.clear();
            }
        }
        if (actionMap != null) {
            actionMap.clear();
        }
    }

    /// Runs the action bound to a key stroke under one condition, if this
    /// component is enabled and has one. Answers whether an action ran.
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        InputMap im = inputMapOrNull(condition);
        if (im == null || actionMap == null || !isEnabled()) {
            return false;
        }
        Object key = im.get(ks);
        Action a = key == null ? null : actionMap.get(key);
        return a != null && SwingUtilities.notifyAction(a, ks, e, this, e.getModifiers());
    }

    /// After the key listeners, looks the event up in the key bindings.
    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (!e.isConsumed()) {
            cn1KeyBindings(this, e);
        }
    }

    /// A stop of the tab key also when it has `WHEN_FOCUSED` bindings.
    @Override
    public boolean cn1FocusTraversable() {
        return super.cn1FocusTraversable() || (focusInputMap != null && focusInputMap.size() > 0);
    }

    /// Looks a key event up in the key bindings that apply to `target`,
    /// the component it is for, and consumes it when an action ran.
    public static boolean cn1KeyBindings(Component target, KeyEvent e) {
        KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
        if (ks == null) {
            return false;
        }
        boolean pressed = e.getID() == KeyEvent.KEY_PRESSED;
        boolean done = target instanceof JComponent
                && ((JComponent) target).processKeyBinding(ks, e, WHEN_FOCUSED, pressed);
        Component top = target;
        for (Component c = target; c != null && !done; c = c.getParent()) {
            top = c;
            if (c instanceof Window) {
                break;
            }
            if (c instanceof JComponent) {
                done = ((JComponent) c).processKeyBinding(ks, e, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, pressed);
            }
        }
        if (!done) {
            done = windowBindings(top, ks, e, pressed);
        }
        if (done) {
            e.consume();
        }
        return done;
    }

    private static boolean windowBindings(Component c, KeyStroke ks, KeyEvent e, boolean pressed) {
        if (!c.isVisible() || !c.isEnabled()) {
            return false;
        }
        if (c instanceof JComponent
                && ((JComponent) c).processKeyBinding(ks, e, WHEN_IN_FOCUSED_WINDOW, pressed)) {
            return true;
        }
        if (c instanceof Container) {
            Component[] cs = ((Container) c).getComponents();
            for (int i = 0; i < cs.length; i++) {
                if (windowBindings(cs[i], ks, e, pressed)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// The action a listener registered with `registerKeyboardAction`
    /// becomes: it runs the listener with the command it was given.
    private static final class Standin implements Action {

        final ActionListener listener;
        private final String command;

        Standin(ActionListener listener, String command) {
            this.listener = listener;
            this.command = command;
        }

        @Override
        public Object getValue(String key) {
            if (ACTION_COMMAND_KEY.equals(key)) {
                return command;
            }
            return listener instanceof Action ? ((Action) listener).getValue(key) : null;
        }

        @Override
        public void putValue(String key, Object value) {
        }

        @Override
        public void setEnabled(boolean b) {
        }

        @Override
        public boolean isEnabled() {
            return listener != null && (!(listener instanceof Action) || ((Action) listener).isEnabled());
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            listener.actionPerformed(ae);
        }
    }
}
