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

/// An open outline through a list of points, x and y alternating. It has
/// a black stroke and no fill to begin with; a fill, when set, covers the
/// outline as if it were closed.
public class Polyline extends Shape {

    private final ObservableList<Double> points = FXCollections.observableArrayList();

    /// Creates a polyline without points.
    public Polyline() {
        super(true);
        listen();
    }

    /// Creates a polyline through points, x and y alternating.
    public Polyline(double... points) {
        super(true);
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

    @Override
    protected FxPath cn1CreatePath() {
        return Polygon.through(points, false);
    }
}
