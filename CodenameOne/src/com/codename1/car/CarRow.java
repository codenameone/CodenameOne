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
package com.codename1.car;

import com.codename1.ui.Image;

/// A single selectable row in a `CarListTemplate`. Maps to `androidx.car.app.model.Row` and to a
/// `CPListItem` on CarPlay. A row has a primary title, an optional secondary text line, an optional
/// leading image and an activation listener.
///
/// ```java
/// CarRow track = new CarRow("Take Five")
///         .setText("Dave Brubeck")
///         .setImage(albumArt)
///         .setOnAction(ctx -> play(trackId));
/// ```
public class CarRow {
    private String title;
    private String text;
    private Image image;
    private boolean browsable;
    private CarActionListener listener;

    /// Creates an empty row.
    public CarRow() {
    }

    /// Creates a row with the supplied title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the primary line
    public CarRow(String title) {
        this.title = title;
    }

    /// Returns the primary title.
    public String getTitle() {
        return title;
    }

    /// Sets the primary title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the primary line
    ///
    /// #### Returns
    ///
    /// this row, for chaining
    public CarRow setTitle(String title) {
        this.title = title;
        return this;
    }

    /// Returns the secondary text line, or null.
    public String getText() {
        return text;
    }

    /// Sets the secondary text line shown under the title.
    ///
    /// #### Parameters
    ///
    /// - `text`: the secondary line
    ///
    /// #### Returns
    ///
    /// this row, for chaining
    public CarRow setText(String text) {
        this.text = text;
        return this;
    }

    /// Returns the leading image, or null.
    public Image getImage() {
        return image;
    }

    /// Sets the leading image (album art, POI thumbnail, icon).
    ///
    /// #### Parameters
    ///
    /// - `image`: the leading image
    ///
    /// #### Returns
    ///
    /// this row, for chaining
    public CarRow setImage(Image image) {
        this.image = image;
        return this;
    }

    /// Returns true when this row drills into a sub-screen (renders a chevron).
    public boolean isBrowsable() {
        return browsable;
    }

    /// Marks this row as browsable, i.e. selecting it pushes a deeper `CarScreen` (the head unit
    /// renders a disclosure chevron). Set this for rows whose listener pushes another screen.
    ///
    /// #### Parameters
    ///
    /// - `browsable`: true to render a drill-in affordance
    ///
    /// #### Returns
    ///
    /// this row, for chaining
    public CarRow setBrowsable(boolean browsable) {
        this.browsable = browsable;
        return this;
    }

    /// Returns the activation listener, or null.
    public CarActionListener getOnAction() {
        return listener;
    }

    /// Sets the listener invoked (on the EDT) when the row is selected.
    ///
    /// #### Parameters
    ///
    /// - `listener`: the activation callback
    ///
    /// #### Returns
    ///
    /// this row, for chaining
    public CarRow setOnAction(CarActionListener listener) {
        this.listener = listener;
        return this;
    }
}
