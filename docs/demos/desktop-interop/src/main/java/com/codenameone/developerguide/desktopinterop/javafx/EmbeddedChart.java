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
package com.codenameone.developerguide.desktopinterop.javafx;

import com.codename1.fxcompat.FxInterop;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/// A Codename One screen that shows a JavaFX scene graph, and a stage opened
/// from a Codename One application.
public class EmbeddedChart {

    public void show() {
        // tag::desktopInteropEmbedFx[]
        VBox root = new VBox(8, new Label("Orders this week"), new Button("Refresh"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/com/example/orders.css");

        Form form = new Form("Report", new BorderLayout());
        form.add(BorderLayout.CENTER, FxInterop.asComponent(scene));
        form.show();
        // end::desktopInteropEmbedFx[]
    }

    public void openEditor(VBox editor) {
        // tag::desktopInteropFxWindows[]
        Stage stage = new Stage();
        stage.setTitle("Edit order");
        stage.setScene(new Scene(editor));
        stage.show();   // closing it returns to the form that was showing
        // end::desktopInteropFxWindows[]
    }
}
