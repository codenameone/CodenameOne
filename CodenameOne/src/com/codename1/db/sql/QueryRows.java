/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.db.sql;

import java.util.ArrayList;

/// Driver result rows with their physical projection width, before duplicate labels
/// are collapsed into the row maps. Retained even when a query returns no rows.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class QueryRows extends ArrayList {
    private static final long serialVersionUID = 1L;
    private int columnCount;
    private String[] columnTypes;
    private String[] columnNames;

    public QueryRows(int columnCount) {
        this.columnCount = columnCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    /// The portable kind of each column, in projection order, or null when the
    /// driver did not record them. See `Database.columnTypes(List)`.
    public String[] getColumnTypes() {
        return columnTypes == null ? null : (String[]) columnTypes.clone();
    }

    public void setColumnTypes(String[] columnTypes) {
        this.columnTypes = columnTypes == null ? null : (String[]) columnTypes.clone();
    }

    /// The label of each column, in projection order, or null when the driver did
    /// not record them. Known even when no row came back, and listing a label twice
    /// where the projection repeats one, which the row maps cannot.
    public String[] getColumnNames() {
        return columnNames == null ? null : (String[]) columnNames.clone();
    }

    /// The column names themselves, not a copy, for a caller that only reads
    /// them -- the generated DB-API connection does so on every statement, where
    /// a copy per call was an allocation per statement. Must not be changed.
    public String[] sharedColumnNames() {
        return columnNames;
    }

    /// Takes `columnNames` as it is rather than copying it; the caller hands over
    /// an array nothing changes afterwards.
    public void adoptColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames == null ? null : (String[]) columnNames.clone();
    }
}
