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
package com.codename1.unitycompat.app;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.unitycompat.unityengine.Random;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import com.codename1.unitycompat.unityengine.ui.UnityGameView;

/// The main class of a Codename One application that is a Unity project: one
/// form, filled by one [UnityGameView], showing the project's first scene.
///
/// It is abstract for one reason. The scenes are compiled, at build time,
/// into a class of the application (`com.codename1.generated.unity.UnityAppImpl`),
/// and this class is compiled long before that one exists. A device build has
/// no reflection to find it by name, so the application's main class names it
/// in source:
///
/// ```java
/// public class MyGame extends UnityApplication {
///     @Override
///     protected void installProject() {
///         com.codename1.generated.unity.UnityAppImpl.install();
///     }
/// }
/// ```
///
/// The build generates exactly that class when the application has no main
/// class of its own, and `cn1:import-unity-project` writes it. Write it by
/// hand to do more. In the order they are called:
///
/// - [#onProjectInstalled()]: the project is installed and no scene is
///   loaded yet. The place to give the scripts what they will ask for in
///   their first `Awake`, by setting a static field of a C# class.
/// - [#createForm]: to put the game beside other components.
/// - [#onStarted]: the first scene is loaded and the game loop runs.
///
/// Scripts run inside the game's frame, which a port may draw on a thread
/// other than the event dispatch thread. Code of the application that
/// touches anything a script owns goes through [#callInFrame(Runnable)].
///
/// Two display properties are read, both optional:
///
/// - `AppName`: the title of the form, and so of a desktop window.
/// - `unity.seed`: the seed of `UnityEngine.Random`, for a run that can be
///   repeated. Without it the clock seeds it, as Unity does.
public abstract class UnityApplication {
    /// Escape, as every port with a keyboard delivers it: its character.
    private static final int KEY_ESCAPE = 27;

    private Form form;
    private UnityGameView view;

    /// Makes the application's Unity project the one the runtime runs: a call
    /// to the generated `UnityAppImpl.install()`.
    protected abstract void installProject();

    /// The lifecycle's first call. Nothing to prepare: the project is
    /// installed when it starts.
    public void init(Object context) {
    }

    /// Starts the game, or continues it when the application comes back from
    /// the background.
    public void start() {
        if (view != null) {
            form.show();
            view.resume();
            return;
        }
        Display display = Display.getInstance();
        UnityRuntime.reset();
        Random.InitState(seed(display.getProperty("unity.seed", "")));
        installProject();
        onProjectInstalled();
        // The view reports its real size before every frame; this is for
        // what a script reads in Awake and Start, before the first one.
        UnityRuntime.resize(display.getDisplayWidth(), display.getDisplayHeight());
        UnityRuntime.begin();

        view = new UnityGameView();
        form = createForm(view);
        form.show();
        view.start();
        onStarted(form, view);

        // `Application.Quit()` is a request; a frame cannot leave from
        // inside itself, so it is looked for from outside.
        UITimer.timer(250, true, form, new QuitWatch());
    }

    /// The seed a property names, or the clock's when it names none. Parsed
    /// by hand: a property is text anyone can set, and a number that is not
    /// one must not stop the game from starting.
    private static int seed(String text) {
        boolean negative = text.startsWith("-");
        int value = 0;
        int digits = 0;
        for (int i = negative ? 1 : 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                digits = 0;
                break;
            }
            value = value * 10 + (c - '0');
            digits++;
        }
        if (digits == 0) {
            return (int) System.currentTimeMillis();
        }
        return negative ? -value : value;
    }

    /// Leaves the application once a script has asked to.
    private static final class QuitWatch implements Runnable {
        @Override
        public void run() {
            if (UnityRuntime.quitRequested()) {
                Display.getInstance().exitApplication();
            }
        }
    }

    /// A form keeps Escape for its back command and never shows it to the
    /// component with the focus. A game reads it -- pause, Unity's `Cancel`
    /// button -- so it goes to the view first.
    private static final class GameForm extends Form {
        private final UnityGameView gameView;

        GameForm(UnityGameView gameView) {
            super(new BorderLayout());
            this.gameView = gameView;
        }

        @Override
        public void keyPressed(int keyCode) {
            if (keyCode == KEY_ESCAPE) {
                gameView.keyPressed(keyCode);
                return;
            }
            super.keyPressed(keyCode);
        }

        @Override
        public void keyReleased(int keyCode) {
            if (keyCode == KEY_ESCAPE) {
                gameView.keyReleased(keyCode);
                return;
            }
            super.keyReleased(keyCode);
        }
    }

    /// The form the game is shown in: the view and nothing else, with no
    /// title bar of the form's own. Override to lay the view out differently;
    /// the form returned must contain it.
    protected Form createForm(final UnityGameView gameView) {
        Form f = new GameForm(gameView);
        f.setTitle(Display.getInstance().getProperty("AppName", "Unity"));
        f.getTitleArea().setHidden(true);
        if (f.getToolbar() != null) {
            f.getToolbar().setHidden(true);
        }
        f.setScrollable(false);
        f.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
        f.getContentPane().getAllStyles().setMargin(0, 0, 0, 0);
        f.add(BorderLayout.CENTER, gameView);
        return f;
    }

    /// Called once, after [#installProject()] and before the first scene is
    /// built, so before any script's `Awake`. The translated classes are
    /// loaded and their static fields can be set; no game object exists
    /// yet. Does nothing; there for a subclass:
    ///
    /// ```java
    /// @Override
    /// protected void onProjectInstalled() {
    ///     Platform.Services = new GameServices(this);
    /// }
    /// ```
    protected void onProjectInstalled() {
    }

    /// Runs `call` inside the game's frame: at the start of the next one,
    /// before any script. Safe from any thread and at any time after the
    /// project is installed -- from [#onProjectInstalled()] on -- with or
    /// without a view; see [UnityGameView#callInFrame(Runnable)].
    public static void callInFrame(Runnable call) {
        UnityRuntime.callInFrame(call);
    }

    /// Called once, after the first scene is loaded and the game loop runs.
    /// Does nothing; there for a subclass.
    protected void onStarted(Form gameForm, UnityGameView gameView) {
    }

    /// The application went to the background: the game loop pauses, so no
    /// frame is simulated while nobody sees it and `Time.time` does not jump
    /// when it returns.
    public void stop() {
        if (view != null) {
            view.pause();
        }
    }

    /// The application is ending.
    public void destroy() {
        if (view != null) {
            view.stop();
        }
    }

    /// The view the project is shown in, once started.
    public UnityGameView getView() {
        return view;
    }

    /// The form that holds the view, once started.
    public Form getForm() {
        return form;
    }
}
