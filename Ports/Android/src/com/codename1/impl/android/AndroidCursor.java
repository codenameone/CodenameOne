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
package com.codename1.impl.android;

import com.codename1.db.Cursor;
import com.codename1.db.CursorExt;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.db.RowExt;
import java.io.IOException;

/**
 * Cursor over an android.database.Cursor.
 *
 * This does not use the shared AbstractDBCursor: Android's cursor is already a windowed random
 * access cursor, so re-deriving navigation from a forward-only primitive would make seeking
 * slower rather than faster. The externally visible behaviour is the same either way, and the
 * portable conformance suite checks that.
 *
 * @author Chen
 */
public class AndroidCursor implements Cursor, CursorExt, RowExt {

    private android.database.Cursor c;
    private int last_read_column_index = -1;
    private boolean closed;

    public AndroidCursor(android.database.Cursor c) {
        this.c = c;
        this.last_read_column_index = -1;
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("This cursor has been closed");
        }
    }

    /**
     * Marks the cursor dead because the database that owned it was closed. The underlying cursor
     * belongs to the connection and is already gone.
     */
    void invalidate() {
        closed = true;
    }

    @Override
    public boolean first() throws IOException {
        checkOpen();
        last_read_column_index = -1;
        return c.moveToFirst();
    }

    @Override
    public boolean last() throws IOException {
        checkOpen();
        last_read_column_index = -1;
        return c.moveToLast();
    }

    @Override
    public boolean next() throws IOException {
        checkOpen();
        last_read_column_index = -1;
        return c.moveToNext();
    }

    @Override
    public boolean prev() throws IOException {
        checkOpen();
        last_read_column_index = -1;
        return c.moveToPrevious();
    }

    @Override
    public void beforeFirst() throws IOException {
        checkOpen();
        last_read_column_index = -1;
        c.moveToPosition(-1);
    }

    @Override
    public int getCount() throws IOException {
        checkOpen();
        return c.getCount();
    }

    @Override
    public int getColumnIndex(String columnName) throws IOException {
        checkOpen();
        // Android's own lookup is case sensitive; the portable contract is not.
        int direct = c.getColumnIndex(columnName);
        if (direct >= 0) {
            return direct;
        }
        String[] names = c.getColumnNames();
        for (int iter = 0; iter < names.length; iter++) {
            if (names[iter] != null && names[iter].equalsIgnoreCase(columnName)) {
                return iter;
            }
        }
        return -1;
    }

    @Override
    public String getColumnName(int columnIndex) throws IOException {
        checkOpen();
        return c.getColumnName(columnIndex);
    }

    @Override
    public int getPosition() throws IOException {
        checkOpen();
        return c.getPosition();
    }

    @Override
    public Row getRow() throws IOException {
        checkOpen();
        int position = c.getPosition();
        if (position < 0 || position >= c.getCount()) {
            throw new IOException("The cursor is not on a row. Call next(), first(), last() or "
                    + "position(int) and check that it returned true before calling getRow().");
        }
        return this;
    }

    @Override
    public boolean position(int row) throws IOException {
        checkOpen();
        last_read_column_index = -1;
        if (row < 0) {
            c.moveToPosition(-1);
            return false;
        }
        return c.moveToPosition(row);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        c.close();
    }

    @Override
    public byte[] getBlob(int index) throws IOException {
        checkOpen();
        last_read_column_index = index;
        return c.getBlob(index);
    }

    @Override
    public double getDouble(int index) throws IOException {
        checkOpen();
        last_read_column_index = index;
        return c.getDouble(index);
    }

    @Override
    public float getFloat(int index) throws IOException {
        checkOpen();
        last_read_column_index = index;
        return c.getFloat(index);
    }

    @Override
    public int getInteger(int index) throws IOException {
        checkOpen();
        last_read_column_index = index;
        return c.getInt(index);
    }

    @Override
    public long getLong(int index) throws IOException {
        checkOpen();
        last_read_column_index = index;
        return c.getLong(index);
    }

    @Override
    public short getShort(int index) throws IOException {
        checkOpen();
        last_read_column_index = index;
        return c.getShort(index);
    }

    @Override
    public String getString(int index) throws IOException {
        checkOpen();
        last_read_column_index = index;
        return c.getString(index);
    }

    public boolean isNull(int index) throws IOException {
        checkOpen();
        return c.isNull(index);
    }

    @Override
    public boolean wasNull() throws IOException {
        if (last_read_column_index < 0) {
            // Before anything has been read there is no "last value", so this used to answer
            // true, which says the value you have not read is null. False is the answer JDBC
            // gives and the one the portable contract specifies.
            return Database.isLegacyBehavior();
        }
        return c.isNull(last_read_column_index);
    }

    @Override
    public int getColumnCount() throws IOException {
        checkOpen();
        return c.getColumnCount();
    }
}
