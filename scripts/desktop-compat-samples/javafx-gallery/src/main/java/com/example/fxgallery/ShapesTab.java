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

import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

/**
 * Scene graph shapes on the left, immediate mode drawing on a canvas on the right.
 */
public class ShapesTab extends VBox {

    private final Label hint = new Label("Drag on the canvas to draw, right click to clear, arrow keys move the square");
    private final Canvas canvas = new Canvas(380, 300);
    private final Rectangle player = new Rectangle(20, 20, Color.CRIMSON);

    public ShapesTab() {
        super(8);
        setPadding(new Insets(12));
        Button redraw = new Button("Redraw canvas");
        redraw.setOnAction(event -> paint());
        getChildren().addAll(new HBox(16, shapes(), canvas), redraw, hint);
        paint();
        installMouse();
    }

    private Pane shapes() {
        Pane pane = new Pane();
        pane.setPrefSize(380, 300);
        pane.setStyle("-fx-background-color: white; -fx-border-color: lightgray;");

        Rectangle rounded = new Rectangle(20, 20, 110, 60);
        rounded.setArcWidth(20);
        rounded.setArcHeight(20);
        rounded.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.DODGERBLUE), new Stop(1, Color.MIDNIGHTBLUE)));
        rounded.setStroke(Color.BLACK);
        rounded.setStrokeWidth(2);

        Circle sun = new Circle(200, 50, 32);
        sun.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.YELLOW), new Stop(1, Color.ORANGERED)));

        Ellipse ellipse = new Ellipse(310, 50, 45, 25);
        ellipse.setFill(Color.TRANSPARENT);
        ellipse.setStroke(Color.DARKGREEN);
        ellipse.getStrokeDashArray().addAll(6.0, 4.0);

        Line line = new Line(20, 110, 360, 110);
        line.setStrokeWidth(4);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.setStroke(Color.SLATEGRAY);

        Polygon triangle = new Polygon(40, 200, 90, 130, 140, 200);
        triangle.setFill(Color.web("#8e24aa", 0.8));

        Path path = new Path(
                new MoveTo(170, 200),
                new LineTo(190, 140),
                new QuadCurveTo(220, 100, 250, 140),
                new CubicCurveTo(260, 170, 290, 170, 300, 140),
                new LineTo(320, 200),
                new ClosePath());
        path.setFill(Color.LIGHTGREEN);
        path.setStroke(Color.DARKGREEN);
        path.setStrokeWidth(2);

        Arc pie = new Arc(60, 250, 35, 35, 30, 300);
        pie.setType(ArcType.ROUND);
        pie.setFill(Color.GOLD);

        Text rotated = new Text(140, 255, "Rotated text");
        rotated.setFont(Font.font("SansSerif", FontWeight.BOLD, 18));
        rotated.setFill(Color.DARKSLATEBLUE);
        rotated.getTransforms().add(new Rotate(-12, 140, 255));

        Circle clipped = new Circle(320, 250, 40, Color.TEAL);
        clipped.setClip(new Rectangle(280, 230, 80, 40));

        player.setX(250);
        player.setY(215);

        pane.getChildren().addAll(rounded, sun, ellipse, line, triangle, path, pie, rotated, clipped, player);
        pane.setFocusTraversable(true);
        pane.setOnMouseClicked(event -> pane.requestFocus());
        pane.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.LEFT) {
                player.setX(player.getX() - 5);
            } else if (code == KeyCode.RIGHT) {
                player.setX(player.getX() + 5);
            } else if (code == KeyCode.UP) {
                player.setY(player.getY() - 5);
            } else if (code == KeyCode.DOWN) {
                player.setY(player.getY() + 5);
            }
            event.consume();
        });
        return pane;
    }

    private void paint() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.LIGHTSKYBLUE), new Stop(1, Color.WHITE)));
        g.fillRect(0, 0, w, h);

        g.setStroke(Color.DARKGRAY);
        g.setLineWidth(1);
        for (int x = 0; x <= w; x += 40) {
            g.strokeLine(x, 0, x, h);
        }

        g.setFill(Color.FORESTGREEN);
        g.fillOval(30, 30, 70, 50);
        g.setStroke(Color.DARKRED);
        g.setLineWidth(3);
        g.strokeRoundRect(130, 30, 90, 50, 14, 14);
        g.strokeArc(250, 25, 80, 60, 45, 240, ArcType.OPEN);

        g.beginPath();
        g.moveTo(30, 180);
        g.bezierCurveTo(90, 100, 150, 260, 210, 180);
        g.quadraticCurveTo(250, 130, 290, 180);
        g.lineTo(340, 120);
        g.setStroke(Color.NAVY);
        g.stroke();

        g.save();
        g.translate(190, 250);
        g.rotate(-8);
        g.setFill(Color.BLACK);
        g.setFont(Font.font(20));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText("Canvas text", 0, 0);
        g.restore();

        g.setGlobalAlpha(0.5);
        g.setFill(Color.MAGENTA);
        g.fillPolygon(new double[] {300, 360, 330}, new double[] {220, 220, 280}, 3);
        g.setGlobalAlpha(1);
    }

    private void installMouse() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                paint();
            } else {
                g.setFill(Color.BLACK);
                g.fillOval(event.getX() - 2, event.getY() - 2, 4, 4);
            }
        });
        canvas.setOnMouseDragged(event -> {
            g.setFill(Color.BLACK);
            g.fillOval(event.getX() - 2, event.getY() - 2, 4, 4);
            hint.setText(String.format("Drawing at %.0f, %.0f", event.getX(), event.getY()));
        });
        canvas.setOnMouseReleased(event -> hint.setText("Stroke finished"));
    }
}
