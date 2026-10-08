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
package com.codename1.desktopcompat;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.rt.EmbedHost;
import com.codename1.desktopcompat.rt.WindowHosts;

/// Swing inside a Codename One application: the one call that turns a
/// Swing component tree into a Codename One component.
///
/// ```java
/// // in src/main/desktop/java
/// public class ReportPanel {
///     public static com.codename1.ui.Component create() {
///         JPanel panel = new JPanel(new BorderLayout());
///         panel.add(new JScrollPane(new JTable(rows, columns)));
///         return SwingInterop.asComponent(panel);
///     }
/// }
///
/// // anywhere in the Codename One application
/// form.add(BorderLayout.CENTER, ReportPanel.create());
/// ```
///
/// Windows need nothing from this class. `JFrame.setVisible(true)` shows
/// the frame as a form over the current one, with a back command;
/// closing it returns to the form it was shown over, and a `JDialog` or
/// `JOptionPane` floats over whatever form is showing and blocks its caller
/// as on a desktop. In an application with a main class of its own,
/// `EXIT_ON_CLOSE` closes the Swing windows and leaves the application
/// running.
public final class SwingInterop {

    private SwingInterop() {
    }

    /// Answers a Codename One component that shows `awtComponent` -- a
    /// `java.awt.Component` that is not a window -- laid out to the size
    /// the answered component is given, painted with it, and receiving the
    /// pointer input that falls on it. Its preferred size is the Swing
    /// component's, scaled to the display.
    ///
    /// The parameter is declared as `Object` because the build relocates
    /// the Swing classes: the caller's `java.awt.Component` is this layer's
    /// class by the time the code runs, and no single declared type is
    /// right both when the caller compiles and when it runs.
    ///
    /// The Swing component leaves the container it was in. Call this on the
    /// event dispatch thread, as for all Swing and Codename One UI work.
    ///
    /// #### Parameters
    ///
    /// - `awtComponent`: the `java.awt.Component` to show
    ///
    /// #### Returns
    ///
    /// the Codename One component that shows it
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: `awtComponent` is not a Swing or AWT
    ///   component, or is a window. Show a window with `setVisible(true)`,
    ///   or embed its content pane.
    public static com.codename1.ui.Component asComponent(Object awtComponent) {
        if (awtComponent instanceof Window) {
            throw new IllegalArgumentException("A window cannot be embedded; show it with setVisible(true), or "
                    + "embed its content pane");
        }
        if (!(awtComponent instanceof Component)) {
            throw new IllegalArgumentException("Not a java.awt.Component: "
                    + (awtComponent == null ? "null" : awtComponent.getClass().getName()));
        }
        return new EmbedHost((Component) awtComponent);
    }

    /// Says whether the windows after the first open as windows of the
    /// platform's window manager where it has one, which is the default.
    /// With `false` every window stays inside the application's own
    /// window: a frame is a form, and a dialog or a `JOptionPane` floats
    /// over the current form, as on a phone.
    ///
    /// #### Parameters
    ///
    /// - `nativeWindows`: `false` to keep every window inside the
    ///   application's window
    public static void setNativeWindows(boolean nativeWindows) {
        WindowHosts.setNativeWindows(nativeWindows);
    }
}
