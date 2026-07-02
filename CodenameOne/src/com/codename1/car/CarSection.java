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

/// A titled group of `CarRow`s within a `CarListTemplate`. Maps to a sectioned list
/// (`androidx.app.car` `ItemList` with a header / `CPListSection`). The header may be null for an
/// untitled section.
public class CarSection {
    private String header;
    private final List<CarRow> rows = new ArrayList<CarRow>();

    /// Creates an untitled section.
    public CarSection() {
    }

    /// Creates a section with the supplied header.
    ///
    /// #### Parameters
    ///
    /// - `header`: the section header, or null
    public CarSection(String header) {
        this.header = header;
    }

    /// Returns the section header, or null.
    public String getHeader() {
        return header;
    }

    /// Sets the section header.
    ///
    /// #### Parameters
    ///
    /// - `header`: the header text, or null for an untitled section
    ///
    /// #### Returns
    ///
    /// this section, for chaining
    public CarSection setHeader(String header) {
        this.header = header;
        return this;
    }

    /// Appends a row to the section.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row to add
    ///
    /// #### Returns
    ///
    /// this section, for chaining
    public CarSection addRow(CarRow row) {
        if (row != null) {
            rows.add(row);
        }
        return this;
    }

    /// Returns the rows in this section, in order.
    public List<CarRow> getRows() {
        return rows;
    }
}
