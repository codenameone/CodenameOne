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
package com.codenameone.examples.hellocodenameone.desktop;

import com.codename1.fxcompat.FxInterop;

import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

/// The JavaFX screens of the desktop compatibility screenshot tests.
///
/// This class is ordinary JavaFX; the build relocates it onto the JavaFX
/// layer and compiles the FXML document and the style sheet beside it.
/// Each scene is built here and handed to the application as a Codename One
/// component, which is all the tests in `tests/desktopcompat` ever see of
/// JavaFX.
///
/// Everything a scene shows is fixed: no clock, no animation, no focused
/// text field.
public final class FxScenes {

    private static final String[] NAMES = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn"};
    private static final int[] MOONS = {0, 0, 1, 2, 95, 146};

    private static Alert open;

    private FxScenes() {
    }

    private static com.codename1.ui.Component host(Parent content) {
        StackPane root = new StackPane(content);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: white;");
        return FxInterop.asComponent(root);
    }

    /// A text control that takes no focus on its own: a caret blinks.
    private static <T extends Node> T still(T node) {
        node.setFocusTraversable(false);
        return node;
    }

    /// Scene (h): the basic controls in a `GridPane` inside a `VBox`.
    public static com.codename1.ui.Component controls() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Name:"), 0, 0);
        TextField name = still(new TextField("Ada Lovelace"));
        grid.add(name, 1, 0);
        GridPane.setHgrow(name, Priority.ALWAYS);
        grid.add(new Label("Password:"), 0, 1);
        PasswordField password = still(new PasswordField());
        password.setText("secret");
        grid.add(password, 1, 1);
        grid.add(new Label("Planet:"), 0, 2);
        ComboBox<String> planet = new ComboBox<String>(FXCollections.observableArrayList(NAMES));
        planet.setValue("Earth");
        grid.add(planet, 1, 2);
        grid.add(new Label("Volume:"), 0, 3);
        grid.add(new Slider(0, 100, 35), 1, 3);
        grid.add(new Label("Progress:"), 0, 4);
        ProgressBar progress = new ProgressBar(0.6);
        progress.setMaxWidth(Double.MAX_VALUE);
        grid.add(progress, 1, 4);

        CheckBox remember = new CheckBox("Remember me");
        remember.setSelected(true);
        CheckBox offline = new CheckBox("Work offline");
        HBox checks = new HBox(12, remember, offline);

        ToggleGroup sizes = new ToggleGroup();
        RadioButton small = new RadioButton("Small");
        RadioButton medium = new RadioButton("Medium");
        RadioButton large = new RadioButton("Large");
        small.setToggleGroup(sizes);
        medium.setToggleGroup(sizes);
        large.setToggleGroup(sizes);
        medium.setSelected(true);
        HBox radios = new HBox(12, small, medium, large);

