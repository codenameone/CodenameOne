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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * One example of each layout pane.
 */
public class LayoutsTab extends ScrollPane {

    public LayoutsTab() {
        VBox content = new VBox(14,
                titled("HBox and VBox with grow priorities", boxes()),
                titled("GridPane form", form()),
                titled("BorderPane", border()),
                titled("StackPane", stack()),
                titled("FlowPane", flow()),
                titled("AnchorPane", anchor()),
                titled("SplitPane", split()));
        content.setPadding(new Insets(12));
        setContent(content);
        setFitToWidth(true);
    }

    private static VBox titled(String title, Node body) {
        Label label = new Label(title);
        label.getStyleClass().add("section");
        VBox box = new VBox(4, label, body);
        box.getStyleClass().add("card");
        return box;
    }

    private static Label cell(String text, String colour) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: " + colour + "; -fx-padding: 6 10 6 10;");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Node boxes() {
        Label fixed = cell("fixed", "#ffe082");
        Label grows = cell("grows", "#a5d6a7");
        Region spacer = new Region();
        Button end = new Button("End");
        HBox.setHgrow(grows, Priority.ALWAYS);
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        HBox row = new HBox(6, fixed, grows, spacer, end);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox column = new VBox(4, cell("first", "#90caf9"), cell("second", "#ce93d8"));
        column.setFillWidth(true);
        return new VBox(6, row, column);
    }

    private Node form() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.RIGHT);
        labels.setMinWidth(80);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);

        grid.add(new Label("First name"), 0, 0);
        grid.add(new TextField("Ada"), 1, 0);
        grid.add(new Label("Last name"), 0, 1);
        grid.add(new TextField("Lovelace"), 1, 1);
        Button save = new Button("Save");
        GridPane.setHalignment(save, HPos.RIGHT);
        grid.add(save, 0, 2, 2, 1);
        return grid;
    }

    private Node border() {
        BorderPane pane = new BorderPane();
        pane.setTop(cell("top", "#ffcc80"));
        pane.setLeft(cell("left", "#80cbc4"));
        pane.setCenter(cell("center", "#eeeeee"));
        pane.setRight(cell("right", "#80cbc4"));
        pane.setBottom(cell("bottom", "#ffcc80"));
        pane.setPrefHeight(110);
        return pane;
    }

    private Node stack() {
        Rectangle back = new Rectangle(160, 60, Color.STEELBLUE);
        back.setArcWidth(16);
        back.setArcHeight(16);
        Label front = new Label("On top");
        front.setTextFill(Color.WHITE);
        StackPane stack = new StackPane(back, front);
        StackPane.setAlignment(front, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(front, new Insets(0, 8, 4, 0));
        stack.setMaxWidth(160);
        return stack;
    }

    private Node flow() {
        FlowPane flow = new FlowPane(6, 6);
        for (int i = 1; i <= 12; i++) {
            flow.getChildren().add(new Button("Item " + i));
        }
        flow.setPrefWrapLength(360);
        return flow;
    }

    private Node anchor() {
        Button topLeft = new Button("Top left");
        Button bottomRight = new Button("Bottom right");
        Label stretched = cell("left and right anchored", "#ef9a9a");
        AnchorPane.setTopAnchor(topLeft, 4.0);
        AnchorPane.setLeftAnchor(topLeft, 4.0);
        AnchorPane.setBottomAnchor(bottomRight, 4.0);
        AnchorPane.setRightAnchor(bottomRight, 4.0);
        AnchorPane.setLeftAnchor(stretched, 100.0);
        AnchorPane.setRightAnchor(stretched, 100.0);
        AnchorPane.setTopAnchor(stretched, 40.0);
        AnchorPane pane = new AnchorPane(topLeft, bottomRight, stretched);
        pane.setPrefHeight(110);
        return pane;
    }

    private Node split() {
        SplitPane inner = new SplitPane(cell("upper", "#fff59d"), cell("lower", "#b39ddb"));
        inner.setOrientation(Orientation.VERTICAL);
        SplitPane split = new SplitPane(cell("navigation", "#c5e1a5"), inner);
        split.setDividerPositions(0.35);
        split.setPrefHeight(120);
        return split;
    }
}
