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

import javafx.stage.Stage;

/// The class a JavaFX application extends.
///
/// On Codename One the application is not launched from a `main` method:
/// `com.codename1.fxcompat.runtime.FxLifecycle` creates it and calls
/// [#init()], [#start(Stage)] and [#stop()], all on the JavaFX
/// application thread. `launch`, `getParameters`, `getHostServices` and
/// the user agent style sheet are not part of this layer.
public abstract class Application {

    /// Creates the application.
    public Application() {
    }

    /// Called once before [#start(Stage)]; does nothing unless overridden.
    public void init() throws Exception {
    }

    /// Called once with the primary stage; builds and shows the user
    /// interface.
    public abstract void start(Stage primaryStage) throws Exception;

    /// Called once when the application ends; does nothing unless
    /// overridden.
    public void stop() throws Exception {
    }
}
