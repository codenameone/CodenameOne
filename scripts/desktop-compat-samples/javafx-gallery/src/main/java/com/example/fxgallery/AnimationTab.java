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

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Timelines, transitions and a frame timer.
 */
public class AnimationTab extends VBox {

    private final Rectangle bar = new Rectangle(20, 20, 40, 24);
    private final Circle ball = new Circle(40, 100, 16, Color.TOMATO);
    private final Rectangle box = new Rectangle(20, 150, 40, 40);
    private final Label frames = new Label("0 frames");
    private final Label clock = new Label("0 s");

    private final Timeline timeline;
    private final Animation transitions;
    private final AnimationTimer timer;
    private long frameCount;
    private int seconds;

    public AnimationTab() {
        super(10);
        setPadding(new Insets(12));

        bar.setFill(Color.ROYALBLUE);
        box.setFill(Color.MEDIUMSEAGREEN);
        Pane stage = new Pane(bar, ball, box);
        stage.setPrefSize(420, 210);
        stage.setStyle("-fx-background-color: white; -fx-border-color: lightgray;");

        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.widthProperty(), 40)),
                new KeyFrame(Duration.millis(900), new KeyValue(bar.widthProperty(), 360, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(1), event -> clock.setText(++seconds + " s")));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.setAutoReverse(true);

        TranslateTransition slide = new TranslateTransition(Duration.millis(800), ball);
        slide.setByX(320);
        slide.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(800), ball);
        fade.setFromValue(1.0);
        fade.setToValue(0.2);
        RotateTransition spin = new RotateTransition(Duration.millis(600), box);
        spin.setByAngle(180);
        TranslateTransition back = new TranslateTransition(Duration.millis(400), ball);
        back.setToX(0);
        FadeTransition appear = new FadeTransition(Duration.millis(400), ball);
        appear.setToValue(1.0);
        transitions = new SequentialTransition(new ParallelTransition(slide, fade), spin,
                new ParallelTransition(back, appear));
        transitions.setOnFinished(event -> frames.setText(frameCount + " frames, transitions finished"));

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                frameCount++;
                if (frameCount % 30 == 0) {
                    frames.setText(frameCount + " frames");
                }
            }
        };

        Button play = new Button("Play");
        play.setOnAction(event -> start());
        Button pause = new Button("Pause");
        pause.setOnAction(event -> {
            timeline.pause();
            transitions.pause();
        });
        Button stop = new Button("Stop");
        stop.setOnAction(event -> stop());

        getChildren().addAll(stage, new HBox(8, play, pause, stop), new HBox(16, clock, frames));
    }

    private void start() {
        timeline.play();
        transitions.play();
        timer.start();
    }

    private void stop() {
        timeline.stop();
        transitions.stop();
        timer.stop();
        bar.setWidth(40);
        ball.setTranslateX(0);
        ball.setOpacity(1);
        box.setRotate(0);
    }
}
