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
package com.example.fxgallery;

import java.util.Arrays;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * The standard dialogs, a context menu and a second window.
 */
public class DialogsTab extends VBox {

    private final Label result = new Label("No dialog shown yet");
    private int windows;

    public DialogsTab() {
        super(10);
        setPadding(new Insets(12));

        Button confirm = new Button("Confirm");
        confirm.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete");
            alert.setHeaderText("Delete the selected item?");
            alert.setContentText("This cannot be undone.");
            Optional<ButtonType> answer = alert.showAndWait();
            result.setText(answer.filter(button -> button == ButtonType.OK).isPresent() ? "Deleted" : "Kept");
        });

        Button error = new Button("Error");
        error.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The file could not be saved.", ButtonType.CLOSE);
            alert.setHeaderText(null);
            alert.showAndWait();
            result.setText("Error acknowledged");
        });

        Button input = new Button("Text input");
        input.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog("World");
            dialog.setTitle("Greeting");
            dialog.setHeaderText("Who should be greeted?");
            dialog.setContentText("Name:");
            dialog.showAndWait().ifPresent(name -> result.setText("Hello, " + name));
        });

        Button choice = new Button("Choice");
        choice.setOnAction(event -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Medium", Arrays.asList("Small", "Medium", "Large"));
            dialog.setTitle("Size");
            dialog.setHeaderText("Pick a size");
            Optional<String> picked = dialog.showAndWait();
            result.setText(picked.map(size -> "Picked " + size).orElse("Nothing picked"));
        });

        Button window = new Button("Second window");
        window.setOnAction(event -> openWindow());

        Label target = new Label("Right click me");
        target.setStyle("-fx-border-color: gray; -fx-padding: 8;");
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> result.setText("Copy chosen"));
        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(event -> result.setText("Paste chosen"));
        target.setContextMenu(new ContextMenu(copy, paste));

        getChildren().addAll(new FlowPane(8, 8, confirm, error, input, choice, window), target, result);
    }

    private void openWindow() {
        Stage stage = new Stage();
        stage.initOwner(getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Window " + (++windows));

        Label label = new Label("This is another stage");
        Button close = new Button("Close");
        close.setOnAction(event -> stage.close());
        VBox root = new VBox(12, label, close);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));

        stage.setScene(new Scene(root, 280, 160));
        stage.setOnHidden(event -> result.setText("Window " + windows + " closed"));
        stage.show();
    }
}
