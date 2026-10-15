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

import com.codename1.compat.jdk.CompatBoot;
import com.codename1.io.Log;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Display;

import javafx.application.Application;
import javafx.stage.Stage;

/// The Codename One lifecycle of a JavaFX application.
///
/// #### Contract
///
/// The application's Codename One main class -- the class
/// `codename1.packageName` and `codename1.mainName` name, an ordinary source
/// file of the project -- extends this class and implements one method.
/// `cn1:import-desktop-project` writes that class:
///
/// ```java
/// public class MyApp extends FxLifecycle {
///     protected Application createApplication() {
///         return new com.example.MyFxApp();
///     }
/// }
/// ```
///
/// Extending this class is what makes the JavaFX stages THE application:
/// [#runApp()] installs the [StageHosts#setExitHook(Runnable)] hook that
/// exits it. A Codename One application that only uses some JavaFX scenes
/// (library mode) has a main class that does not extend it, and the last
/// stage closing returns to the form it was shown over.
///
/// Codename One then drives it as any `Lifecycle`:
///
/// - `init(Object)`: the inherited behaviour (theme, network error
///   handling); nothing JavaFX runs yet.
/// - `start()`, the first time: on the event dispatch thread, which is
///   the JavaFX application thread, the application's resources are
///   installed (`CompatBoot.cn1Init()`), [#createApplication()] is called once,
///   then `Application.init()`, then `Application.start(Stage)` with a
///   new primary stage. The stage is a form (see [StageHosts]); the
///   application shows it by calling `Stage.show()`.
/// - `start()`, after a `stop()`: the form that was showing is shown
///   again; the application is not restarted.
/// - `stop()`: the inherited behaviour, remembering the current form.
/// - `destroy()`: `Application.stop()` is called, once.
///
/// `Platform.exit()`, and the last window closing while
/// `Platform.isImplicitExit()`, call `Application.stop()` once and then
/// `Display.exitApplication()`.
///
/// An exception from `init`, `start` or `stop` is logged and rethrown as
/// a `RuntimeException`. `Application.launch` is not used: there is no
/// `main` method on a device, and no reflection to find the class with.
/// The application's `main` is never called, and `launch` does nothing
/// when something else calls it. `Application.getParameters()` answers
/// empty parameters.
public abstract class FxLifecycle extends Lifecycle {

    private Application application;
    private Stage primaryStage;
    private boolean stopped;

    /// Creates the application; called once, on the event dispatch thread.
    protected abstract Application createApplication();

    /// Returns the application, or `null` before the first `start()`.
    public final Application getApplication() {
        return application;
    }

    /// Returns the primary stage, or `null` before the first `start()`.
    public final Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void runApp() {
        // Before any application code: its resources and bundles.
        CompatBoot.cn1Init();
        application = createApplication();
        if (application == null) {
            throw new IllegalStateException("createApplication() answered null");
        }
        stopped = false;
        StageHosts.setExitHook(new Runnable() {
            @Override
            public void run() {
                stopApplication();
                if (Display.isInitialized()) {
                    Display.getInstance().exitApplication();
                }
            }
        });
        primaryStage = new Stage();
        primaryStage.cn1MarkPrimary();
        try {
            application.init();
            application.start(primaryStage);
        } catch (RuntimeException e) {
            Log.e(e);
            throw e;
        } catch (Exception e) {
            Log.e(e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void stopApplication() {
        if (application == null || stopped) {
            return;
        }
        stopped = true;
        try {
            application.stop();
        } catch (RuntimeException e) {
            Log.e(e);
            throw e;
        } catch (Exception e) {
            Log.e(e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void destroy() {
        stopApplication();
        super.destroy();
    }
}
