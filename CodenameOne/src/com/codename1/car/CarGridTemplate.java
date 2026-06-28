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

import java.util.ArrayList;
import java.util.List;

/// A grid of image-forward `CarGridItem`s -- browse categories, station presets, playlist artwork.
/// Maps to `androidx.car.app.model.GridTemplate` and `CPGridTemplate` (which itself caps the grid,
/// typically at eight buttons; see `CarContext#getGridItemLimit()`).
public class CarGridTemplate extends CarTemplate {
    private final List<CarGridItem> items = new ArrayList<CarGridItem>();
    private CarActionStrip headerActions;
    private boolean loading;

    /// Sets the header title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the header title
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarGridTemplate setTitle(String title) {
        setTitleInternal(title);
        return this;
    }

    /// Adds an item to the grid.
    ///
    /// #### Parameters
    ///
    /// - `item`: the grid item to add
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarGridTemplate addItem(CarGridItem item) {
        if (item != null) {
            items.add(item);
        }
        return this;
    }

    /// Returns the grid items, in order.
    public List<CarGridItem> getItems() {
        return items;
    }

    /// Returns the header action strip, or null.
    public CarActionStrip getHeaderActions() {
        return headerActions;
    }

    /// Sets the header action strip.
    ///
    /// #### Parameters
    ///
    /// - `headerActions`: the action strip
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarGridTemplate setHeaderActions(CarActionStrip headerActions) {
        this.headerActions = headerActions;
        return this;
    }

    /// Returns true when the grid should render a loading spinner instead of content.
    public boolean isLoading() {
        return loading;
    }

    /// Marks the template as loading; the head unit renders a spinner.
    ///
    /// #### Parameters
    ///
    /// - `loading`: true to show a spinner
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarGridTemplate setLoading(boolean loading) {
        this.loading = loading;
        return this;
    }
}
