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

/// Forwards every call to a wrapped cursor; subclass it to override some.
public class CursorWrapper implements Cursor {

    protected final Cursor mCursor;

    public CursorWrapper(Cursor cursor) {
        mCursor = cursor;
    }

    public Cursor getWrappedCursor() {
        return mCursor;
    }

    @Override
    public void close() {
        mCursor.close();
    }

    @Override
    public boolean isClosed() {
        return mCursor.isClosed();
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    @Deprecated
    public void deactivate() {
        mCursor.deactivate();
    }

    @Override
    public boolean moveToFirst() {
        return mCursor.moveToFirst();
    }

    @Override
    public int getColumnCount() {
        return mCursor.getColumnCount();
    }

    @Override
    public int getColumnIndex(String columnName) {
        return mCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) {
        return mCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return mCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return mCursor.getColumnNames();
    }

    @Override
    public double getDouble(int columnIndex) {
        return mCursor.getDouble(columnIndex);
    }

    @Override
    public void setExtras(Bundle extras) {
        mCursor.setExtras(extras);
    }

    @Override
    public Bundle getExtras() {
        return mCursor.getExtras();
    }

    @Override
    public float getFloat(int columnIndex) {
        return mCursor.getFloat(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return mCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        return mCursor.getLong(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) {
        return mCursor.getShort(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        return mCursor.getString(columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        mCursor.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return mCursor.getBlob(columnIndex);
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return mCursor.getWantsAllOnMoveCalls();
    }

    @Override
    public boolean isAfterLast() {
        return mCursor.isAfterLast();
    }

    @Override
    public boolean isBeforeFirst() {
        return mCursor.isBeforeFirst();
    }

    @Override
    public boolean isFirst() {
        return mCursor.isFirst();
    }

    @Override
    public boolean isLast() {
        return mCursor.isLast();
    }

    @Override
    public int getType(int columnIndex) {
        return mCursor.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        return mCursor.isNull(columnIndex);
    }

    @Override
    public boolean moveToLast() {
        return mCursor.moveToLast();
    }

    @Override
    public boolean move(int offset) {
        return mCursor.move(offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        return mCursor.moveToPosition(position);
    }

    @Override
    public boolean moveToNext() {
        return mCursor.moveToNext();
    }

    @Override
    public int getPosition() {
        return mCursor.getPosition();
    }

    @Override
    public boolean moveToPrevious() {
        return mCursor.moveToPrevious();
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mCursor.registerDataSetObserver(observer);
    }

    @Override
    @Deprecated
    public boolean requery() {
        return mCursor.requery();
    }

    @Override
    public Bundle respond(Bundle extras) {
        return mCursor.respond(extras);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        mCursor.setNotificationUri(cr, uri);
    }

    @Override
    public Uri getNotificationUri() {
        return mCursor.getNotificationUri();
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mCursor.unregisterDataSetObserver(observer);
    }
}
