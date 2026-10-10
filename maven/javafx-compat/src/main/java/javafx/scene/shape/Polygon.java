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
package javafx.scene.shape;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxPath;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/// A closed outline through a list of points, x and y alternating. A
/// trailing x without its y is ignored.
public class Polygon extends Shape {

    private final ObservableList<Double> points = FXCollections.observableArrayList();

    /// Creates a polygon without points.
    public Polygon() {
        listen();
    }

    /// Creates a polygon through points, x and y alternating.
    public Polygon(double... points) {
        if (points != null) {
            for (int i = 0; i < points.length; i++) {
                this.points.add(Double.valueOf(points[i]));
            }
        }
        listen();
    }

    private void listen() {
        points.addListener(new ListChangeListener<Double>() {
            @Override
            public void onChanged(Change<? extends Double> change) {
                cn1Invalidated(Dirty.GEOMETRY);
            }
        });
    }

    /// Returns the points, x and y alternating.
    public final ObservableList<Double> getPoints() {
        return points;
    }

    static FxPath through(ObservableList<Double> points, boolean close) {
        FxPath p = new FxPath();
        int n = points.size() & ~1;
        for (int i = 0; i < n; i += 2) {
            Double x = points.get(i);
            Double y = points.get(i + 1);
            if (x == null || y == null) {
                continue;
            }
            if (p.isEmpty()) {
                p.moveTo(x.doubleValue(), y.doubleValue());
            } else {
                p.lineTo(x.doubleValue(), y.doubleValue());
            }
        }
        if (close) {
            p.closePath();
        }
        return p;
    }

    @Override
    protected FxPath cn1CreatePath() {
        return through(points, true);
    }
}
