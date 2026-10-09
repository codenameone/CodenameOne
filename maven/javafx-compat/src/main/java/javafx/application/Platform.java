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
package javafx.application;

import com.codename1.fxcompat.runtime.StageHosts;
import com.codename1.ui.Display;

/// Access to the JavaFX application thread, which is the Codename One
/// event dispatch thread.
public final class Platform {

    private static boolean implicitExit = true;

    private Platform() {
    }

    /// Runs code on the JavaFX application thread at some later time, in
    /// the order it was asked for. Without a Codename One display, as in
    /// a plain unit test, it runs at once on the calling thread.
    public static void runLater(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable must not be null");
        }
        if (Display.isInitialized()) {
            Display.getInstance().callSerially(runnable);
        } else {
            runnable.run();
        }
    }

    /// Returns whether the caller is on the JavaFX application thread.
    /// Without a Codename One display every thread counts as it.
    public static boolean isFxApplicationThread() {
        return !Display.isInitialized() || Display.getInstance().isEdt();
    }

    /// Ends the application: `Application.stop()` is called and Codename
    /// One exits.
    public static void exit() {
        StageHosts.exit();
    }

    /// Sets whether the application ends when its last window is hidden.
    public static void setImplicitExit(boolean value) {
        implicitExit = value;
    }

    /// Returns whether the application ends when its last window is
    /// hidden.
    public static boolean isImplicitExit() {
        return implicitExit;
    }

    /// Returns whether a part of JavaFX is there to be used.
    ///
    /// The scene graph, the controls, FXML, clipping to a shape and a
    /// pointing device are. Touch events are not: every pointer reaches
    /// the scene as a mouse event, a finger included, so an application
    /// that asks before it hides the cursor or wires touch handlers is
    /// told the truth. Everything else answers `false`.
    public static boolean isSupported(ConditionalFeature feature) {
        return feature == ConditionalFeature.GRAPHICS || feature == ConditionalFeature.CONTROLS
                || feature == ConditionalFeature.FXML || feature == ConditionalFeature.SHAPE_CLIP
                || feature == ConditionalFeature.INPUT_POINTER;
    }

    /// The toolkit needs no starting on Codename One; the code is run on
    /// the JavaFX application thread as [#runLater(Runnable)] would.
    public static void startup(Runnable runnable) {
        runLater(runnable);
    }
}
