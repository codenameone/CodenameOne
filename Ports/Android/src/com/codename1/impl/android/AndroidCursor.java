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
import com.codename1.db.Row;
import java.io.IOException;

/**
 *
 * @author Chen
 */
public class AndroidCursor implements Cursor, Row{
    
    private android.database.Cursor c;
    
    public AndroidCursor(android.database.Cursor c) {
        this.c = c;
    }
    
    @Override
    public boolean first() throws IOException {
        return c.moveToFirst();
    }

    @Override
    public boolean last() throws IOException {
        return c.moveToLast();
    }

    @Override
    public boolean next() throws IOException {
        return c.moveToNext();
    }

    @Override
    public boolean prev() throws IOException {
        return c.moveToPrevious();
    }

    @Override
    public int getColumnIndex(String columnName) throws IOException {
        return c.getColumnIndex(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) throws IOException {
        return c.getColumnName(columnIndex);
    }

    @Override
    public int getPosition() throws IOException {
        return c.getPosition();
    }

    @Override
    public Row getRow() throws IOException {
        return this;
    }

    @Override
    public boolean position(int row) throws IOException {
        return c.moveToPosition(row);
    }

    @Override
    public void close() throws IOException {
        c.close();
    }

    @Override
    public byte[] getBlob(int index) throws IOException {
        return c.getBlob(index);
    }

    @Override
    public double getDouble(int index) throws IOException {
        return c.getDouble(index);
    }

    @Override
    public float getFloat(int index) throws IOException {
        return c.getFloat(index);
    }

    @Override
    public int getInteger(int index) throws IOException {
        return c.getInt(index);
    }

    @Override
    public long getLong(int index) throws IOException {
        return c.getLong(index);
    }

    @Override
    public short getShort(int index) throws IOException {
        return c.getShort(index);
    }

    @Override
    public String getString(int index) throws IOException {
        return c.getString(index);
    }
    
}
