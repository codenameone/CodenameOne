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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Properties, listeners and the bindings between them.
 */
public class BindingsTab extends VBox {

    private final IntegerProperty clicks = new SimpleIntegerProperty(this, "clicks", 0);
    private final ObservableList<String> entries = FXCollections.observableArrayList();
    private final TextArea history = new TextArea();

    public BindingsTab() {
        super(10);
        setPadding(new Insets(12));
        getChildren().addAll(mirror(), greeting(), conditional(), arithmetic(), counter(), list(), history);
        history.setEditable(false);
        history.setPrefRowCount(5);
    }

    private HBox mirror() {
        TextField left = new TextField("type here");
        TextField right = new TextField();
        right.textProperty().bindBidirectional(left.textProperty());
        return new HBox(8, new Label("Bidirectional:"), left, right);
    }

    private HBox greeting() {
        TextField first = new TextField("Ada");
        TextField last = new TextField("Lovelace");
        StringBinding full = Bindings.createStringBinding(
                () -> "Hello, " + first.getText().trim() + " " + last.getText().trim() + "!",
                first.textProperty(), last.textProperty());
        Label label = new Label();
        label.textProperty().bind(full);

        Button clear = new Button("Clear");
        clear.disableProperty().bind(first.textProperty().isEmpty().and(last.textProperty().isEmpty()));
        clear.setOnAction(event -> {
            first.clear();
            last.clear();
        });
        return new HBox(8, first, last, clear, label);
    }

    private HBox conditional() {
        CheckBox enabled = new CheckBox("Enabled");
        Label state = new Label();
        state.textProperty().bind(Bindings.when(enabled.selectedProperty()).then("Switched on").otherwise("Switched off"));
        state.styleProperty().bind(Bindings.when(enabled.selectedProperty())
                .then("-fx-text-fill: green;").otherwise("-fx-text-fill: gray;"));
        return new HBox(8, enabled, state);
    }

    private HBox arithmetic() {
        Slider width = new Slider(1, 20, 4);
        Slider height = new Slider(1, 20, 5);
        NumberBinding area = width.valueProperty().multiply(height.valueProperty());
        Label label = new Label();
        label.textProperty().bind(Bindings.format("%.1f x %.1f = %.1f", width.valueProperty(),
                height.valueProperty(), area));
        Label big = new Label("large");
        big.visibleProperty().bind(area.greaterThan(100));
        return new HBox(8, width, height, label, big);
    }

    private HBox counter() {
        Button button = new Button("Count");
        button.setOnAction(event -> clicks.set(clicks.get() + 1));
        Label label = new Label();
        label.textProperty().bind(clicks.asString("Clicked %d times"));

        ChangeListener<Number> listener = (observable, before, now) ->
                entries.add("clicks: " + before + " -> " + now);
        clicks.addListener(listener);

        Button reset = new Button("Reset");
        reset.setOnAction(event -> {
            clicks.removeListener(listener);
            clicks.set(0);
            clicks.addListener(listener);
            entries.clear();
        });
        return new HBox(8, button, reset, label);
    }

    private HBox list() {
        TextField entry = new TextField();
        entry.setPromptText("Add an entry");
        Button add = new Button("Add");
        add.disableProperty().bind(entry.textProperty().isEmpty());
        add.setOnAction(event -> {
            entries.add(entry.getText());
            entry.clear();
        });
        entries.addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                for (String added : change.getAddedSubList()) {
                    history.appendText("+ " + added + "\n");
                }
                if (change.wasRemoved()) {
                    history.appendText("- " + change.getRemovedSize() + " removed\n");
                }
            }
        });
        Label size = new Label();
        size.textProperty().bind(Bindings.size(entries).asString("%d entries"));
        return new HBox(8, entry, add, size);
    }
}
