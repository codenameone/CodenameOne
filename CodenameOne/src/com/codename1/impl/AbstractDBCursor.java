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
package com.codename1.impl;

import com.codename1.db.Cursor;
import com.codename1.db.CursorExt;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.db.RowExt;

import java.io.IOException;

/// Shared cursor implementation that gives every port identical navigation semantics.
///
/// Cursor behaviour used to be the single largest source of divergence in this API: some ports
/// counted rows from zero and some from one, some could seek backwards and some threw, and one
/// reported success on an empty result set. All of that logic now lives here, expressed in terms
/// of just two primitives a port must supply:
///
/// - `#rewind()` -- reposition before the first row
/// - `#stepForward()` -- advance one row, reporting whether a row was reached
///
/// Everything else -- `first`, `last`, `prev`, absolute positioning, the null latch behind
/// `RowExt#wasNull()`, off-a-row guards, idempotent close and case-insensitive column lookup --
/// is derived from those two and is therefore identical everywhere.
///
/// #### Cost of seeking
///
/// SQLite statements step forward only, so a backward or absolute seek is implemented by
/// rewinding and re-stepping. Forward iteration with `Cursor#next()` costs O(1) per row; a seek
/// costs O(distance from the start). This mirrors what Android's own windowed cursor does when a
/// requested row falls outside its window, so it is not a new hazard -- but it does mean a cursor
/// is a repeatable read only inside a transaction, because a concurrent write between the first
/// pass and the re-step can change what the second pass sees.
///
/// Ports whose underlying cursor is genuinely random access, such as Android's, do not need this
/// class.
public abstract class AbstractDBCursor implements Cursor, CursorExt, Row, RowExt {

    /// Position of the cursor, -1 before the first row, and the row count once exhausted.
    private int position = -1;

    /// Whether the underlying statement is currently sitting on a row. Distinct from the position
    /// because "before the first row" and "past the last row" are both off-a-row states.
    private boolean onRow;

    private boolean closed;

    /// Latches whether the most recent value read through this row was SQL NULL.
    private boolean lastReadWasNull;

    /// Whether any value has been read through this cursor yet, which only the compatibility hint
    /// cares about.
    private boolean anythingRead;

    /// Row count once known, or -1. Filled in by anything that walks to the end.
    private int knownCount = -1;

    /// Column count once asked for, or -1. A statement's column count does not change, and every
    /// value read checks its index against it, so asking the engine once keeps that check off the
    /// per-read path.
    private int knownColumnCount = -1;

    /// Repositions the underlying statement before its first row.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the underlying statement cannot be reset
    protected abstract void rewind() throws IOException;

    /// Advances the underlying statement by one row.
    ///
    /// #### Returns
    ///
    /// true if a row was reached, false at the end of the result set
    ///
    /// #### Throws
    ///
    /// - `IOException`: if stepping fails
    protected abstract boolean stepForward() throws IOException;

    /// Releases the underlying statement. Called at most once.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the statement cannot be released
    protected abstract void closeImpl() throws IOException;

    /// Returns the number of columns in the result set.
    protected abstract int columnCount() throws IOException;

    /// Returns the label of a column, honouring any `AS` alias.
    protected abstract String columnLabel(int columnIndex) throws IOException;

    /// Reports whether the value in the given column of the current row is SQL NULL.
    protected abstract boolean isNullAt(int index) throws IOException;

    protected abstract String readString(int index) throws IOException;

    protected abstract byte[] readBlob(int index) throws IOException;

    protected abstract double readDouble(int index) throws IOException;

    protected abstract long readLong(int index) throws IOException;

    /// Hook for legacy compatibility: when true, `#first()` rewinds without landing on a row and
    /// reports success unconditionally.
    ///
    /// Only the iOS port behaved this way, so only that port overrides this, and only while
    /// `Database#isLegacyBehavior()` is set.
    ///
    /// #### Returns
    ///
    /// false, meaning `#first()` moves onto the first row
    protected boolean isLegacyFirstRewind() {
        return false;
    }

    /// Hook for legacy compatibility: value added to the reported position.
    ///
    /// The simulator used to report positions counted from one, because it exposed the underlying
    /// JDBC row number directly. Only that port overrides this.
    ///
    /// #### Returns
    ///
    /// 0, meaning positions are reported from zero
    protected int legacyPositionOffset() {
        return 0;
    }

