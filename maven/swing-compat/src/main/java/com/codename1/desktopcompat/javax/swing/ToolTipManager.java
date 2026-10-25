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

import com.codename1.desktopcompat.java.awt.event.MouseMotionListener;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Window;

/// The one switch for every tool tip of the application.
///
/// A tool tip is shown by the Codename One widget behind a component, with
/// the delays of the Codename One tool tip manager. Disabling here takes
/// the tips off the widgets and enabling puts them back; the delays are
/// kept and answered and do not change when a tip appears.
public class ToolTipManager {

    private static final ToolTipManager SHARED = new ToolTipManager();

    private boolean enabled = true;
    private int initialDelay = 750;
    private int dismissDelay = 4000;
    private int reshowDelay = 500;

    ToolTipManager() {
    }

    public static ToolTipManager sharedInstance() {
        return SHARED;
    }

    public void setEnabled(boolean flag) {
        if (enabled == flag) {
            return;
        }
        enabled = flag;
        Window[] all = Window.getWindows();
        for (int i = 0; i < all.length; i++) {
            apply(all[i]);
        }
    }

    private static void apply(Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).cn1ApplyToolTip();
        }
        if (c instanceof Container) {
            Container p = (Container) c;
            for (int i = 0; i < p.getComponentCount(); i++) {
                apply(p.getComponent(i));
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setInitialDelay(int milliseconds) {
        initialDelay = milliseconds;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    public void setDismissDelay(int milliseconds) {
        dismissDelay = milliseconds;
    }

    public int getDismissDelay() {
        return dismissDelay;
    }

    public void setReshowDelay(int milliseconds) {
        reshowDelay = milliseconds;
    }

    public int getReshowDelay() {
        return reshowDelay;
    }

    private final Tips tips = new Tips();

    /// Makes `component` show a tool tip: from here on the pointer
    /// entering it or moving over it asks its
    /// `getToolTipText(MouseEvent)` for the text at that place. As in
    /// Swing this is done with a mouse listener, so the component gets
    /// the mouse events over it from then on.
    public void registerComponent(JComponent component) {
        component.removeMouseListener(tips);
        component.addMouseListener(tips);
        component.removeMouseMotionListener(tips);
        component.addMouseMotionListener(tips);
        component.cn1ApplyToolTip();
    }

    public void unregisterComponent(JComponent component) {
        component.removeMouseListener(tips);
        component.removeMouseMotionListener(tips);
        component.cn1ApplyToolTip();
    }

    /// Asks a component for the tool tip at the pointer as it moves.
    private static final class Tips extends MouseAdapter {

        private static void ask(MouseEvent e) {
            Object src = e.getSource();
            if (src instanceof JComponent) {
                ((JComponent) src).cn1ToolTipAt(e);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            ask(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            ask(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }
    }
}