        Button disabled = new Button("Disabled");
        disabled.setDisable(true);
        HBox buttons = new HBox(8, disabled, new Button("Cancel"), new Button("OK"));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(12, grid, checks, radios, buttons);
        box.setAlignment(Pos.TOP_LEFT);
        return host(box);
    }

    /// A row of the table of scene (i).
    public static final class Planet {
        private final String name;
        private final int moons;

        Planet(String name, int moons) {
            this.name = name;
            this.moons = moons;
        }

        /// The planet's name.
        public String getName() {
            return name;
        }

        /// How many moons it has.
        public int getMoons() {
            return moons;
        }
    }

    /// Scene (i): a `TableView` over a `ListView` with a cell factory.
    public static com.codename1.ui.Component data() {
        ObservableList<Planet> planets = FXCollections.observableArrayList();
        for (int i = 0; i < NAMES.length; i++) {
            planets.add(new Planet(NAMES[i], MOONS[i]));
        }
        TableView<Planet> table = new TableView<Planet>(planets);
        TableColumn<Planet, String> name = new TableColumn<Planet, String>("Planet");
        name.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<Planet, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(TableColumn.CellDataFeatures<Planet, String> row) {
                        return new ReadOnlyObjectWrapper<String>(row.getValue().getName());
                    }
                });
        name.setPrefWidth(160);
        TableColumn<Planet, Number> moons = new TableColumn<Planet, Number>("Moons");
        moons.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<Planet, Number>, ObservableValue<Number>>() {
                    @Override
                    public ObservableValue<Number> call(TableColumn.CellDataFeatures<Planet, Number> row) {
                        return new ReadOnlyObjectWrapper<Number>(Integer.valueOf(row.getValue().getMoons()));
                    }
                });
        moons.setPrefWidth(90);
        table.getColumns().add(name);
        table.getColumns().add(moons);
        table.setPrefHeight(200);

        ListView<Planet> list = new ListView<Planet>(planets);
        list.setCellFactory(new Callback<ListView<Planet>, ListCell<Planet>>() {
            @Override
            public ListCell<Planet> call(ListView<Planet> view) {
                return new ListCell<Planet>() {
                    @Override
                    protected void updateItem(Planet item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item.getName() + "  -  " + item.getMoons()
                                    + (item.getMoons() == 1 ? " moon" : " moons"));
                            setStyle(item.getMoons() > 10 ? "-fx-font-weight: bold; -fx-text-fill: #8a3b00;"
                                    : "-fx-text-fill: black;");
                        }
                    }
                };
            }
        });
        list.setPrefHeight(200);

        VBox box = new VBox(8, new Label("TableView"), table, new Label("ListView, cell factory"), list);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(list, Priority.ALWAYS);
        return host(box);
    }

    /// Scene (j): shape nodes over a `Canvas`, with gradients and
    /// transforms in both.
    public static com.codename1.ui.Component shapes() {
        Pane nodes = new Pane();
        nodes.setPrefSize(320, 150);
        Rectangle rect = new Rectangle(10, 10, 90, 60);
        rect.setArcWidth(18);
        rect.setArcHeight(18);
        rect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#11998e")), new Stop(1, Color.web("#38ef7d"))));
        Circle circle = new Circle(155, 40, 30);
        circle.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, Color.web("#c06c84"))));
        circle.setStroke(Color.web("#6c5b7b"));
        circle.setStrokeWidth(3);
        Ellipse ellipse = new Ellipse(250, 40, 50, 26);
        ellipse.setFill(Color.web("#f8b195"));
        ellipse.setRotate(-20);
        Polygon triangle = new Polygon(55, 85, 100, 140, 10, 140);
        triangle.setFill(Color.web("#f67280"));
        Line dashed = new Line(120, 110, 310, 110);
        dashed.setStroke(Color.DARKGRAY);
        dashed.setStrokeWidth(2);
        dashed.getStrokeDashArray().addAll(Double.valueOf(8), Double.valueOf(6));
        Rectangle turned = new Rectangle(200, 120, 60, 24);
        turned.setFill(Color.web("#355c7d"));
        turned.setRotate(15);
        nodes.getChildren().addAll(rect, circle, ellipse, triangle, dashed, turned);

        Canvas canvas = new Canvas(320, 150);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fdfbfb")), new Stop(1, Color.web("#c9d6e6"))));
        g.fillRect(0, 0, 320, 150);
        g.setFill(Color.web("#6c5b7b"));
        g.fillOval(12, 12, 80, 50);
        g.setStroke(Color.web("#355c7d"));
        g.setLineWidth(3);
        g.strokeRoundRect(110, 12, 80, 50, 14, 14);
        g.save();
        g.translate(250, 40);
        g.rotate(30);
        g.setFill(Color.web("#f67280"));
        g.fillRect(-30, -18, 60, 36);
        g.restore();
        g.setLineWidth(2);
        g.setLineDashes(8, 6);
        g.strokeLine(12, 84, 308, 84);
        g.setLineDashes();
        g.setFill(Color.web("#355c7d"));
        g.fillText("Canvas", 12, 120);
        g.save();
        g.scale(1.5, 1.5);
        g.fillRect(100, 66, 30, 14);
        g.restore();

        VBox box = new VBox(8, new Label("Shapes"), nodes, new Label("Canvas"), canvas);
        return host(box);
    }

    /// Scene (k): a screen declared in FXML, with a controller and a
    /// style sheet whose colours are looked up.
    public static com.codename1.ui.Component fxml() {
        Parent root;
        try {
            root = FXMLLoader.load(FxScenes.class.getResource("account.fxml"));
        } catch (java.io.IOException failed) {
            throw new IllegalStateException("account.fxml did not load: " + failed.getMessage());
        }
        Scene scene = new Scene(root);
        scene.getStylesheets().add(FxScenes.class.getResource("account.css").toExternalForm());
        return FxInterop.asComponent(scene);
    }

    private static Label tile(String text, String color) {
        Label tile = new Label(text);
        tile.setPrefSize(84, 40);
        tile.setAlignment(Pos.CENTER);
        tile.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 6;");
        return tile;
    }

    /// Scene (l): a `BorderPane` holding a `StackPane` and a `FlowPane`.
    public static com.codename1.ui.Component panes() {
        Label title = new Label("BorderPane top");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setPadding(new Insets(8));
        title.setStyle("-fx-background-color: #355c7d; -fx-text-fill: white; -fx-font-weight: bold;");

        Rectangle back = new Rectangle(200, 110, Color.web("#f8b195"));
        back.setArcWidth(16);
        back.setArcHeight(16);
        Circle middle = new Circle(40, Color.web("#c06c84"));
        Label front = new Label("StackPane");
        front.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        StackPane stack = new StackPane(back, middle, front);
        stack.setPadding(new Insets(12));

        FlowPane flow = new FlowPane(8, 8);
        flow.setPadding(new Insets(8));
        String[] colors = {"#6c5b7b", "#c06c84", "#f67280", "#355c7d", "#11998e", "#8a3b00", "#2f6fed"};
        for (int i = 0; i < colors.length; i++) {
            flow.getChildren().add(tile("Tile " + (i + 1), colors[i]));
        }

        Label left = new Label("Left");
        left.setPadding(new Insets(8));
        left.setStyle("-fx-background-color: #f2f6fa;");
        left.setMaxHeight(Double.MAX_VALUE);
        Label status = new Label("BorderPane bottom");
        status.setPadding(new Insets(6));

        BorderPane border = new BorderPane();
        border.setTop(title);
        border.setLeft(left);
        border.setCenter(new VBox(stack, flow));
        border.setBottom(status);
        return host(border);
    }

    /// Scene (m): asks for a confirmation in an `Alert` and answers the
    /// name of the button that was chosen. The call blocks, as on a desktop,
    /// until [#answerConfirm()] or the user answered.
    public static String confirm() {
        FxInterop.setNativeWindows(false);
        try {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Delete the selected planet?");
            alert.setHeaderText("Confirm");
            open = alert;
            Optional<ButtonType> answer = alert.showAndWait();
            if (!answer.isPresent()) {
                return "closed";
            }
            return answer.get() == ButtonType.OK ? "ok" : answer.get() == ButtonType.CANCEL ? "cancel" : "other";
        } finally {
            open = null;
            FxInterop.setNativeWindows(true);
        }
    }

    /// Whether the dialog of [#confirm()] is up.
    public static boolean confirmShowing() {
        return open != null && open.isShowing();
    }

    /// Presses "OK" in the dialog of [#confirm()]; false if it is not up.
    public static boolean answerConfirm() {
        Alert alert = open;
        if (alert == null || !alert.isShowing()) {
            return false;
        }
        Node ok = alert.getDialogPane().lookupButton(ButtonType.OK);
        if (!(ok instanceof Button)) {
            return false;
        }
        ((Button) ok).fire();
        return true;
    }
}
