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
package com.codename1.db;

import java.io.IOException;

/// The Cursor interface is used to iterate over the results returned from a database query.
/// **IMPORTANT:** Notice that some methods might not be supported on all platforms!
///
/// There is more thorough coverage of the `Database API here`.
///
/// The sample code below presents a Database Explorer tool that allows executing arbitrary SQL and
/// viewing the tabular results:
///
/// ```java
/// Toolbar.setGlobalToolbar(true);
/// Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
/// FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_QUERY_BUILDER, s);
/// Form hi = new Form("SQL Explorer", new BorderLayout());
/// hi.getToolbar().addCommandToRightBar("", icon, (e) -> {
///     TextArea query = new TextArea(3, 80);
///     Command ok = new Command("Execute");
///     Command cancel = new Command("Cancel");
///     if(Dialog.show("Query", query, ok, cancel) == ok) {
///         Database db = null;
///         Cursor cur = null;
///         try {
///             db = Display.getInstance().openOrCreate("MyDB.db");
///             if(query.getText().startsWith("select")) {
///                 cur = db.executeQuery(query.getText());
///                 int columns = cur.getColumnCount();
///                 hi.removeAll();
///                 if(columns > 0) {
///                     boolean next = cur.next();
///                     if(next) {
///                         ArrayList data = new ArrayList<>();
///                         String[] columnNames = new String[columns];
///                         for(int iter = 0 ; iter
///
/// @author Chen
public interface Cursor {

    /// Move the cursor to the first row.
    ///
    /// If cursor provides forward-only navigation and is positioned after the
    /// first row then calling first() method would throw a IOException.
    ///
    /// #### Returns
    ///
    /// true if succeeded
    ///
    /// #### Throws
    ///
    /// - `IOException`
    // PMD Fix (UnnecessaryModifier): Interface methods are implicitly public; remove redundant modifiers.
    boolean first() throws IOException;

    /// Move the cursor to the last row.
    ///
    /// #### Returns
    ///
    /// true if succeeded
    ///
    /// #### Throws
    ///
    /// - `IOException`
    boolean last() throws IOException;

    /// Moves the cursor to the next row.
    /// Calling next() method the first time will position cursor on the first.
    ///
    /// #### Returns
    ///
    /// true if succeeded
    ///
    /// #### Throws
    ///
    /// - `IOException`
    boolean next() throws IOException;

    /// Moves the cursor to the previous row.
    /// If cursor is forward type then calling prev() would throw a IOException.
    ///
    /// #### Returns
    ///
    /// true if succeeded
    ///
    /// #### Throws
    ///
    /// - `IOException`
    boolean prev() throws IOException;

    /// Returns the zero-based index for a given column name.
    /// Note that columns meta information is available only after navigation to
    /// the first row
    ///
    /// #### Parameters
    ///
    /// - `columnName`: the name of the column.
    ///
    /// #### Returns
    ///
    /// the index of the column
    ///
    /// #### Throws
    ///
    /// - `IOException`
    int getColumnIndex(String columnName) throws IOException;

    /// Returns the column name at a given zero-based column index.
    /// Note that columns meta information is available only after navigation to
    /// the first row
    ///
    /// #### Parameters
    ///
    /// - `columnIndex`: the index of the column
    ///
    /// #### Returns
    ///
    /// the name of the column
    ///
    /// #### Throws
    ///
    /// - `IOException`
    String getColumnName(int columnIndex) throws IOException;

    /// Returns the column count
    ///
    /// #### Returns
    ///
    /// the column count
    ///
    /// #### Throws
    ///
    /// - `IOException`
    int getColumnCount() throws IOException;

    /// Returns the current Cursor position.
    ///
    /// #### Returns
    ///
    /// the cursor position
    ///
    /// #### Throws
    ///
    /// - `IOException`
    int getPosition() throws IOException;

    /// Move the cursor to an absolute row position
    ///
    /// #### Parameters
    ///
    /// - `row`: position to move to
    ///
    /// #### Returns
    ///
    /// true if succeeded
    ///
    /// #### Throws
    ///
    /// - `IOException`
    boolean position(int row) throws IOException;

    /// Close the cursor and release its resources
    ///
    /// #### Throws
    ///
    /// - `IOException`
    void close() throws IOException;

    /// Get the Row data Object.
    ///
    /// #### Returns
    ///
    /// a Row Object
    ///
    /// #### Throws
    ///
    /// - `IOException`
    Row getRow() throws IOException;

}
