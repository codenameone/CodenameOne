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
package com.codename1.fxcompat;

import com.codename1.fxcompat.runtime.SceneEmbed;
import com.codename1.fxcompat.runtime.StageHosts;
import com.codename1.ui.Component;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/// JavaFX inside a Codename One application: the calls that turn a scene
/// graph into a Codename One component.
///
/// ```java
/// // in src/main/desktop/java
/// public class ChartPane {
///     public static com.codename1.ui.Component create() {
///         VBox box = new VBox(8, new Label("Sales"), new TableView<Row>(rows));
///         return FxInterop.asComponent(box);
///     }
/// }
///
/// // anywhere in the Codename One application
/// form.add(BorderLayout.CENTER, ChartPane.create());
/// ```
///
/// Windows need nothing from this class. `new Stage().show()` shows the
/// stage as a form over the current one, with a back command; closing it
/// returns to the form it was shown over, and an `Alert` or a modal stage
/// floats over whatever form is showing, `showAndWait` blocking its caller
/// as on a desktop. In an application with a main class of its own,
/// `Platform.exit()` and the last stage closing leave the application
/// running.
public final class FxInterop {

    private FxInterop() {
    }

    /// Answers a Codename One component that shows a scene graph, laid out
    /// to the size the answered component is given, painted with it, and
    /// receiving the pointer input that falls on it. Its preferred size is
    /// the root's, scaled to the display.
    ///
    /// The argument is a `javafx.scene.Parent` or a `javafx.scene.Scene`.
    /// A root that is the root of a scene already is shown with that scene,
    /// its style sheets included; any other is given a scene of its own.
    /// Pass a scene to give it style sheets or a fill; one that a stage is
    /// showing leaves that stage. Call this on the event dispatch thread,
    /// which is the JavaFX application thread.
    ///
    /// The parameter is declared as `Object` because the build relocates
    /// the JavaFX classes an application names: the application is compiled
    /// against `javafx.scene.Parent`, and what reaches this method on a
    /// device is the relocated class. Anything that is neither a parent nor
    /// a scene is refused with an `IllegalArgumentException`.
    ///
    /// #### Parameters
    ///
    /// - `sceneOrRoot`: the `Scene` to show, or the `Parent` at the root of
    ///   the scene graph to show
    ///
    /// #### Returns
    ///
    /// the Codename One component that shows it
    public static Component asComponent(Object sceneOrRoot) {
        Scene scene;
        if (sceneOrRoot instanceof Scene) {
            scene = (Scene) sceneOrRoot;
        } else if (sceneOrRoot instanceof Parent) {
            Parent root = (Parent) sceneOrRoot;
            scene = root.getScene();
            if (scene == null || scene.getRoot() != root) {
                scene = new Scene(root);
            }
        } else {
            throw new IllegalArgumentException("asComponent takes a javafx.scene.Parent or a javafx.scene.Scene, not "
                    + (sceneOrRoot == null ? "null" : sceneOrRoot.getClass().getName()));
        }
        Stage stage = new Stage();
        SceneEmbed embed = new SceneEmbed(stage);
        stage.setScene(scene);
        stage.cn1Embed(embed);
        return embed;
    }

    /// Says whether stages other than the primary one open as windows of
    /// the platform's window manager where it has one, which is the
    /// default. With `false` every stage stays inside the application's
    /// own window: a modal stage or an `Alert` is a dialog over the current
    /// form and any other stage a form, as on a phone.
    ///
    /// #### Parameters
    ///
    /// - `nativeWindows`: `false` to keep every stage inside the
    ///   application's window
    public static void setNativeWindows(boolean nativeWindows) {
        StageHosts.setNativeWindows(nativeWindows);
    }
}
