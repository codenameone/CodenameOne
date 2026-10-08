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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The class a JavaFX application extends.
///
/// On Codename One the application is not launched from a `main` method:
/// `com.codename1.fxcompat.runtime.FxLifecycle` creates it and calls
/// [#init()], [#start(Stage)] and [#stop()], all on the JavaFX
/// application thread. The application's `main` is never called, so
/// [#launch(String...)] is never reached from it; see there for what it
/// does when something else calls it. The user agent style sheet is not
/// part of this layer.
public abstract class Application {

    private HostServices hostServices;

    /// Creates the application.
    public Application() {
    }

    /// Does nothing. On a desktop this creates the application and blocks
    /// until it exits; here the lifecycle has already created the
    /// application and started it before any application code runs, so by
    /// the time this can be called there is nothing left to launch. It
    /// returns at once, and the application that is running goes on.
    public static void launch(String... args) {
        // Deliberately empty: see the comment above.
    }

    /// Does nothing, as [#launch(String...)]: the application class is
    /// named to the build, which starts it, and not looked up here.
    public static void launch(Class<? extends Application> appClass, String... args) {
        // Deliberately empty: see the comment above.
    }

    /// The command line of the application, which on a device has none:
    /// every list and the map are empty and cannot be changed.
    public final Parameters getParameters() {
        return new EmptyParameters();
    }

    /// The services of the platform the application runs on.
    public final HostServices getHostServices() {
        if (hostServices == null) {
            hostServices = new HostServices();
        }
        return hostServices;
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

    /// The arguments an application was started with.
    public abstract static class Parameters {

        /// Creates the parameters.
        public Parameters() {
        }

        /// Every argument, as it was given.
        public abstract List<String> getRaw();

        /// The arguments that are not of the form `--name=value`.
        public abstract List<String> getUnnamed();

        /// The `--name=value` arguments, by name.
        public abstract Map<String, String> getNamed();
    }

    /// What [#getParameters()] answers: nothing. Each call hands out
    /// collections of its own, so no caller can change what another sees.
    private static final class EmptyParameters extends Parameters {
        @Override
        public List<String> getRaw() {
            return new ArrayList<String>();
        }

        @Override
        public List<String> getUnnamed() {
            return new ArrayList<String>();
        }

        @Override
        public Map<String, String> getNamed() {
            return new HashMap<String, String>();
        }
    }
}
