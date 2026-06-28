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

/// A single cell in a `CarGridTemplate`: a large image with a short title and optional secondary
/// text. Maps to `androidx.car.app.model.GridItem` and to a `CPGridButton` on CarPlay. Grid items
/// are image-forward (browse categories, presets, playlists) -- always supply an image.
public class CarGridItem {
    private String title;
    private String text;
    private Image image;
    private CarActionListener listener;

    /// Creates an empty grid item.
    public CarGridItem() {
    }

    /// Creates a grid item with a title and image.
    ///
    /// #### Parameters
    ///
    /// - `title`: the item label
    ///
    /// - `image`: the item image
    public CarGridItem(String title, Image image) {
        this.title = title;
        this.image = image;
    }

    /// Returns the item title.
    public String getTitle() {
        return title;
    }

    /// Sets the item title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the label
    ///
    /// #### Returns
    ///
    /// this item, for chaining
    public CarGridItem setTitle(String title) {
        this.title = title;
        return this;
    }

    /// Returns the secondary text line, or null.
    public String getText() {
        return text;
    }

    /// Sets the secondary text line.
    ///
    /// #### Parameters
    ///
    /// - `text`: the secondary line
    ///
    /// #### Returns
    ///
    /// this item, for chaining
    public CarGridItem setText(String text) {
        this.text = text;
        return this;
    }

    /// Returns the item image, or null.
    public Image getImage() {
        return image;
    }

    /// Sets the item image.
    ///
    /// #### Parameters
    ///
    /// - `image`: the image
    ///
    /// #### Returns
    ///
    /// this item, for chaining
    public CarGridItem setImage(Image image) {
        this.image = image;
        return this;
    }

    /// Returns the activation listener, or null.
    public CarActionListener getOnAction() {
        return listener;
    }

    /// Sets the listener invoked (on the EDT) when the item is selected.
    ///
    /// #### Parameters
    ///
    /// - `listener`: the activation callback
    ///
    /// #### Returns
    ///
    /// this item, for chaining
    public CarGridItem setOnAction(CarActionListener listener) {
        this.listener = listener;
        return this;
    }
}
