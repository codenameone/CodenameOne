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
package android.database.sqlite;

import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;
import android.util.Log;

import com.codename1.db.Row;
import com.codename1.db.RowExt;

import java.io.IOException;
import java.util.ArrayList;

/// A cursor over the result of an SQLite query.
///
/// Android reads a result into a `CursorWindow`, a block of memory the
/// cursor moves around in freely. Codename One's database cursors only step
/// forward cheaply (going back re-runs the statement), so this cursor does
/// the same thing as the window: the query runs once, when the cursor is
/// created, and every row is read into memory as text with its NULL flag.
/// Numeric getters convert that text the way SQLite converts a value
/// (`getLong("12.5")` is 12, non-numeric text is 0).
///
/// Blob bytes are buffered alongside the text so every getter sees the
/// same query result, even on ports where moving backward re-runs a query.
///
/// `getType` is inferred from the text, since Codename One reports no
/// column types: NULL, then INTEGER for an integer literal, FLOAT for any
/// other number, STRING otherwise. Text that happens to look like a number
/// therefore reports a numeric type, and a blob reports STRING.
public class SQLiteCursor extends AbstractCursor {

    static final String TAG = "SQLiteCursor";

    private final String mEditTable;
    private final SQLiteCursorDriver mDriver;
    private final SQLiteQuery mQuery;
    private String[] mColumns;
    private ArrayList<String[]> mRows;
    private ArrayList<boolean[]> mNulls;
    private ArrayList<byte[][]> mBlobs;
    private com.codename1.db.Cursor mBacking;

