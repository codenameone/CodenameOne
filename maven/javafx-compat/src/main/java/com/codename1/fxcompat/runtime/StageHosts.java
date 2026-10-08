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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Desktop;
import com.codename1.ui.Display;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/// Chooses what a window is on screen.
///
/// - The primary stage is always a form, [StageForm]: it is the
///   application's main surface on every device.
/// - Any other window is a desktop window, [StageWindow], where
///   `com.codename1.ui.Desktop.isSupported()`.
/// - Elsewhere a stage with a modality is a Codename One dialog,
///   [StageDialog], and any other window a form shown on top of the
///   current one, whose back command asks the window to close and returns
///   to the form below.
public final class StageHosts {

    private static Runnable exitHook;

    private StageHosts() {
    }

    /// Creates the host of a window that is about to be shown.
    public static StageHost create(Window window) {
        boolean primary = window instanceof Stage && ((Stage) window).cn1IsPrimary();
        if (!primary && Display.isInitialized() && Desktop.isSupported()) {
            return new StageWindow(window);
        }
        if (!primary && window instanceof Stage && ((Stage) window).getModality() != Modality.NONE) {
            return new StageDialog(window);
        }
        return new StageForm(window, primary);
    }

    /// Installs what ends the application; the lifecycle does.
    public static void setExitHook(Runnable hook) {
        exitHook = hook;
    }

    /// Ends the application, as `Platform.exit()` asks.
    public static void exit() {
        Runnable hook = exitHook;
        if (hook != null) {
            hook.run();
        }
    }

    /// The last showing window was hidden.
    public static void lastWindowHidden() {
        if (javafx.application.Platform.isImplicitExit()) {
            exit();
        }
    }
}
