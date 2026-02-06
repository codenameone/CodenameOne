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

/// The `Row` interface is returned by  `com.codename1.db.Cursor#getRow()` to provide
/// access to the content of an individual row.
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
public interface Row {

    /// Gets column value by index.
    ///
    /// #### Parameters
    ///
    /// - `index`: starts with zero
    ///
    /// #### Returns
    ///
    /// byte [] data
    ///
    /// #### Throws
    ///
    /// - `IOException`
    // PMD Fix (UnnecessaryModifier): Remove redundant public modifiers from interface methods.
    byte[] getBlob(int index) throws IOException;

    /// Gets column value by index.
    ///
    /// #### Parameters
    ///
    /// - `index`: starts with zero
    ///
    /// #### Returns
    ///
    /// a double data from the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    double getDouble(int index) throws IOException;

    /// Gets column value by index.
    ///
    /// #### Parameters
    ///
    /// - `index`: starts with zero
    ///
    /// #### Returns
    ///
    /// a float data from the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    float getFloat(int index) throws IOException;

    /// Gets column value by index.
    ///
    /// #### Parameters
    ///
    /// - `index`: starts with zero
    ///
    /// #### Returns
    ///
    /// a int data from the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    int getInteger(int index) throws IOException;

    /// Gets column value by index.
    ///
    /// #### Parameters
    ///
    /// - `index`: starts with zero
    ///
    /// #### Returns
    ///
    /// a long data from the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    long getLong(int index) throws IOException;

    /// Gets column value by index.
    ///
    /// #### Parameters
    ///
    /// - `index`: starts with zero
    ///
    /// #### Returns
    ///
    /// a short data from the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    short getShort(int index) throws IOException;

    /// Gets column value by index.
    ///
    /// #### Parameters
    ///
    /// - `index`: starts with zero
    ///
    /// #### Returns
    ///
    /// a String data from the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    String getString(int index) throws IOException;


    /// Reports whether the last column read had a value of SQL NULL.
    /// Note that you must first call one of the getter methods on a column to try to read its value and then call the method wasNull to see if the value read was SQL NULL.
    ///
    /// #### Returns
    ///
    /// true if the last column value read was SQL NULL and false otherwise. If the targeted platform do not support this feature it returns null (meaning we don't now if the latest read value was SQL NULL or not)
    ///
    /// #### Throws
    ///
    /// - `IOException`
    //public Boolean wasNull()throws IOException;
}
