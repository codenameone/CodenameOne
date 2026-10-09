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

    /// A component's tip follows its tool tip text without registering.
    public void registerComponent(JComponent component) {
        component.cn1ApplyToolTip();
    }

    /// A component's tip follows its tool tip text; set that to `null` to
    /// take the tip away.
    public void unregisterComponent(JComponent component) {
        component.cn1ApplyToolTip();
    }
}
