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

// The benchmark gauge in JavaFX: a Canvas redrawn on every pulse with Prism's
// software pipeline on a display-less glass platform -- the JavaFX path on a
// board without a GPU. Prints the same CN1BENCH lines as ../common.h.
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class Gauge extends Application {
    private static final int W = 800;
    private static final int H = 480;
    private double angle;
    private long frames;
    private boolean first = true;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext g = canvas.getGraphicsContext2D();
        Font font = Font.font("DejaVu Sans", FontWeight.BOLD, 28);
        stage.setScene(new Scene(new Pane(canvas), W, H));
        stage.show();
        long start = System.currentTimeMillis();
        long cpu0 = cpuTicks();
        long end = start + seconds() * 1000L;
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                angle += 0.04;
                draw(g, font);
                frames++;
                if (first) {
                    first = false;
                    // The first pulse renders the frame drawn here.
                    Platform.runLater(() -> System.out.println("CN1BENCH first_frame_epoch_ms=" + System.currentTimeMillis()));
                }
                if (System.currentTimeMillis() >= end) {
                    stop();
                    long elapsed = Math.max(1, System.currentTimeMillis() - start);
                    System.out.println("CN1BENCH fps=" + frames * 1000 / elapsed + " frames=" + frames
                            + " rss_kb=0 cpu_pct=" + (cpuTicks() - cpu0) * 1000 / elapsed);
                    Platform.exit();
                }
            }
        }.start();
    }

    private void draw(GraphicsContext g, Font font) {
        int cx = W / 2, cy = H / 2, r = Math.min(W, H) / 2 - 10;
        int sweep = (int) ((Math.sin(angle) + 1) * 135);
        g.setFill(Color.rgb(0x10, 0x18, 0x20));
        g.fillRect(0, 0, W, H);
        g.setFill(Color.rgb(0x2a, 0x3a, 0x4a));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setFill(Color.rgb(0x33, 0xaa, 0xff));
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 225 - sweep, sweep, ArcType.ROUND);
        g.setFill(Color.rgb(0x10, 0x18, 0x20));
        g.fillOval(cx - r + 24, cy - r + 24, r * 2 - 48, r * 2 - 48);
        double a = Math.toRadians(225 - sweep);
        g.setStroke(Color.WHITE);
        g.setLineWidth(1);
        g.strokeLine(cx, cy, cx + (int) (Math.cos(a) * (r - 30)), cy - (int) (Math.sin(a) * (r - 30)));
        g.setFill(Color.WHITE);
        g.setFont(font);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        g.fillText((sweep * 100 / 270) + " %", cx, cy + r / 3 - 28);
    }

    private static int seconds() {
        String s = System.getenv("CN1_BENCH_SECONDS");
        return s == null ? 10 : Integer.parseInt(s);
    }

    // utime + stime from /proc/self/stat, in clock ticks (100 per second).
    private static long cpuTicks() {
        try {
            String stat = new String(Files.readAllBytes(Paths.get("/proc/self/stat")));
            String[] f = stat.substring(stat.lastIndexOf(')') + 2).split(" ");
            return Long.parseLong(f[11]) + Long.parseLong(f[12]);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
