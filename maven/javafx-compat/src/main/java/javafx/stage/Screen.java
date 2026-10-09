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
package javafx.stage;

import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Display;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;

/// The display the application is on.
///
/// Codename One has one: the screen of the device, or the window of the
/// application on a desktop. Its bounds are that display in logical
/// pixels, read each time they are asked for, so they follow a rotation
/// of the device. The visual bounds are the same rectangle, because the
/// status bar and the keyboard are outside what Codename One reports.
///
/// Before the display exists, as in a plain unit test, both are empty.
public final class Screen {

    private static final Screen PRIMARY = new Screen();

    private Screen() {
    }

    /// Returns the one screen.
    public static Screen getPrimary() {
        return PRIMARY;
    }

    /// Returns the screens, which is the one screen.
    public static ObservableList<Screen> getScreens() {
        ObservableList<Screen> list = FXCollections.observableArrayList();
        list.add(PRIMARY);
        return list;
    }

    /// Returns the screens that a rectangle touches, which is the one
    /// screen when it touches it.
    public static ObservableList<Screen> getScreensForRectangle(double x, double y, double width, double height) {
        ObservableList<Screen> list = FXCollections.observableArrayList();
        if (PRIMARY.getBounds().intersects(x, y, width, height)) {
            list.add(PRIMARY);
        }
        return list;
    }

    /// Returns the screens that a rectangle touches.
    public static ObservableList<Screen> getScreensForRectangle(Rectangle2D r) {
        return getScreensForRectangle(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    /// Returns the whole of the screen in logical pixels.
    public final Rectangle2D getBounds() {
        if (!Display.isInitialized()) {
            return Rectangle2D.EMPTY;
        }
        Display d = Display.getInstance();
        return new Rectangle2D(0, 0, Units.toLogical(d.getDisplayWidth()), Units.toLogical(d.getDisplayHeight()));
    }

    /// Returns the part of the screen an application can use, which is
    /// all of what [#getBounds()] reports.
    public final Rectangle2D getVisualBounds() {
        return getBounds();
    }

    /// Returns the number of logical pixels to an inch, which is 96 by
    /// the definition of a logical pixel here.
    public final double getDpi() {
        return 96;
    }

    /// Returns the number of device pixels in a logical pixel, across.
    public final double getOutputScaleX() {
        return Units.scale();
    }

    /// Returns the number of device pixels in a logical pixel, down.
    public final double getOutputScaleY() {
        return Units.scale();
    }

    @Override
    public String toString() {
        return "Screen[bounds = " + getBounds() + "]";
    }
}
