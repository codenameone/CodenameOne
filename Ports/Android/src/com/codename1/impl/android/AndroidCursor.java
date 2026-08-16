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

    // The invariant this class keeps, written once so the next change to it has something to
    // check against: a statement runs exactly once per cursor. The platform runs it when data is
    // first asked for and counts the whole result set as it fills its first window; a position
    // outside that window is served by running the statement AGAIN, which for a query is a
    // repeated read and for an INSERT, UPDATE or DELETE with RETURNING is a repeated write. So
    // for a statement that writes, every move that would leave the window is refused, and
    // nothing on the way in -- validation included -- touches the cursor at all, because every
    // move asks getCount() first and that is the walk this is trying not to do.
    //
    // Deliberately not fixed here: the first access counting the whole result set. That is the
    // platform's own behaviour, identical for an application using android.database directly,
    // and there is no public API that fills a window without it.

    /// The moves this wrapper makes, for the one place that makes them.
    private static final int FIRST = 0;
    private static final int LAST = 1;
    private static final int NEXT = 2;
    private static final int PREVIOUS = 3;
    private static final int ABSOLUTE = 4;

    /// Moves the platform cursor and reports a failure the way this API promises to.
    ///
    /// The query runs when the cursor is first stepped, not when executeQuery returns -- so a
    /// statement that compiles and then fails, a constraint violation reached through
    /// executeQuery being the plain case, reports itself from here. The platform raises that as
    /// an unchecked SQLiteException, and every port in this API answers with an IOException, so
    /// the caller can catch one thing rather than a different unchecked type per platform.
    ///
    /// #### Parameters
    ///
    /// - `move`: which move to make
    /// - `row`: the target row, for an absolute move
    ///
    /// #### Returns
    ///
    /// true if the cursor landed on a row
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the engine reports a failure
    private boolean moved(int move, int row) throws IOException {
        try {
            switch (move) {
                case FIRST:
                    return c.moveToFirst();
                case LAST:
                    return c.moveToLast();
                case NEXT:
                    return c.moveToNext();
                case PREVIOUS:
                    return c.moveToPrevious();
                default:
                    return c.moveToPosition(row);
            }
        } catch (RuntimeException failed) {
            throw new IOException(failed.getMessage(), failed);
        }
    }

    /// Asks the platform cursor for its row count, reporting a failure as this API promises to.
    ///
    /// The same reason the moves do: asked before anything has been read, this is what runs the
    /// statement, and the engine reports a failure from it as an unchecked SQLiteException.
    ///
    /// #### Returns
    ///
    /// the number of rows
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the engine reports a failure
    private int counted() throws IOException {
        try {
            return c.getCount();
        } catch (RuntimeException failed) {
            throw new IOException(failed.getMessage(), failed);
        }
    }

    /// Whether the last move landed on a row.
    ///
    /// Tracked here rather than asked of the platform cursor, which answers that question by
    /// counting -- and counting past the first window refills it by running the statement again.
    /// For a statement that writes, that is a repeated write triggered by reading a value.
    private boolean onRow;

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

    /// Whether the platform cursor already holds this row in memory.
    ///
    /// #### Parameters
    ///
    /// - `row`: the position being asked about
    ///
    /// #### Returns
    ///
    /// true if reading that row needs no work from the statement
    private boolean inWindow(int row) {
        if (!(c instanceof android.database.AbstractWindowedCursor)) {
            // A cursor that is not windowed answers no window at all, and nothing here can tell
            // whether a move would refill it.
            return false;
        }
        android.database.CursorWindow window =
                ((android.database.AbstractWindowedCursor) c).getWindow();
        if (window == null) {
            return false;
        }
        int start = window.getStartPosition();
        return row >= start && row < start + window.getNumRows();
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
        if (inWindow(row)) {
            return;
        }
        // Past the end is not a refill. The platform answers a position at or beyond the row
        // count from the count it already holds, without touching the statement -- and it holds
        // one, because the first fill counts the whole result set. This is the move that ends
        // every while (next()) loop, and refusing it made the loop throw instead of finishing,
        // for a single row statement as much as for one that returned none.
        if (row >= counted()) {
            return;
        }
        // Asked again, because the count above is what runs the statement the first time and
        // fills the first window with it. A fresh cursor has no window at all, so the first move
        // onto row zero reaches here having just brought row zero into memory, and refusing it
        // then reported a failure for a statement that had already run -- inviting a retry that
        // would run it a second time. This is not a second execution: the window is inspected,
        // not refilled.
        if (inWindow(row)) {
            return;
        }
        // Says that the changes are done, which is the part a caller has to know. Everything
        // else here is recoverable by reading differently; a retry is not, because the insert or
        // the update has already been applied and doing it again applies it twice. The platform
        // cannot offer anything better: the rows past this point exist only in a window that has
        // to be refilled, and refilling is re-running.
        throw new IOException("This cursor is over a statement that changes the database. Those "
                + "changes have already been applied and must not be repeated -- do not retry "
                + "this statement. The rows past the first window cannot be reached, because "
                + "reaching them would run it again. Read the rows this cursor returns as it "
                + "returns them, or run the statement with execute() and query for what you need "
                + "afterwards.");
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
        onRow = moved(FIRST, 0);
        return onRow;
    }

    @Override
    public boolean last() throws IOException {
        checkOpen();
        // The last row of a writing statement is only reachable through the window that holds
        // it, which is this one or none.
        if (statementWrites) {
            requireAnsweredWithoutRunning("last()");
            requireInWindow(counted() - 1);
        }
        last_read_column_index = -1;
        onRow = moved(LAST, 0);
        return onRow;
    }

    @Override
    public boolean next() throws IOException {
        checkOpen();
        requireInWindow(c.getPosition() + 1);
        last_read_column_index = -1;
        onRow = moved(NEXT, 0);
        return onRow;
    }

    @Override
    public boolean prev() throws IOException {
        checkOpen();
        if (beforeTheFirstRow()) {
            // There is no row behind the first one, and the platform reaches that answer by
            // counting the rows, which runs the statement.
            last_read_column_index = -1;
            return false;
        }
        requireInWindow(c.getPosition() - 1);
        last_read_column_index = -1;
        onRow = moved(PREVIOUS, 0);
        return onRow;
    }

    @Override
    public void beforeFirst() throws IOException {
        checkOpen();
        last_read_column_index = -1;
        onRow = false;
        if (beforeTheFirstRow()) {
            // Already there, and asking the platform to go there anyway would run the statement.
            return;
        }
        moved(ABSOLUTE, -1);
    }

    @Override
    public int getCount() throws IOException {
        checkOpen();
        // Free after the first data access: the count is established by the first fill and held
        // from then on. Asked before it, this is what runs the statement -- once, which is what
        // the platform would do on the first move anyway -- so it reports a failure the way the
        // moves do.
        return counted();
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
        if (!onRow) {
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
            // A rewind, and the same one beforeFirst() performs, including its reason for
            // sometimes performing nothing at all.
            beforeFirst();
            return false;
        }
        if (row > 0) {
            // Row zero is the exception: the window a run fills begins there, so it is the one
            // position that can be promised before the statement has run.
            requireAnsweredWithoutRunning("position(" + row + ")");
        }
        requireInWindow(row);
        onRow = moved(ABSOLUTE, row);
        return onRow;
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
        try {
            c.close();
        } catch (RuntimeException failed) {
            throw new IOException(failed.getMessage(), failed);
        }
    }

    @Override
    public byte[] getBlob(int index) throws IOException {
        beginRead(index);
        try {
            return c.getBlob(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    @Override
    public double getDouble(int index) throws IOException {
        beginRead(index);
        try {
            return c.getDouble(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    @Override
    public float getFloat(int index) throws IOException {
        beginRead(index);
        try {
            return c.getFloat(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    @Override
    public int getInteger(int index) throws IOException {
        beginRead(index);
        try {
            return c.getInt(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    @Override
    public long getLong(int index) throws IOException {
        beginRead(index);
        try {
            return c.getLong(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    @Override
    public short getShort(int index) throws IOException {
        beginRead(index);
        try {
            return c.getShort(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    @Override
    public String getString(int index) throws IOException {
        beginRead(index);
        try {
            return c.getString(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    public boolean isNull(int index) throws IOException {
        checkOpen();
        checkColumn(index);
        try {
            return c.isNull(index);
        } catch (RuntimeException failed) {
            throw readFailed(index, failed);
        }
    }

    /// Whether the statement behind this cursor has been run.
    ///
    /// Answered from whether the platform holds a window, which it fills by running the statement
    /// and not before. It is the difference between a seek that can be decided from rows already
    /// in memory and one that would have to run the statement to find out.
    ///
    /// A cursor that is not windowed answers no window ever, so this reports not-run and the
    /// callers refuse rather than risk it. That is the safe direction: the cursors this class
    /// wraps are the platform's own, and a refusal costs a query where a wrong answer costs a
    /// repeated write.
    ///
    /// #### Returns
    ///
    /// true if the statement has already been run
    private boolean statementHasRun() {
        if (!(c instanceof android.database.AbstractWindowedCursor)) {
            return false;
        }
        return ((android.database.AbstractWindowedCursor) c).getWindow() != null;
    }

    /// Refuses a seek that could only be answered by running a statement that writes.
    ///
    /// The window a writing statement is read through is the one its single run fills, and that
    /// window starts at row zero. So on a cursor that has not run yet, row zero is the only
    /// position that can be promised; whether any other is reachable is not knowable without
    /// running the statement -- which performs the insert or the update.
    ///
    /// Refusing here, before anything runs, is what makes the answer usable. Running first and
    /// refusing afterwards left the write committed behind a failure, so the caller could not
    /// tell whether it had happened and a retry would do it again. Either the statement runs and
    /// the rows are read forward, or nothing runs at all.
    ///
    /// #### Parameters
    ///
    /// - `what`: the seek being attempted, for the message
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the statement would have to run to answer this
    private void requireAnsweredWithoutRunning(String what) throws IOException {
        if (!statementWrites || statementHasRun()) {
            return;
        }
        throw new IOException(what + " cannot be answered on a cursor over a statement that "
                + "changes the database, because the statement has not run yet and running it to "
                + "find out would perform those changes and then fail -- leaving you unable to "
                + "tell whether they happened. Nothing has been run. Read the rows forward with "
                + "next(), or run the statement with execute() and query for what you need "
                + "afterwards.");
    }

    /// Whether this cursor still sits where it was created, before the first row.
    ///
    /// Worth asking before every backward move, because moving there is not free. The platform
    /// answers a move by position and gets the row count first -- before it looks at whether the
    /// target is negative -- and getting the count is what runs the statement. So rewinding a
    /// cursor that has never been touched runs it, and for a statement that writes, that is the
    /// insert or the update happening because somebody rewound.
    ///
    /// Read from the platform's own position rather than a flag of our own: it is a field there,
    /// answered without touching the statement, and it is the same number the moves act on.
    ///
    /// #### Returns
    ///
    /// true if the cursor is before the first row and has read nothing
    private boolean beforeTheFirstRow() {
        return !onRow && c.getPosition() < 0;
    }

    /// Reports a failed value read as this API promises to.
    ///
    /// A read is not always only a read. The platform cursor holds a window of rows rather than
    /// the whole result set, and reading at a row outside that window refills it by running the
    /// statement again, so an engine failure can surface from a getter and not only from a move. A
    /// row too wide for the window fails here too, as the SQLiteBlobTooBigException a large blob
    /// raises. Both arrive unchecked, where this API promises an IOException and every other port
    /// raises one.
    ///
    /// Deliberately not applied to the metadata calls -- getPosition, getColumnCount,
    /// getColumnIndex and getColumnName. Those are answered from the prepared statement and from
    /// the cursor's own fields with no engine work behind them, and checkOpen has already rejected
    /// the one state that makes them throw, so wrapping them would only claim a failure that
    /// cannot arrive.
    ///
    /// #### Params
    ///
    /// - `index`: the column being read
    /// - `failed`: what the platform raised
    ///
    /// #### Returns
    ///
    /// the exception to throw
    private static IOException readFailed(int index, RuntimeException failed) {
        return new IOException("Reading column " + index + " failed: " + failed.getMessage(),
                failed);
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
        if (!onRow) {
            throw new IOException("This cursor is not on a row. Its position is "
                    + c.getPosition() + "; move onto a row and check that the move returned true "
                    + "before reading a value.");
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
        try {
            return c.isNull(last_read_column_index);
        } catch (RuntimeException failed) {
            throw readFailed(last_read_column_index, failed);
        }
    }

    @Override
    public int getColumnCount() throws IOException {
        checkOpen();
        return c.getColumnCount();
    }
}
