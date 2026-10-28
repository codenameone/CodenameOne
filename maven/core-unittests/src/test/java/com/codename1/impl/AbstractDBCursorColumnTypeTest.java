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
package com.codename1.impl;

import com.codename1.db.Database;
import com.codename1.db.Row;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The column type a cursor reports is the storage class the value had before
/// any getter read it. SQLite's sqlite3_column_type is undefined once a getter
/// converted the value, so the type is cached per row.
class AbstractDBCursorColumnTypeTest {

    /// Two rows of one INTEGER column. Reading the value as text "converts" it,
    /// after which the engine answers TEXT, as SQLite may.
    private static final class ConvertingCursor extends AbstractDBCursor {
        int row = -1;
        boolean converted;
        int typeQueries;

        protected void rewind() {
            row = -1;
            converted = false;
        }

        protected boolean stepForward() {
            row++;
            converted = false;
            return row < 2;
        }

        protected void closeImpl() {
        }

        protected int columnCount() {
            return 1;
        }

        protected String columnLabel(int columnIndex) {
            return "c";
        }

        protected boolean isNullAt(int index) {
            return false;
        }

        protected int columnType(int index) {
            typeQueries++;
            return converted ? Database.COLUMN_TEXT : Database.COLUMN_INTEGER;
        }

        protected String readString(int index) {
            converted = true;
            return String.valueOf(row);
        }

        protected byte[] readBlob(int index) {
            return new byte[0];
        }

        protected double readDouble(int index) {
            return row;
        }

        protected long readLong(int index) {
            return row;
        }
    }

    /// An engine that cannot say: the base class default.
    private static final class UntypedCursor extends AbstractDBCursor {
        private int row = -1;

        protected void rewind() {
            row = -1;
        }

        protected boolean stepForward() {
            row++;
            return row < 1;
        }

        protected void closeImpl() {
        }

        protected int columnCount() {
            return 1;
        }

        protected String columnLabel(int columnIndex) {
            return "c";
        }

        protected boolean isNullAt(int index) {
            return false;
        }

        protected String readString(int index) {
            return "x";
        }

        protected byte[] readBlob(int index) {
            return new byte[0];
        }

        protected double readDouble(int index) {
            return 0;
        }

        protected long readLong(int index) {
            return 0;
        }
    }

    @Test
    void getterBeforeGetColumnTypeStillReportsTheStoredClass() throws IOException {
        ConvertingCursor cursor = new ConvertingCursor();
        cursor.next();
        Row row = cursor.getRow();
        assertEquals("0", row.getString(0));
        assertEquals(Database.COLUMN_INTEGER, Database.getColumnType(row, 0));
        assertEquals(Database.COLUMN_INTEGER, cursor.getColumnType(0));
    }

    @Test
    void cacheIsPerRow() throws IOException {
        ConvertingCursor cursor = new ConvertingCursor();
        cursor.next();
        assertEquals(Database.COLUMN_INTEGER, cursor.getColumnType(0));
        int queries = cursor.typeQueries;
        cursor.getString(0);
        assertEquals(Database.COLUMN_INTEGER, cursor.getColumnType(0));
        assertEquals(queries, cursor.typeQueries);
        cursor.next();
        assertEquals(Database.COLUMN_INTEGER, cursor.getColumnType(0));
        assertEquals(queries + 1, cursor.typeQueries);
        cursor.getString(0);
        assertEquals(Database.COLUMN_INTEGER, cursor.getColumnType(0));
    }

    @Test
    void cacheSurvivesRewind() throws IOException {
        ConvertingCursor cursor = new ConvertingCursor();
        cursor.next();
        cursor.getString(0);
        cursor.first();
        assertEquals(Database.COLUMN_INTEGER, cursor.getColumnType(0));
    }

    @Test
    void engineThatCannotSayAnswersUnknown() throws IOException {
        UntypedCursor cursor = new UntypedCursor();
        cursor.next();
        cursor.getString(0);
        assertEquals(Database.COLUMN_UNKNOWN, Database.getColumnType(cursor.getRow(), 0));
    }
}
