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

/// A scrolling list of selectable rows, optionally grouped into `CarSection`s, with an optional
/// header `CarActionStrip`. The most common in-car template -- audio browse, POI lists, settings.
/// Maps to `androidx.car.app.model.ListTemplate` and `CPListTemplate`.
///
/// Head units enforce a hard row cap (see `com.codename1.car.spi.CarBridge#getListRowLimit()` /
/// `CarContext#getListRowLimit()`); rows beyond the cap are dropped by the platform.
///
/// ```java
/// CarListTemplate t = new CarListTemplate().setTitle("Library");
/// t.addRow(new CarRow("Songs").setBrowsable(true).setOnAction(ctx -> ctx.pushScreen(new SongsScreen())));
/// t.addRow(new CarRow("Albums").setBrowsable(true).setOnAction(ctx -> ctx.pushScreen(new AlbumsScreen())));
/// ```
public class CarListTemplate extends CarTemplate {
    private final List<CarSection> sections = new ArrayList<CarSection>();
    private CarSection defaultSection;
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
    public CarListTemplate setTitle(String title) {
        setTitleInternal(title);
        return this;
    }

    /// Adds a row to the template's default (untitled) section, creating it on first use. Use
    /// [#addSection(CarSection)] when you need grouped rows.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row to add
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarListTemplate addRow(CarRow row) {
        if (defaultSection == null) {
            defaultSection = new CarSection();
            sections.add(defaultSection);
        }
        defaultSection.addRow(row);
        return this;
    }

    /// Adds a titled section of rows.
    ///
    /// #### Parameters
    ///
    /// - `section`: the section to add
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarListTemplate addSection(CarSection section) {
        if (section != null) {
            sections.add(section);
        }
        return this;
    }

    /// Returns the sections of this list, in order.
    public List<CarSection> getSections() {
        return sections;
    }

    /// Returns the header action strip, or null.
    public CarActionStrip getHeaderActions() {
        return headerActions;
    }

    /// Sets the header action strip (e.g. a refresh or search action).
    ///
    /// #### Parameters
    ///
    /// - `headerActions`: the action strip
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarListTemplate setHeaderActions(CarActionStrip headerActions) {
        this.headerActions = headerActions;
        return this;
    }

    /// Returns true when the list should render a loading spinner instead of content.
    public boolean isLoading() {
        return loading;
    }

    /// Marks the template as loading; the head unit renders a spinner and ignores any rows. Set this
    /// while fetching content, then `CarScreen#invalidate()` once the rows are ready.
    ///
    /// #### Parameters
    ///
    /// - `loading`: true to show a spinner
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarListTemplate setLoading(boolean loading) {
        this.loading = loading;
        return this;
    }
}
