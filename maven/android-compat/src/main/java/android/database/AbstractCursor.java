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
package android.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

/// The cursor bookkeeping shared by every implementation: position and
/// movement, column lookup, observers and closing. A subclass supplies the
/// row count, the column names and the typed getters for the current row.
public abstract class AbstractCursor implements Cursor {

    protected int mPos = -1;
    protected boolean mClosed;
    @Deprecated
    protected ContentResolver mContentResolver;

    private Uri mNotifyUri;
    private final ContentObservable mContentObservable = new ContentObservable();
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private Bundle mExtras = Bundle.EMPTY;

    @Override
    public abstract int getCount();

    @Override
    public abstract String[] getColumnNames();

    @Override
    public abstract String getString(int column);

    @Override
    public abstract short getShort(int column);

    @Override
    public abstract int getInt(int column);

    @Override
    public abstract long getLong(int column);

    @Override
    public abstract float getFloat(int column);

    @Override
    public abstract double getDouble(int column);

    @Override
    public abstract boolean isNull(int column);

    @Override
    public int getType(int column) {
        return FIELD_TYPE_STRING;
    }

    @Override
    public byte[] getBlob(int column) {
        throw new UnsupportedOperationException("getBlob is not supported");
    }

    @Override
    public int getColumnCount() {
        return getColumnNames().length;
    }

    @Override
    @Deprecated
    public void deactivate() {
        onDeactivateOrClose();
    }

    protected void onDeactivateOrClose() {
        mDataSetObservable.notifyInvalidated();
    }

    @Override
    @Deprecated
    public boolean requery() {
        mDataSetObservable.notifyChanged();
        return true;
    }

    @Override
    public boolean isClosed() {
        return mClosed;
    }

    @Override
    public void close() {
        mClosed = true;
        mContentObservable.unregisterAll();
        onDeactivateOrClose();
    }

    /// Called when the position changes, before it is committed; returning
    /// false leaves the cursor before the first row.
    public boolean onMove(int oldPosition, int newPosition) {
        return true;
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        String result = getString(columnIndex);
        if (result != null) {
            char[] data = buffer.data;
            if (data == null || data.length < result.length()) {
                buffer.data = result.toCharArray();
            } else {
                result.getChars(0, result.length(), data, 0);
            }
            buffer.sizeCopied = result.length();
        } else {
            buffer.sizeCopied = 0;
        }
    }

    @Override
    public final int getPosition() {
        return mPos;
    }

    @Override
    public final boolean moveToPosition(int position) {
        final int count = getCount();
        if (position >= count) {
            mPos = count;
            return false;
        }
        if (position < 0) {
            mPos = -1;
            return false;
        }
        if (position == mPos) {
            return true;
        }
        boolean result = onMove(mPos, position);
        if (!result) {
            mPos = -1;
        } else {
            mPos = position;
        }
        return result;
    }

    @Override
    public final boolean move(int offset) {
        return moveToPosition(mPos + offset);
    }

    @Override
    public final boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public final boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    @Override
    public final boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }

    @Override
    public final boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }

    @Override
    public final boolean isFirst() {
        return mPos == 0 && getCount() != 0;
    }

    @Override
    public final boolean isLast() {
        int cnt = getCount();
        return mPos == (cnt - 1) && cnt != 0;
    }

    // An empty cursor is both before the first row and after the last one:
    // AOSP's AbstractCursor answers true from both predicates when getCount()
    // is 0, and code written against Android (`while (!c.isAfterLast())`)
    // relies on it. Deliberately not changed to "neither".
    @Override
    public final boolean isBeforeFirst() {
        return getCount() == 0 || mPos == -1;
    }

    @Override
    public final boolean isAfterLast() {
        return getCount() == 0 || mPos == getCount();
    }

    /// Case-insensitive, and a `table.column` name is looked up by its
    /// column part, as on Android.
    @Override
    public int getColumnIndex(String columnName) {
        final int periodIndex = columnName.lastIndexOf('.');
        if (periodIndex != -1) {
            columnName = columnName.substring(periodIndex + 1);
        }
        String[] columnNames = getColumnNames();
        int length = columnNames.length;
        for (int i = 0; i < length; i++) {
            if (columnNames[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) {
        final int index = getColumnIndex(columnName);
        if (index < 0) {
            throw new IllegalArgumentException("column '" + columnName + "' does not exist");
        }
        return index;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumnNames()[columnIndex];
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mContentObservable.registerObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        if (!mClosed) {
            mContentObservable.unregisterObserver(observer);
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /// Tells the content observers the data changed.
    protected void onChange(boolean selfChange) {
        mContentObservable.dispatchChange(selfChange, null);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri notifyUri) {
        mContentResolver = cr;
        mNotifyUri = notifyUri;
    }

    @Override
    public Uri getNotificationUri() {
        return mNotifyUri;
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return false;
    }

    @Override
    public void setExtras(Bundle extras) {
        mExtras = extras == null ? Bundle.EMPTY : extras;
    }

    @Override
    public Bundle getExtras() {
        return mExtras;
    }

    @Override
    public Bundle respond(Bundle extras) {
        return Bundle.EMPTY;
    }

    /// Throws unless the cursor is on a row.
    protected void checkPosition() {
        if (-1 == mPos || getCount() == mPos) {
            throw new CursorIndexOutOfBoundsException(mPos, getCount());
        }
    }
}
