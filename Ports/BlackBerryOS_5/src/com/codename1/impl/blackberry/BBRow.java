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

import com.codename1.db.Row;
import java.io.IOException;
import net.rim.device.api.database.DataTypeException;

/**
 *
 * @author Chen
 */
public class BBRow implements Row{
    
    private net.rim.device.api.database.Row row;
    
    public BBRow(net.rim.device.api.database.Row row){
        this.row = row;
    }
    
    public byte[] getBlob(int index) throws IOException{
        try {
            return row.getBlobBytes(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public boolean getBoolean(int index) throws IOException{
        try {
            return row.getBoolean(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public byte getByte(int index) throws IOException{
        try {
            return row.getByte(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public double getDouble(int index) throws IOException{
        try {
            return row.getDouble(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public float getFloat(int index) throws IOException{
        try {
            return row.getFloat(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public int getInteger(int index) throws IOException{
        try {
            return row.getInteger(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public long getLong(int index) throws IOException{
        try {
            return row.getLong(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public Object getObject(int index) throws IOException{
        try {
            return row.getObject(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public short getShort(int index) throws IOException{
        try {
            return row.getShort(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public String getString(int index) throws IOException{
        try {
            return row.getString(index);
        } catch (DataTypeException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
}