    @Deprecated
    public SQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
        this(driver, editTable, query);
    }

    public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query object cannot be null");
        }
        mDriver = driver;
        mEditTable = editTable;
        mQuery = query;
        fill();
    }

    /// Runs the query and buffers its rows.
    private void fill() {
        closeBacking();
        com.codename1.db.Cursor c = mQuery.run();
        ArrayList<String[]> rows = new ArrayList<String[]>();
        ArrayList<boolean[]> nulls = new ArrayList<boolean[]>();
        ArrayList<byte[][]> blobs = new ArrayList<byte[][]>();
        String[] columns;
        try {
            int n = c.getColumnCount();
            columns = new String[n];
            for (int i = 0; i < n; i++) {
                columns[i] = c.getColumnName(i);
            }
            while (c.next()) {
                Row row = c.getRow();
                String[] values = new String[n];
                boolean[] isNull = new boolean[n];
                byte[][] bytes = new byte[n][];
                for (int i = 0; i < n; i++) {
                    String s = row.getString(i);
                    boolean wasNull = row instanceof RowExt ? ((RowExt) row).wasNull() : s == null;
                    isNull[i] = wasNull;
                    values[i] = wasNull ? null : s;
                    if (!wasNull) {
                        try {
                            byte[] blob = row.getBlob(i);
                            bytes[i] = blob == null ? null : blob.clone();
                        } catch (IOException unsupportedConversion) {
                            // Some ports reject getBlob() for numeric/text cells.
                            // Preserve the existing UTF-8 fallback in the snapshot.
                            bytes[i] = s.getBytes("UTF-8");
                        }
                    }
                }
                rows.add(values);
                nulls.add(isNull);
                blobs.add(bytes);
            }
        } catch (IOException e) {
            try {
                c.close();
            } catch (IOException ignored) {
                // the query already failed; that is the error to report
            }
            throw mQuery.getDatabase().toSqlException(e, mQuery.getSql());
        }
        mColumns = columns;
        mRows = rows;
        mNulls = nulls;
        mBlobs = blobs;
        mBacking = c;
        closeBacking();
    }

    private void closeBacking() {
        if (mBacking != null) {
            try {
                mBacking.close();
            } catch (IOException e) {
                Log.w(TAG, "closing the result set: " + e);
            }
            mBacking = null;
        }
    }

    public SQLiteDatabase getDatabase() {
        return mQuery.getDatabase();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return true;
    }

    @Override
    public int getCount() {
        return mRows == null ? 0 : mRows.size();
    }

    @Override
    public String[] getColumnNames() {
        return mColumns == null ? new String[0] : mColumns;
    }

    private String value(int column) {
        checkPosition();
        if (column < 0 || column >= mColumns.length) {
            throw new CursorIndexOutOfBoundsException("Requested column: " + column + ", # of columns: "
                    + mColumns.length);
        }
        return mRows.get(mPos)[column];
    }

    @Override
    public String getString(int column) {
        return value(column);
    }

    @Override
    public short getShort(int column) {
        return (short) getLong(column);
    }

    @Override
    public int getInt(int column) {
        return (int) getLong(column);
    }

    @Override
    public long getLong(int column) {
        String v = value(column);
        return v == null ? 0 : toLong(v);
    }

    @Override
    public float getFloat(int column) {
        return (float) getDouble(column);
    }

    @Override
    public double getDouble(int column) {
        String v = value(column);
        return v == null ? 0 : toDouble(v);
    }

    @Override
    public boolean isNull(int column) {
        value(column);
        return mNulls.get(mPos)[column];
    }

    @Override
    public int getType(int column) {
        String v = value(column);
        if (v == null) {
            return FIELD_TYPE_NULL;
        }
        int kind = numericKind(v);
        if (kind == 1) {
            return FIELD_TYPE_INTEGER;
        }
        if (kind == 2) {
            return FIELD_TYPE_FLOAT;
        }
        return FIELD_TYPE_STRING;
    }

    @Override
    public byte[] getBlob(int column) {
        value(column); // validates row and column
        return mBlobs.get(mPos)[column];
    }

    public void setSelectionArguments(String[] selectionArgs) {
        mDriver.setBindArguments(selectionArgs);
    }

    @Override
    @Deprecated
    public void deactivate() {
        super.deactivate();
        mDriver.cursorDeactivated();
    }

    @Override
    public void close() {
        super.close();
        closeBacking();
        mRows = null;
        mNulls = null;
        mBlobs = null;
        mQuery.close();
        mDriver.cursorClosed();
    }

    @Override
    @Deprecated
    public boolean requery() {
        if (isClosed()) {
            return false;
        }
        try {
            mPos = -1;
            fill();
        } catch (IllegalStateException e) {
            Log.w(TAG, "requery() failed " + e.getMessage());
            return false;
        } catch (SQLiteException e) {
            Log.w(TAG, "requery() failed " + e.getMessage());
            return false;
        }
        mDriver.cursorRequeried(this);
        return super.requery();
    }

    public void setFillWindowForwardOnly(boolean fillWindowForwardOnly) {
    }

    @Override
    public String toString() {
        return "SQLiteCursor: " + mQuery.getSql() + (mEditTable == null ? "" : " (" + mEditTable + ")");
    }

    // ------------------------------------------------------------ conversions

    /// 1 for an integer literal, 2 for any other number, 0 otherwise.
    static int numericKind(String s) {
        int n = s.length();
        if (n == 0) {
            return 0;
        }
        int i = 0;
        if (s.charAt(0) == '-' || s.charAt(0) == '+') {
            i++;
        }
        boolean digits = false;
        boolean real = false;
        while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
            digits = true;
        }
        if (i < n && s.charAt(i) == '.') {
            real = true;
            i++;
            while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
                digits = true;
            }
        }
        if (!digits) {
            return 0;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            real = true;
            i++;
            if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
                i++;
            }
            boolean exp = false;
            while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
                exp = true;
            }
            if (!exp) {
                return 0;
            }
        }
        if (i != n) {
            return 0;
        }
        return real ? 2 : 1;
    }

    /// SQLite's text to integer conversion: the longest numeric prefix,
    /// truncated toward zero, or 0.
    static long toLong(String s) {
        String t = s.trim();
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            double d = toDouble(t);
            if (d >= 9.223372036854775807E18) {
                return Long.MAX_VALUE;
            }
            if (d <= -9.223372036854775808E18) {
                return Long.MIN_VALUE;
            }
            return (long) d;
        }
    }

    /// SQLite's text to real conversion: the longest numeric prefix, or 0.
    static double toDouble(String s) {
        String t = s.trim();
        int end = numericPrefix(t);
        if (end == 0) {
            return 0;
        }
        try {
            return Double.parseDouble(t.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int numericPrefix(String s) {
        int n = s.length();
        int i = 0;
        if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
            i++;
        }
        int start = i;
        while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
        }
        if (i < n && s.charAt(i) == '.') {
            i++;
            while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
            }
        }
        if (i == start || (i == start + 1 && s.charAt(start) == '.')) {
            return 0;
        }
        int mantissaEnd = i;
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '-' || s.charAt(j) == '+')) {
                j++;
            }
            int expStart = j;
            while (j < n && s.charAt(j) >= '0' && s.charAt(j) <= '9') {
                j++;
            }
            if (j > expStart) {
                return j;
            }
        }
        return mantissaEnd;
    }
}
