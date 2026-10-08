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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.io.Log;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

/// The Codename One lifecycle of a desktop application.
///
/// The build generates one subclass per application. It implements the
/// one abstract method by calling the application's entry point:
///
/// ```java
/// public final class MyAppLifecycle extends DesktopLifecycle {
///     protected void runMain() throws Exception {
///         com.example.MyApp.main(new String[0]);
///     }
/// }
/// ```
///
/// The contract, in the order things happen:
///
/// 1. **Start.** The first `start()` calls `runMain()` once, on the event
///    dispatch thread -- the thread a Swing application is supposed to
///    build its frames on, so `SwingUtilities.invokeLater` from `main`
///    runs the runnable after `main` returns, and `invokeAndWait` runs it
///    at once. `main` receives no arguments. An exception it throws is
///    logged and the application goes on.
/// 2. **After `main` returns** the application stays alive: nothing
///    here depends on `main` blocking. Once the events `main` queued
///    have run, if no window is showing, a blank form titled with the
///    application's name is shown, so that the user never looks at an
///    empty screen. It is replaced by the first window that shows.
/// 3. **Stop** (the application goes to the background): every showing
///    frame receives `windowIconified` and the active window
///    `windowDeactivated`. A modal dialog that is showing stays open.
/// 4. **Start again**: the same windows receive `windowDeiconified`, the
///    active one `windowActivated`, and the screen shows what it showed.
/// 5. **Destroy** (the platform ends the application): every showing
///    window receives `windowClosing`, the one on top first, as if its
///    close box was used. `EXIT_ON_CLOSE` does not exit a second time.
/// 6. **Exit.** When the last window that was shown or packed is disposed
///    of, the application exits, as a desktop JVM does once no
///    displayable window and no other thread is left. Hiding the last
///    window without disposing of it shows the blank form of step 2
///    instead. `JFrame.EXIT_ON_CLOSE` exits at once.
public abstract class DesktopLifecycle extends Lifecycle {

    private boolean ran;
    private boolean destroying;
    private Form blank;

    /// Calls the application's `main`.
    protected abstract void runMain() throws Exception;

    @Override
    public void start() {
        if (ran) {
            Form current = getCurrentForm();
            if (current != null && Display.getInstance().getCurrent() != current) {
                current.show();
            }
            background(false);
            return;
        }
        runApp();
    }

    @Override
    public void runApp() {
        ran = true;
        WindowHosts.setIdleHook(new Runnable() {
            @Override
            public void run() {
                idle();
            }
        });
        WindowHosts.setExitHook(new Runnable() {
            @Override
            public void run() {
                if (!destroying) {
                    Display.getInstance().exitApplication();
                }
            }
        });
        try {
            runMain();
        } catch (Exception e) {
            Log.e(e);
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                idle();
            }
        });
    }

    @Override
    public void stop() {
        setCurrentForm(Display.getInstance().getCurrent());
        background(true);
    }

    @Override
    public void destroy() {
        destroying = true;
        Window[] ws = WindowHosts.showing();
        for (int i = ws.length - 1; i >= 0; i--) {
            ws[i].cn1Closing();
        }
    }

    private static void background(boolean iconified) {
        Window[] ws = WindowHosts.showing();
        Window active = WindowHosts.active();
        if (iconified && active != null) {
            active.cn1Activated(false, null);
        }
        for (int i = 0; i < ws.length; i++) {
            ws[i].cn1Iconified(iconified);
        }
        if (!iconified && active != null) {
            active.cn1Activated(true, null);
        }
    }

    /// No window is showing: exit when none is left at all, else show the
    /// blank form.
    private void idle() {
        if (destroying) {
            return;
        }
        if (WindowHosts.showing().length > 0) {
            return;
        }
        if (WindowHosts.everShown() && WindowHosts.windows().length == 0) {
            Display.getInstance().exitApplication();
            return;
        }
        Form current = Display.getInstance().getCurrent();
        if (current == null || current instanceof FrameForm) {
            if (blank == null) {
                String name = Display.getInstance().getProperty("AppName", "");
                blank = new Form(name == null ? "" : name, new BorderLayout());
                blank.add(BorderLayout.CENTER, new Label(""));
            }
            blank.show();
        }
    }
}
