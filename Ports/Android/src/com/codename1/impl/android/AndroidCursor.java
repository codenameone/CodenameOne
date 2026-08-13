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

    /**
     * Notified when this cursor closes, so the database can stop tracking it. Without this a long
     * lived database that opens and closes many cursors would retain every one of them until the
     * database itself closed.
     */
    private CloseListener closeListener;

    /** Lets the owning database drop a cursor from its list once it closes. */
    /// Public because the SQLCipher-backed database lives in a sub-package: it is deleted at
    /// build time for applications that never encrypt, so it cannot sit alongside this class.
    public interface CloseListener {
        void cursorClosed(AndroidCursor cursor);
    }

    /// True when the statement behind this cursor changes the database, so moving off the window
    /// it already holds would run those changes again.
    private boolean statementWrites;

    public AndroidCursor(android.database.Cursor c) {
        this.c = c;
        this.last_read_column_index = -1;
    }

    /// Records that the statement behind this cursor writes.
    ///
    /// The platform cursor holds one window of rows and fills another by running its query a
    /// second time. For a SELECT that is a repeated read; for an INSERT, UPDATE or DELETE with
    /// RETURNING it is a repeated write, and an ordinary walk off the end of the window would do
    /// it without anybody asking. Rows outside the window are refused instead.
    ///
    /// #### Parameters
    ///
    /// - `writes`: true when running the statement changes the database
    ///
    /// Public for the same reason CloseListener is: the SQLCipher-backed database lives in a
    /// sub-package, because it is deleted at build time for applications that never encrypt.
    public void statementWrites(boolean writes) {
        statementWrites = writes;
    }

    /// Refuses a move that the platform would satisfy by running the statement again.
    ///
    /// Only for a statement that writes, and only for a position the current window does not
    /// hold: everything inside it is served from memory. A cursor that is not windowed answers no
    /// window at all, and nothing here can tell whether a move would refill it -- that is the
    /// SQLCipher build, whose cursor is the same AOSP class, so in practice the check applies
    /// there too.
    ///
    /// #### Parameters
    ///
    /// - `row`: the position being moved to
    ///
    /// #### Throws
    ///
    /// - `IOException`: if reaching that row would re-run a statement that writes
    private void requireInWindow(int row) throws IOException {
        if (!statementWrites || row < 0) {
            return;
        }
        if (c instanceof android.database.AbstractWindowedCursor) {
            android.database.CursorWindow window =
                    ((android.database.AbstractWindowedCursor) c).getWindow();
            if (window != null) {
                int start = window.getStartPosition();
                if (row >= start && row < start + window.getNumRows()) {
                    return;
                }
            }
        }
        throw new IOException("This cursor is over a statement that changes the database, and the "
                + "row asked for is outside the rows already read. Reaching it would run the "
                + "statement again and repeat those changes, so it is refused: read the rows this "
                + "cursor returns as it returns them, or run the statement with execute() and "
                + "query for what you need afterwards.");
    }

    public void setCloseListener(CloseListener listener) {
        this.closeListener = listener;
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
    public void invalidate() {
        if (closed) {
            return;
        }
        closed = true;
        // Close the native cursor as well, not just this wrapper. It holds the SQLiteQuery that
        // keeps a reference to the database, so leaving it open stops the database close from
        // releasing everything - and marking the wrapper closed first means a later close() from
        // the application is a no-op, so nothing else would ever release it.
        try {
            c.close();
        } catch (RuntimeException ignored) {
            // The database is going away regardless.
        }
    }

    @Override
    public boolean first() throws IOException {
        checkOpen();
        requireInWindow(0);
        last_read_column_index = -1;
        return c.moveToFirst();
    }

    @Override
    public boolean last() throws IOException {
        checkOpen();
        // The last row is only reachable through the window that holds it, and finding out which
        // one that is means counting -- which is a refill of its own on a result set larger than
        // a window. Refused outright for a statement that writes.
        if (statementWrites) {
            requireInWindow(Integer.MAX_VALUE);
        }
        last_read_column_index = -1;
        return c.moveToLast();
    }

    @Override
    public boolean next() throws IOException {
        checkOpen();
        requireInWindow(c.getPosition() + 1);
        last_read_column_index = -1;
        return c.moveToNext();
    }

    @Override
    public boolean prev() throws IOException {
        checkOpen();
        requireInWindow(c.getPosition() - 1);
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
        // Counting walks the whole result set, which past the first window means running the
        // statement again -- and repeating what it writes. The rows this cursor has already
        // returned are the ones it can answer for.
        if (statementWrites) {
            requireInWindow(Integer.MAX_VALUE);
        }
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
        checkColumn(columnIndex);
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
        requireInWindow(row);
        return c.moveToPosition(row);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (closeListener != null) {
            closeListener.cursorClosed(this);
        }
        c.close();
    }

    @Override
    public byte[] getBlob(int index) throws IOException {
        beginRead(index);
        return c.getBlob(index);
    }

    @Override
    public double getDouble(int index) throws IOException {
        beginRead(index);
        return c.getDouble(index);
    }

    @Override
    public float getFloat(int index) throws IOException {
        beginRead(index);
        return c.getFloat(index);
    }

    @Override
    public int getInteger(int index) throws IOException {
        beginRead(index);
        return c.getInt(index);
    }

    @Override
    public long getLong(int index) throws IOException {
        beginRead(index);
        return c.getLong(index);
    }

    @Override
    public short getShort(int index) throws IOException {
        beginRead(index);
        return c.getShort(index);
    }

    @Override
    public String getString(int index) throws IOException {
        beginRead(index);
        return c.getString(index);
    }

    public boolean isNull(int index) throws IOException {
        checkOpen();
        checkColumn(index);
        return c.isNull(index);
    }

    /// Guards a value read and records which column it was, for wasNull().
    private void beginRead(int index) throws IOException {
        checkOpen();
        checkOnARow();
        checkColumn(index);
        last_read_column_index = index;
    }

    /// Rejects a read taken while the cursor sits off a row.
    ///
    /// A Row handed out at a valid position stays usable after the cursor moves, and moving before
    /// the first row or past the last leaves the underlying cursor with no row to read. It answers
    /// that with an unchecked CursorIndexOutOfBoundsException, where the portable contract promises
    /// an IOException and every other port raises one.
    private void checkOnARow() throws IOException {
        int position = c.getPosition();
        int count = c.getCount();
        if (position < 0 || position >= count) {
            throw new IOException("This cursor is not on a row. Its position is " + position
                    + " and it has " + count + (count == 1 ? " row" : " rows"));
        }
    }

    /// Rejects a column index the result set does not have.
    ///
    /// The underlying cursor answers this with an unchecked CursorIndexOutOfBoundsException, which
    /// is not what the portable contract promises and not what the other ports raise.
    private void checkColumn(int index) throws IOException {
        int count = c.getColumnCount();
        if (index < 0 || index >= count) {
            throw new IOException("Column index " + index + " is out of range. This result set has "
                    + count + (count == 1 ? " column" : " columns"));
        }
    }

    @Override
    public boolean wasNull() throws IOException {
        // Checked first: without it a Row retained past the close of its cursor reached straight
        // into an already-closed native cursor below.
        checkOpen();
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
