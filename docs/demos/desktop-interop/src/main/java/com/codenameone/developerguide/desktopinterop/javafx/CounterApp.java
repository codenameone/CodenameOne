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

// tag::desktopInteropJavaFxApp[]
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CounterApp extends Application {
    private final IntegerProperty count = new SimpleIntegerProperty();

    @Override
    public void start(Stage stage) {
        Label label = new Label();
        label.textProperty().bind(Bindings.concat("Pressed ", count.asString(), " times"));

        Button press = new Button("Press");
        press.setOnAction(e -> count.set(count.get() + 1));

        VBox root = new VBox(12, label, press);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(16));

        stage.setTitle("Counter");
        stage.setScene(new Scene(root, 320, 200));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
// end::desktopInteropJavaFxApp[]
