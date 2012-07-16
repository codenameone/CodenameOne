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
package com.codename1.impl.blackberry;

import com.codename1.db.Cursor;
import com.codename1.db.Row;
import java.io.IOException;
import net.rim.device.api.database.DatabaseException;

/**
 *
 * @author Chen
 */
public class BBCursor implements Cursor{

    private net.rim.device.api.database.Cursor cursor;
    
    public BBCursor(net.rim.device.api.database.Cursor cursor) {
        this.cursor = cursor;
    }
    
    
    public boolean first() throws IOException{
        try {
            return cursor.first();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public boolean last() throws IOException{
        try {
            return cursor.last();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public boolean next() throws IOException{
        try {
            return cursor.next();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public boolean prev() throws IOException{
        try {
            return cursor.prev();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public int getColumnIndex(String columnName) throws IOException{
        try {
            return cursor.getColumnIndex(columnName);
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public String getColumnName(int columnIndex) throws IOException{
        try {
            return cursor.getColumnName(columnIndex);
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public int getPosition() throws IOException{
        try {
            return cursor.getPosition();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public Row getRow() throws IOException{
        try {
            net.rim.device.api.database.Row row = cursor.getRow();
            BBRow r = new BBRow(row);
            return r;
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public boolean position(int row) throws IOException{
        try {
            return cursor.position(row);
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void close() throws IOException{
        try {
            cursor.close();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
        
    }

    public int getColumnCount() throws IOException {
        net.rim.device.api.database.Row row;
        try {
            row = cursor.getRow();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
        return row.getColumnNames().length;
    }
    
}
