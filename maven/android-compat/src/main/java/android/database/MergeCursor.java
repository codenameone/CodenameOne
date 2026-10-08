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

/// Presents several cursors one after the other as a single cursor.
public class MergeCursor extends AbstractCursor {

    private final DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            mPos = -1;
        }

        @Override
        public void onInvalidated() {
            mPos = -1;
        }
    };

    private Cursor mCursor;
    private final Cursor[] mCursors;

    public MergeCursor(Cursor[] cursors) {
        mCursors = cursors;
        mCursor = cursors.length > 0 ? cursors[0] : null;
        for (int i = 0; i < mCursors.length; i++) {
            if (mCursors[i] == null) {
                continue;
            }
            mCursors[i].registerDataSetObserver(mObserver);
        }
    }

    @Override
    public int getCount() {
        int count = 0;
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] != null) {
                count += mCursors[i].getCount();
            }
        }
        return count;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        mCursor = null;
        int cursorStartPos = 0;
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] == null) {
                continue;
            }
            if (newPosition < (cursorStartPos + mCursors[i].getCount())) {
                mCursor = mCursors[i];
                break;
            }
            cursorStartPos += mCursors[i].getCount();
        }
        if (mCursor != null) {
            return mCursor.moveToPosition(newPosition - cursorStartPos);
        }
        return false;
    }

    @Override
    public String getString(int column) {
        return mCursor.getString(column);
    }

    @Override
    public short getShort(int column) {
        return mCursor.getShort(column);
    }

    @Override
    public int getInt(int column) {
        return mCursor.getInt(column);
    }

    @Override
    public long getLong(int column) {
        return mCursor.getLong(column);
    }

    @Override
    public float getFloat(int column) {
        return mCursor.getFloat(column);
    }

    @Override
    public double getDouble(int column) {
        return mCursor.getDouble(column);
    }

    @Override
    public int getType(int column) {
        return mCursor.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return mCursor.isNull(column);
    }

    @Override
    public byte[] getBlob(int column) {
        return mCursor.getBlob(column);
    }

    @Override
    public String[] getColumnNames() {
        if (mCursor != null) {
            return mCursor.getColumnNames();
        }
        return new String[0];
    }

    @Override
    @Deprecated
    public void deactivate() {
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] != null) {
                mCursors[i].deactivate();
            }
        }
        super.deactivate();
    }

    @Override
    public void close() {
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] == null) {
                continue;
            }
            mCursors[i].close();
        }
        super.close();
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] != null) {
                mCursors[i].registerContentObserver(observer);
            }
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] != null) {
                mCursors[i].unregisterContentObserver(observer);
            }
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] != null) {
                mCursors[i].registerDataSetObserver(observer);
            }
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] != null) {
                mCursors[i].unregisterDataSetObserver(observer);
            }
        }
    }

    @Override
    @Deprecated
    public boolean requery() {
        int length = mCursors.length;
        for (int i = 0; i < length; i++) {
            if (mCursors[i] == null) {
                continue;
            }
            if (!mCursors[i].requery()) {
                return false;
            }
        }
        return true;
    }
}
