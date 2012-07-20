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
package com.codename1.impl.javase;

import com.codename1.db.Cursor;
import com.codename1.db.Row;
import com.codename1.io.Util;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen
 */
public class SECursor implements Cursor, Row{
    
    private ResultSet resultSet;
    
    public SECursor(ResultSet resultSet) {
        this.resultSet = resultSet;
    }
    
    
    @Override
    public boolean first() throws IOException {
        try {
            return resultSet.first();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public boolean last() throws IOException {
        try {
            return resultSet.last();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public boolean next() throws IOException {
        try {
            return resultSet.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public boolean prev() throws IOException {
        try {
            return resultSet.previous();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public int getColumnIndex(String columnName) throws IOException {
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            int colsCount = meta.getColumnCount();
            for (int i = 0; i < colsCount; i++) {
                String c = meta.getColumnLabel(i+1);                
                if(c.equalsIgnoreCase(columnName)){
                    return i;
                }
            }
            return -1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public String getColumnName(int columnIndex) throws IOException {
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            return meta.getColumnName(columnIndex + 1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public int getPosition() throws IOException {
        try {
            return resultSet.getRow();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public Row getRow() throws IOException {
        return this;
    }

    public boolean position(int row) throws IOException {
        try {
            return resultSet.absolute(row);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void close() throws IOException {
        try {
            resultSet.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public byte[] getBlob(int index) throws IOException {
        try {
            Blob b = resultSet.getBlob(index+1);
            InputStream is = b.getBinaryStream();
            return Util.readInputStream(is);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }

    }

    public double getDouble(int index) throws IOException {
        try {
            return resultSet.getDouble(index+1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public float getFloat(int index) throws IOException {
        try {
            return resultSet.getFloat(index+1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public int getInteger(int index) throws IOException {
        try {
            return resultSet.getInt(index+1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public long getLong(int index) throws IOException {
        try {
            return resultSet.getLong(index+1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public short getShort(int index) throws IOException {
        try {
            return resultSet.getShort(index+1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public String getString(int index) throws IOException {
        try {
            return resultSet.getString(index+1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public int getColumnCount() throws IOException {
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            return meta.getColumnCount();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
}
