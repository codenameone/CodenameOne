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

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.rt.MenuBridge;

/// The single child of a Swing window.
///
/// It holds a layered pane, which fills it and carries the menu bar and
/// the content pane in its lowest layer, and a glass pane over
/// everything, which is invisible until an application shows it. Popup
/// menus open in the popup layer of the layered pane.
///
/// The default button is clicked by the enter key when no listener and
/// no key binding used the key. A visible glass pane keeps mouse events
/// from the components under it; the Codename One widgets under it still
/// react to the pointer.
public class JRootPane extends JComponent {

    public static final int NONE = 0;
    public static final int FRAME = 1;
    public static final int PLAIN_DIALOG = 2;
    public static final int INFORMATION_DIALOG = 3;
    public static final int ERROR_DIALOG = 4;
    public static final int COLOR_CHOOSER_DIALOG = 5;
    public static final int FILE_CHOOSER_DIALOG = 6;
    public static final int QUESTION_DIALOG = 7;
    public static final int WARNING_DIALOG = 8;

    protected JMenuBar menuBar;
    protected Container contentPane;
    protected JLayeredPane layeredPane;
    protected Component glassPane;
    protected JButton defaultButton;

    private int windowDecorationStyle;

    public JRootPane() {
        setGlassPane(createGlassPane());
        setLayeredPane(createLayeredPane());
        setContentPane(createContentPane());
        setLayout(createRootLayout());
    }

    protected JLayeredPane createLayeredPane() {
        JLayeredPane p = new JLayeredPane();
        p.setName(getName() + ".layeredPane");
        return p;
    }

    protected Container createContentPane() {
        JPanel p = new JPanel(new BorderLayout());
        p.setName(getName() + ".contentPane");
        return p;
    }

    protected Component createGlassPane() {
        JPanel p = new JPanel();
        p.setName(getName() + ".glassPane");
        p.setVisible(false);
        p.setOpaque(false);
        return p;
    }

    protected LayoutManager createRootLayout() {
        return new RootLayout(this);
    }

    public int getWindowDecorationStyle() {
        return windowDecorationStyle;
    }

    /// Recorded only: windows are decorated by their host.
    public void setWindowDecorationStyle(int windowDecorationStyle) {
        if (windowDecorationStyle < NONE || windowDecorationStyle > WARNING_DIALOG) {
            throw new IllegalArgumentException("Invalid decoration style");
        }
        this.windowDecorationStyle = windowDecorationStyle;
    }

    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    /// Sets the menu bar of the window.
    ///
    /// Where the window's host takes commands -- a frame on any device --
    /// the menus are not drawn in the window. Every enabled or disabled
    /// item becomes one Codename One command, see
    /// [com.codename1.desktopcompat.rt.MenuBridge]. In a dialog that
    /// floats over a form the menu bar is drawn as a row of menus above
    /// the content pane.
    public void setJMenuBar(JMenuBar menu) {
        if (menuBar != null && menuBar.getParent() == layeredPane) {
            layeredPane.remove(menuBar);
        }
        menuBar = menu;
        if (menu != null) {
            layeredPane.add(menu, JLayeredPane.FRAME_CONTENT_LAYER);
        }
        MenuBridge.barChanged(this);
        revalidate();
    }

    public Container getContentPane() {
        return contentPane;
    }

    public void setContentPane(Container content) {
        if (content == null) {
            throw new IllegalArgumentException("contentPane cannot be set to null.");
        }
        if (contentPane != null && contentPane.getParent() == layeredPane) {
            layeredPane.remove(contentPane);
        }
        contentPane = content;
        layeredPane.add(content, JLayeredPane.FRAME_CONTENT_LAYER);
        revalidate();
    }

    public JLayeredPane getLayeredPane() {
        return layeredPane;
    }

    public void setLayeredPane(JLayeredPane layered) {
        if (layered == null) {
            throw new IllegalArgumentException("layeredPane cannot be set to null.");
        }
        if (layeredPane != null && layeredPane.getParent() == this) {
            remove(layeredPane);
        }
        layeredPane = layered;
        add(layered, -1);
    }

    public Component getGlassPane() {
        return glassPane;
    }

    public void setGlassPane(Component glass) {
        if (glass == null) {
            throw new NullPointerException("glassPane cannot be set to null.");
        }
        boolean visible = false;
        if (glassPane != null && glassPane.getParent() == this) {
            remove(glassPane);
            visible = glassPane.isVisible();
        }
        glass.setVisible(visible);
        glassPane = glass;
        add(glass, 0);
        if (visible) {
            repaint();
        }
    }

    public JButton getDefaultButton() {
        return defaultButton;
    }

    /// Sets the button the enter key clicks.
    public void setDefaultButton(JButton defaultButton) {
        JButton old = this.defaultButton;
        this.defaultButton = defaultButton;
        firePropertyChange("defaultButton", old, defaultButton);
    }

    @Override
    public boolean isValidateRoot() {
        return true;
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (super.processKeyBinding(ks, e, condition, pressed)) {
            return true;
        }
        if (condition == WHEN_IN_FOCUSED_WINDOW && pressed && e.getID() == KeyEvent.KEY_PRESSED
                && e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiersEx() == 0) {
            JButton b = defaultButton;
            if (b != null && b.isEnabled() && b.isShowing()) {
                b.doClick();
                return true;
            }
        }
        return false;
    }

    /// The layered pane and the glass pane fill the root pane; the menu
    /// bar takes its preferred height at the top unless the host shows it
    /// as commands, and the content pane takes the rest.
    private static final class RootLayout implements LayoutManager {

        private final JRootPane root;

        RootLayout(JRootPane root) {
            this.root = root;
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        private int barHeight() {
            JMenuBar mb = root.menuBar;
            if (mb == null || !mb.isVisible() || mb.cn1Bridged()) {
                return 0;
            }
            return mb.getPreferredSize().height;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return size(true);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return size(false);
        }

        private Dimension size(boolean preferred) {
            Insets in = root.getInsets();
            Container cp = root.contentPane;
            Dimension d = cp == null ? new Dimension(0, 0) : preferred ? cp.getPreferredSize() : cp.getMinimumSize();
            int w = d.width;
            int h = d.height;
            int bh = barHeight();
            if (bh > 0) {
                w = Math.max(w, root.menuBar.getPreferredSize().width);
                h += bh;
            }
            return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = root.getInsets();
            int w = root.getWidth() - in.left - in.right;
            int h = root.getHeight() - in.top - in.bottom;
            if (root.layeredPane != null) {
                root.layeredPane.setBounds(in.left, in.top, w, h);
            }
            if (root.glassPane != null) {
                root.glassPane.setBounds(in.left, in.top, w, h);
            }
            int bh = barHeight();
            if (root.menuBar != null) {
                root.menuBar.setBounds(0, 0, bh > 0 ? w : 0, bh);
            }
            if (root.contentPane != null) {
                root.contentPane.setBounds(0, bh, w, Math.max(0, h - bh));
            }
        }
    }
}