    /// Throws if the cursor has been closed.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if this cursor is closed
    protected void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("This cursor has been closed");
        }
    }

    /// Marks the cursor closed from the owning database, without touching the statement.
    ///
    /// Closing a database invalidates its cursors. The statement is owned by the connection and
    /// has already gone, so releasing it again would be a use-after-free on some engines.
    public void invalidate() {
        closed = true;
        onRow = false;
    }

    /// Whether the statement behind this cursor changes the database.
    ///
    /// Set by the port that created the cursor, since only it holds the SQL text.
    private boolean statementWrites;

    /// Records that the statement behind this cursor writes, so it is never re-executed.
    ///
    /// #### Parameters
    ///
    /// - `writes`: true when running the statement changes the database
    protected void noteStatementWrites(boolean writes) {
        statementWrites = writes;
    }

    /// Whether this cursor refuses to move backwards because its statement writes.
    ///
    /// #### Returns
    ///
    /// true when only forward movement is allowed
    protected boolean statementWrites() {
        return statementWrites;
    }

    private void rewindInternal() throws IOException {
        if (statementWrites) {
            // Rewinding re-executes the statement, which is fine for a query and a second set of
            // writes for anything else. An INSERT ... RETURNING reached through executeQuery does
            // its insert while the cursor is stepped, so a getCount() or a last() -- both of which
            // rewind -- would insert the rows again, and quietly: the caller asked how many rows
            // there were, not for them to be written twice. Refused instead, because there is no
            // answer to give that does not involve running the statement a second time.
            throw new IOException("This cursor is over a statement that changes the database, so "
                    + "it can only move forward: rewinding would run the statement again. Read "
                    + "the rows with next(), or run the statement with execute() and query for "
                    + "what you need afterwards.");
        }
        rewind();
        position = -1;
        onRow = false;
        lastReadWasNull = false;
    }

    /// True once the result set has been walked to the end and not rewound since.
    private boolean isExhausted() {
        return !onRow && knownCount >= 0 && position >= knownCount;
    }

    /// Records that the result set is exhausted, which also establishes the row count.
    private void markExhausted() {
        onRow = false;
        if (knownCount < 0) {
            knownCount = position + 1;
        }
        position = knownCount;
    }

    @Override
    public boolean next() throws IOException {
        checkOpen();
        if (isExhausted()) {
            // Do not step again. sqlite3_step after SQLITE_DONE resets the statement and
            // re-executes it, so on the iOS, Linux, Windows and JavaScript engines a second
            // next() past the end returns the first row again while this wrapper counts a
            // position beyond the row count. Only an explicit rewind clears this.
            return false;
        }
        if (stepForward()) {
            position++;
            onRow = true;
            return true;
        }
        markExhausted();
        return false;
    }

    @Override
    public boolean first() throws IOException {
        checkOpen();
        if (isLegacyFirstRewind()) {
            knownCount = -1;
            rewindInternal();
            return true;
        }
        return position(0);
    }

    @Override
    public boolean last() throws IOException {
        checkOpen();
        int total = getCount();
        if (total <= 0) {
            return false;
        }
        return position(total - 1);
    }

    @Override
    public boolean prev() throws IOException {
        checkOpen();
        if (position <= 0) {
            beforeFirst();
            return false;
        }
        return position(position - 1);
    }

    @Override
    public boolean position(int row) throws IOException {
        checkOpen();
        if (row < 0) {
            beforeFirst();
            return false;
        }
        return seek(row, true);
    }

    /// Moves to a row, optionally treating a rewind as the start of a new pass.
    ///
    /// A caller seeking backwards re-executes the statement, and the pass that follows can differ
    /// from the one before it, so the remembered count is dropped. `#getCount()` uses this with
    /// the flag off: its rewinds are how it counts and how it puts the cursor back, and dropping
    /// the count there would discard the one it had just taken.
    private boolean seek(int row, boolean newPass) throws IOException {
        if (onRow && position == row) {
            return true;
        }
        if (!onRow || row < position) {
            if (newPass) {
                knownCount = -1;
            }
            rewindInternal();
        }
        while (position < row) {
            if (!stepForward()) {
                markExhausted();
                return false;
            }
            position++;
            onRow = true;
        }
        return true;
    }

    @Override
    public void beforeFirst() throws IOException {
        checkOpen();
        // The count goes with the pass that established it. Rewinding re-executes the statement,
        // and outside a transaction the new pass can see rows another connection has written
        // since -- so a remembered count would answer for a result set that no longer exists, and
        // last() would stop on what used to be the final row.
        //
        // Only here, and not in the internal rewinds: getCount() and position() rewind as part of
        // counting and seeking, and clearing it there would discard the count they just took.
        knownCount = -1;
        rewindInternal();
    }

    @Override
    public int getCount() throws IOException {
        checkOpen();
        if (knownCount >= 0) {
            return knownCount;
        }
        int restoreTo = position;
        boolean wasOnRow = onRow;

        rewindInternal();
        int total = 0;
        while (stepForward()) {
            total++;
        }
        knownCount = total;
        onRow = false;
        position = total;

        if (wasOnRow) {
            seek(restoreTo, false);
        } else if (restoreTo < 0) {
            rewindInternal();
        }
        return knownCount;
    }

    @Override
    public int getPosition() throws IOException {
        checkOpen();
        return position + legacyPositionOffset();
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
    public int getColumnCount() throws IOException {
        checkOpen();
        return columnCount();
    }

    @Override
    public String getColumnName(int columnIndex) throws IOException {
        checkOpen();
        checkColumn(columnIndex);
        return columnLabel(columnIndex);
    }

    @Override
    public int getColumnIndex(String columnName) throws IOException {
        checkOpen();
        int count = columnCount();
        for (int iter = 0; iter < count; iter++) {
            String name = columnLabel(iter);
            if (name != null && name.equalsIgnoreCase(columnName)) {
                return iter;
            }
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        onRow = false;
        closeImpl();
    }

    @Override
    public boolean wasNull() throws IOException {
        // The only shared cursor operation that used to skip this, so a Row retained past the
        // close of its cursor or its database kept answering from a stale latch instead of
        // reporting that it is gone.
        checkOpen();
        if (!anythingRead && Database.isLegacyBehavior() && legacyWasNullBeforeAnyRead()) {
            return true;
        }
        return lastReadWasNull;
    }

    /// Whether this port used to answer `wasNull()` with true before anything had been read.
    ///
    /// It is a per-port question, which is why it is asked here rather than decided here: the
    /// answer is what that port actually did, and only a port that did it restores it under the
    /// compatibility hint. The default is the contract, which says a value nobody has read is not
    /// a null value.
    ///
    /// #### Returns
    ///
    /// true if the compatibility hint should make `wasNull()` true before the first read
    protected boolean legacyWasNullBeforeAnyRead() {
        return false;
    }

    /// Guards a value read and updates the null latch.
    private void beginRead(int index) throws IOException {
        checkOpen();
        if (!onRow) {
            throw new IOException("The cursor is not on a row");
        }
        checkColumn(index);
        anythingRead = true;
        lastReadWasNull = isNullAt(index);
    }

    /// Rejects a column index the result set does not have.
    ///
    /// The SQLite C API answers an out-of-range index with SQL NULL and a status code the callers
    /// of a `sqlite3_column_*` function have no way to see, so without this a mistyped index reads
    /// as a real null and the caller gets null or zero back. That is indistinguishable from data,
    /// which is the worst of the three possible outcomes; the other engines behind this API report
    /// it, so it is reported here for all of them.
    private void checkColumn(int index) throws IOException {
        if (knownColumnCount < 0) {
            knownColumnCount = columnCount();
        }
        int count = knownColumnCount;
        if (index < 0 || index >= count) {
            throw new IOException("Column index " + index + " is out of range. This result set has "
                    + count + (count == 1 ? " column" : " columns"));
        }
    }

    @Override
    public String getString(int index) throws IOException {
        beginRead(index);
        if (lastReadWasNull) {
            return null;
        }
        return readString(index);
    }

    @Override
    public byte[] getBlob(int index) throws IOException {
        beginRead(index);
        if (lastReadWasNull) {
            return null;
        }
        return readBlob(index);
    }

    @Override
    public double getDouble(int index) throws IOException {
        beginRead(index);
        if (lastReadWasNull) {
            return 0;
        }
        return readDouble(index);
    }

    @Override
    public float getFloat(int index) throws IOException {
        beginRead(index);
        if (lastReadWasNull) {
            return 0;
        }
        return (float) readDouble(index);
    }

    @Override
    public long getLong(int index) throws IOException {
        beginRead(index);
        if (lastReadWasNull) {
            return 0;
        }
        return readLong(index);
    }

    @Override
    public int getInteger(int index) throws IOException {
        beginRead(index);
        if (lastReadWasNull) {
            return 0;
        }
        return (int) readLong(index);
    }

    @Override
    public short getShort(int index) throws IOException {
        beginRead(index);
        if (lastReadWasNull) {
            return 0;
        }
        return (short) readLong(index);
    }

    /// Convenience for ports that need to know whether legacy mode is active.
    ///
    /// #### Returns
    ///
    /// true when the pre-normalization behaviour is in effect
    protected static boolean legacyBehavior() {
        return Database.isLegacyBehavior();
    }
}
