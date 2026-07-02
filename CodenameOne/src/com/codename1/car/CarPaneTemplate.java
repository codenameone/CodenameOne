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

/// A detail pane: a handful of informational `CarRow`s (label / value lines) plus up to two action
/// buttons. Use it for a POI detail card, a "now connected" status, or a confirmation screen. Maps
/// to `androidx.car.app.model.PaneTemplate` and `CPInformationTemplate`.
public class CarPaneTemplate extends CarTemplate {
    private final List<CarRow> rows = new ArrayList<CarRow>();
    private final List<CarAction> actions = new ArrayList<CarAction>();
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
    public CarPaneTemplate setTitle(String title) {
        setTitleInternal(title);
        return this;
    }

    /// Adds an informational row (its title is the label, its text the value).
    ///
    /// #### Parameters
    ///
    /// - `row`: the row to add
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarPaneTemplate addRow(CarRow row) {
        if (row != null) {
            rows.add(row);
        }
        return this;
    }

    /// Returns the informational rows, in order.
    public List<CarRow> getRows() {
        return rows;
    }

    /// Adds a pane action button (the head unit displays at most two).
    ///
    /// #### Parameters
    ///
    /// - `action`: the action button to add
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarPaneTemplate addAction(CarAction action) {
        if (action != null) {
            actions.add(action);
        }
        return this;
    }

    /// Returns the pane action buttons, in order.
    public List<CarAction> getActions() {
        return actions;
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
    public CarPaneTemplate setHeaderActions(CarActionStrip headerActions) {
        this.headerActions = headerActions;
        return this;
    }

    /// Returns true when the pane should render a loading spinner instead of content.
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
    public CarPaneTemplate setLoading(boolean loading) {
        this.loading = loading;
        return this;
    }
}
