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

import java.time.LocalDate;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The standard controls, created and wired in code.
 */
public class ControlsTab extends ScrollPane {

    private final Label echo = new Label("Nothing happened yet");

    public ControlsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(12));
        content.getChildren().addAll(
                header(),
                buttons(),
                new Separator(),
                choices(),
                new Separator(),
                textInputs(),
                new Separator(),
                selectors(),
                new Separator(),
                ranges(),
                echo);
        echo.getStyleClass().add("echo");
        setContent(content);
        setFitToWidth(true);
    }

    private HBox header() {
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("images/logo.png")));
        logo.setFitHeight(32);
        logo.setPreserveRatio(true);
        Label title = new Label("Controls");
        title.getStyleClass().add("title");
        HBox box = new HBox(8, logo, title);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox buttons() {
        Button plain = new Button("Click me");
        plain.setTooltip(new Tooltip("A plain button"));
        plain.setOnAction(event -> echo.setText("Button clicked"));

        Button primary = new Button("Primary");
        primary.setId("primary-button");
        primary.getStyleClass().add("primary");
        primary.setDefaultButton(true);
        primary.setOnAction(event -> echo.setText("Primary clicked"));

        Button styled = new Button("Inline style");
        styled.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 12;");

        Button disabled = new Button("Disabled");
        disabled.setDisable(true);

        ToggleButton toggle = new ToggleButton("Toggle");
        toggle.selectedProperty().addListener((observable, was, is) ->
                echo.setText(is ? "Toggle is on" : "Toggle is off"));

        Hyperlink link = new Hyperlink("A hyperlink");
        link.setOnAction(event -> echo.setText("Link followed"));

        return new HBox(8, plain, primary, styled, disabled, toggle, link);
    }

    private VBox choices() {
        CheckBox news = new CheckBox("Send me the newsletter");
        news.setSelected(true);
        news.setOnAction(event -> echo.setText("Newsletter: " + news.isSelected()));

        CheckBox terms = new CheckBox("Three states");
        terms.setAllowIndeterminate(true);
        terms.setIndeterminate(true);

        ToggleGroup size = new ToggleGroup();
        RadioButton small = new RadioButton("Small");
        RadioButton medium = new RadioButton("Medium");
        RadioButton large = new RadioButton("Large");
        small.setToggleGroup(size);
        medium.setToggleGroup(size);
        large.setToggleGroup(size);
        medium.setSelected(true);
        size.selectedToggleProperty().addListener((observable, before, now) -> {
            if (now instanceof RadioButton) {
                echo.setText("Size: " + ((RadioButton) now).getText());
            }
        });

        return new VBox(6, news, terms, new HBox(12, small, medium, large));
    }

    private VBox textInputs() {
        TextField name = new TextField();
        name.setPromptText("Your name");
        name.setPrefColumnCount(20);
        name.setOnAction(event -> echo.setText("Hello, " + name.getText()));

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        TextArea notes = new TextArea("Several lines\nof text");
        notes.setPrefRowCount(3);
        notes.setWrapText(true);

        Label length = new Label();
        length.textProperty().bind(notes.textProperty().length().asString("%d characters"));

        return new VBox(6, name, password, notes, length);
    }

    private HBox selectors() {
        ComboBox<String> fruit = new ComboBox<>(FXCollections.observableArrayList("Apple", "Banana", "Cherry"));
        fruit.setPromptText("Fruit");
        fruit.setOnAction(event -> echo.setText("Fruit: " + fruit.getValue()));

        ComboBox<String> editable = new ComboBox<>();
        editable.getItems().addAll("Red", "Green", "Blue");
        editable.setEditable(true);
        editable.setValue("Green");

        ChoiceBox<String> size = new ChoiceBox<>();
        size.getItems().addAll("S", "M", "L", "XL");
        size.getSelectionModel().select(1);
        size.getSelectionModel().selectedItemProperty().addListener(
                (observable, before, now) -> echo.setText("Choice: " + now));

        DatePicker date = new DatePicker(LocalDate.of(2024, 2, 29));
        date.setOnAction(event -> echo.setText("Date: " + date.getValue()));

        return new HBox(8, fruit, editable, size, date);
    }

    private VBox ranges() {
        Slider slider = new Slider(0, 100, 40);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(25);
        slider.setBlockIncrement(5);

        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(240);
        bar.progressProperty().bind(slider.valueProperty().divide(100));

        ProgressIndicator indicator = new ProgressIndicator();
        indicator.progressProperty().bind(bar.progressProperty());

        ProgressIndicator busy = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        busy.setPrefSize(28, 28);

        Label value = new Label();
        value.textProperty().bind(slider.valueProperty().asString("%.0f"));

        HBox row = new HBox(10, bar, indicator, busy, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, slider, row);
    }
}
