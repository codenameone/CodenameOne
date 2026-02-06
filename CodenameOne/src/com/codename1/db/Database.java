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

import com.codename1.ui.Display;

import java.io.IOException;

/// Allows access to SQLite specifically connecting to a database and executing sql queries on the data.
/// There is more thorough coverage of the `Database API here`.
///
/// The Database class abstracts the underlying SQLite of the device if
/// available.
///
/// Notice that this might not be supported on all platforms in which case the `Database` will be null.
///
/// SQLite should be used for very large data handling, for small storage
/// refer to `com.codename1.io.Storage` which is more portable.
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
public abstract class Database {

    /// Checks if this platform supports custom database paths.  On platforms that
    /// support this, you can pass a file path to `#openOrCreate(java.lang.String)`, `#exists(java.lang.String)`,
    /// `#delete(java.lang.String)`, and `#getDatabasePath(java.lang.String)`.
    ///
    /// #### Returns
    ///
    /// True on platorms that support custom database paths.
    public static boolean isCustomPathSupported() {
        return Display.getInstance().isDatabaseCustomPathSupported();
    }

    private static void validateDatabaseNameArgument(String databaseName) {
        // PMD Fix (CollapsibleIfStatements): Merge the custom path support and separator checks into one condition.
        if (!isCustomPathSupported() && (databaseName.indexOf("/") != -1 || databaseName.indexOf("\\") != -1)) {
            throw new IllegalArgumentException("This platform does not support custom database paths.  The database name cannot contain file separators.");
        }
    }

    /// Opens a database or create one if not exists.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database.  Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    /// #### Returns
    ///
    /// Database Object or null if not supported on the platform
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database cannot be created
    public static Database openOrCreate(String databaseName) throws IOException {
        validateDatabaseNameArgument(databaseName);
        return Display.getInstance().openOrCreate(databaseName);
    }

    /// Indicates weather a database exists
    ///
    /// **NOTE:** Not supported in the  Javascript port.  Will always return false.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database.  Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    /// #### Returns
    ///
    /// true if database exists
    public static boolean exists(String databaseName) {
        validateDatabaseNameArgument(databaseName);
        return Display.getInstance().exists(databaseName);
    }

    /// Deletes database
    ///
    /// **NOTE:** This method is not supported in the  Javascript port.  Will silently fail.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database. Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database cannot be deleted
    public static void delete(String databaseName) throws IOException {
        validateDatabaseNameArgument(databaseName);
        Display.getInstance().delete(databaseName);
    }

    /// Returns the file path of the Database if exists and if supported on
    /// the platform.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName The name of the database. Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    ///
    /// **NOTE:** This method will return null in the Javascript port.
    ///
    /// #### Returns
    ///
    /// the file path of the database
    public static String getDatabasePath(String databaseName) {
        validateDatabaseNameArgument(databaseName);
        return Display.getInstance().getDatabasePath(databaseName);
    }

    /// Checks if the last value accessed from a given row was null.  Not all platforms
    /// support wasNull().  If the platform does not support it, this will just return false.
    ///
    /// Check `#supportsWasNull(com.codename1.db.Row)` to see if the platform supports
    /// wasNull().
    ///
    /// Currently wasNull() is supported on UWP, iOS, Android, and JavaSE (Simulator).
    ///
    /// #### Parameters
    ///
    /// - `row`: The row to check.
    ///
    /// #### Returns
    ///
    /// True if the last value accessed was null.
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - RowExt#wasNull()
    ///
    /// - #supportsWasNull(com.codename1.db.Row)
    public static boolean wasNull(Row row) throws IOException {
        if (row instanceof RowExt) {
            return ((RowExt) row).wasNull();
        }
        return false;
    }

    /// Checks to see if the given row supports `#wasNull(com.codename1.db.Row)`.
    ///
    /// #### Parameters
    ///
    /// - `row`: The row to check.
    ///
    /// #### Returns
    ///
    /// True if the row supports wasNull().
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #wasNull(com.codename1.db.Row)
    ///
    /// - RowExt#wasNull()
    public static boolean supportsWasNull(Row row) throws IOException {
        return row instanceof RowExt;
    }

    /// Starts a transaction
    ///
    /// **NOTE:** Not supported in Javascript port.  This method will do nothing when running in Javascript.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database is not opened
    public abstract void beginTransaction() throws IOException;

    /// Commits current transaction
    ///
    /// **NOTE:** Not supported in Javascript port.   This method will do nothing when running in Javascript.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database is not opened or transaction was not started
    public abstract void commitTransaction() throws IOException;

    /// Rolls back current transaction
    ///
    /// **NOTE:** Not supported in Javascript port.   This method will do nothing when running in Javascript.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database is not opened or transaction was not started
    public abstract void rollbackTransaction() throws IOException;

    /// Closes the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract void close() throws IOException;

    /// Execute an update query.
    /// Used for INSERT, UPDATE, DELETE and similar sql statements.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract void execute(String sql) throws IOException;

    /// Execute an update query with params.
    /// Used for INSERT, UPDATE, DELETE and similar sql statements.
    /// The sql can be constructed with '?' and the params will be binded to the
    /// query
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: to bind to the query where the '?' exists
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract void execute(String sql, String[] params) throws IOException;

    /// Execute an update query with params.
    /// Used for INSERT, UPDATE, DELETE and similar sql statements.
    /// The sql can be constructed with '?' and the params will be binded to the
    /// query
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: @param params to bind to the query where the '?' exists, supported object
    ///               types are String, byte[], Double, Long and null
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public void execute(String sql, Object... params) throws IOException {
        if (params == null) {
            execute(sql);
        } else {
            //throw new RuntimeException("not implemented");
            int len = params.length;
            String[] strParams = new String[len];
            for (int i = 0; i < len; i++) {
                if (params[i] instanceof byte[]) {
                    throw new RuntimeException("Blobs aren't supported on this platform");
                }
                if (params[i] == null) {
                    strParams[i] = null;
                } else {
                    strParams[i] = params[i].toString();
                }
            }
            execute(sql, strParams);
        }

    }

    /// This method should be called with SELECT type statements that return
    /// row set.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: to bind to the query where the '?' exists
    ///
    /// #### Returns
    ///
    /// a cursor to iterate over the results
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract Cursor executeQuery(String sql, String[] params) throws IOException;

    /// This method should be called with SELECT type statements that return
    /// row set it accepts object with params.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: @param params to bind to the query where the '?' exists, supported object
    ///               types are String, byte[], Double, Long and null
    ///
    /// #### Returns
    ///
    /// a cursor to iterate over the results
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            return executeQuery(sql);
        } else {
            int len = params.length;
            String[] strParams = new String[len];
            for (int i = 0; i < len; i++) {
                if (params[i] instanceof byte[]) {
                    throw new RuntimeException("Blobs aren't supported on this platform");
                }
                if (params[i] == null) {
                    strParams[i] = null;
                } else {
                    strParams[i] = params[i].toString();
                }
            }
            return executeQuery(sql, strParams);
        }
    }

    /// This method should be called with SELECT type statements that return
    /// row set.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// #### Returns
    ///
    /// a cursor to iterate over the results
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract Cursor executeQuery(String sql) throws IOException;

}
