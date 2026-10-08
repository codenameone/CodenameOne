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

import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * The gallery's main window: a menu bar, one tab per topic and a status line.
 */
public class GalleryApp extends Application {

    private final Label status = new Label("Ready");
    private TabPane tabs;

    @Override
    public void start(Stage stage) {
        tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Controls", new ControlsTab()),
                tab("Data", new DataTab()),
                tab("Layouts", new LayoutsTab()),
                tab("Bindings", new BindingsTab()),
                tab("FXML", new FxmlTab()),
                tab("Shapes", new ShapesTab()),
                tab("Animation", new AnimationTab()),
                tab("Dialogs", new DialogsTab()),
                tab("Tasks", new TasksTab()));
        tabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, before, now) -> status.setText("Showing " + now.getText()));

        status.setId("status");
        status.setPadding(new Insets(4, 8, 4, 8));

        BorderPane root = new BorderPane();
        root.setTop(createMenuBar(stage));
        root.setCenter(tabs);
        root.setBottom(status);

        Scene scene = new Scene(root, 920, 660);
        scene.getStylesheets().add(GalleryApp.class.getResource("css/gallery.css").toExternalForm());

        stage.setTitle("JavaFX Gallery");
        stage.getIcons().add(new Image(GalleryApp.class.getResourceAsStream("images/logo.png")));
        stage.setScene(scene);
        stage.show();
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuItem about = new MenuItem("About");
        about.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.setTitle("About");
            alert.setHeaderText("JavaFX Gallery");
            alert.setContentText("A tour of the JavaFX controls, layouts and APIs.");
            alert.showAndWait();
        });

        MenuItem exit = new MenuItem("Exit");
        exit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        exit.setOnAction(event -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Close the gallery?",
                    ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> answer = confirm.showAndWait();
            if (answer.isPresent() && answer.get() == ButtonType.YES) {
                Platform.exit();
            }
        });

        CheckMenuItem compact = new CheckMenuItem("Compact status");
        compact.selectedProperty().addListener((observable, was, is) ->
                status.setStyle(is ? "-fx-font-size: 10px;" : ""));

        Menu file = new Menu("File");
        file.getItems().addAll(about, new SeparatorMenuItem(), exit);

        Menu view = new Menu("View");
        view.getItems().add(compact);
        Menu go = new Menu("Go to");
        for (Tab tab : tabs.getTabs()) {
            MenuItem item = new MenuItem(tab.getText());
            item.setOnAction(event -> tabs.getSelectionModel().select(tab));
            go.getItems().add(item);
        }
        view.getItems().add(go);

        return new MenuBar(file, view);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
