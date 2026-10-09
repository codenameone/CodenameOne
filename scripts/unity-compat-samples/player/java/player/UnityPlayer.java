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
package player;

import com.codename1.generated.unity.UnityAppImpl;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.unitycompat.unityengine.Random;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import com.codename1.unitycompat.unityengine.ui.UnityGameView;

/// A Codename One application that is nothing but a Unity project: one
/// form, filled by one [UnityGameView], showing the first scene of the
/// project `build-unity-project.sh` compiled into
/// `com.codename1.generated.unity.UnityAppImpl`.
///
/// It uses the Codename One API alone, so it is the same class on every
/// port; [UnityPlayerDesktop] is the `main` that puts it in a desktop
/// window.
///
/// Two display properties are read, both optional:
///
/// - `AppName`: the title of the form, and so of a desktop window. The
///   launcher sets it to the Unity project's `productName`.
/// - `unity.player.seed`: the seed of `UnityEngine.Random`, for a run that
///   can be repeated. Without it the clock seeds it, as Unity does.
public class UnityPlayer {
    /// Escape, as every port with a keyboard delivers it: its character.
    private static final int KEY_ESCAPE = 27;

    private Form form;
    private UnityGameView view;

    public void init(Object context) {
        // Nothing to prepare: the project is installed when it starts.
    }

    public void start() {
        if (view != null) {
            // Back from the background.
            form.show();
            view.resume();
            return;
        }
        Display display = Display.getInstance();
        UnityRuntime.reset();
        String seed = display.getProperty("unity.player.seed", "");
        Random.InitState(seed.length() > 0 ? Integer.parseInt(seed) : (int) System.currentTimeMillis());
        UnityAppImpl.install();
        // The view reports its real size before every frame; this is for
        // what a script reads in Awake and Start, before the first one.
        UnityRuntime.resize(display.getDisplayWidth(), display.getDisplayHeight());
        UnityRuntime.begin();

        view = new UnityGameView();
        form = new Form(new BorderLayout()) {
            // A form keeps Escape for its back command and never shows it
            // to the component with the focus. A game reads it -- pause,
            // Unity's `Cancel` button -- so it goes to the view first.
            @Override
            public void keyPressed(int keyCode) {
                if (keyCode == KEY_ESCAPE) {
                    view.keyPressed(keyCode);
                    return;
                }
                super.keyPressed(keyCode);
            }

            @Override
            public void keyReleased(int keyCode) {
                if (keyCode == KEY_ESCAPE) {
                    view.keyReleased(keyCode);
                    return;
                }
                super.keyReleased(keyCode);
            }
        };
        form.setTitle(display.getProperty("AppName", "Unity"));
        // The game is the whole window: no title bar of the form's own.
        form.getTitleArea().setHidden(true);
        if (form.getToolbar() != null) {
            form.getToolbar().setHidden(true);
        }
        form.setScrollable(false);
        form.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
        form.getContentPane().getAllStyles().setMargin(0, 0, 0, 0);
        form.add(BorderLayout.CENTER, view);
        form.show();
        view.start();

        // `Application.Quit()` is a request; a frame cannot leave from
        // inside itself, so it is looked for from outside.
        UITimer.timer(250, true, form, new Runnable() {
            public void run() {
                if (UnityRuntime.quitRequested()) {
                    Display.getInstance().exitApplication();
                }
            }
        });
    }

    public void stop() {
        if (view != null) {
            view.pause();
        }
    }

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
