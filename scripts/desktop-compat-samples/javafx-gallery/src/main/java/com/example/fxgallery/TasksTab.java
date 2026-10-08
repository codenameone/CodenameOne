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

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Work off the application thread: a {@link Task} bound to a progress bar, and
 * {@link Platform#runLater(Runnable)} from a plain thread.
 */
public class TasksTab extends VBox {

    private final ProgressBar progress = new ProgressBar(0);
    private final Label message = new Label("Idle");
    private final Label outcome = new Label();
    private final Label ticker = new Label("No ticks yet");
    private Task<Integer> running;

    public TasksTab() {
        super(10);
        setPadding(new Insets(12));
        progress.setPrefWidth(320);

        Button start = new Button("Start task");
        Button cancel = new Button("Cancel");
        cancel.setDisable(true);
        start.setOnAction(event -> {
            running = createTask();
            progress.progressProperty().bind(running.progressProperty());
            message.textProperty().bind(running.messageProperty());
            start.disableProperty().bind(running.runningProperty());
            cancel.disableProperty().bind(running.runningProperty().not());
            running.setOnSucceeded(done -> outcome.setText("Sum: " + running.getValue()));
            running.setOnCancelled(done -> outcome.setText("Cancelled"));
            running.setOnFailed(done -> outcome.setText("Failed: " + running.getException()));
            Thread thread = new Thread(running, "gallery-task");
            thread.setDaemon(true);
            thread.start();
        });
        cancel.setOnAction(event -> {
            if (running != null) {
                running.cancel();
            }
        });

        Button tick = new Button("Tick from a thread");
        tick.setOnAction(event -> {
            Thread thread = new Thread(() -> {
                for (int i = 1; i <= 3; i++) {
                    int count = i;
                    Platform.runLater(() -> ticker.setText("Tick " + count));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        });

        getChildren().addAll(new HBox(8, start, cancel), progress, message, outcome, new HBox(8, tick, ticker));
    }

    private static Task<Integer> createTask() {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                int sum = 0;
                int steps = 40;
                for (int i = 1; i <= steps; i++) {
                    if (isCancelled()) {
                        updateMessage("Stopping");
                        break;
                    }
                    sum += i;
                    updateProgress(i, steps);
                    updateMessage("Step " + i + " of " + steps);
                    Thread.sleep(50);
                }
                return sum;
            }
        };
    }
}
