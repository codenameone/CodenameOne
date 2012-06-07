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
package com.codename1.db;

import java.io.IOException;

/**
 * This Class represents a database row.
 * 
 * @author Chen
 */
public interface Row {

    /**
     * Gets column value by index.
     * 
     * @param index starts with zero
     * @return byte [] data
     * @throws IOException 
     */
    public byte[] getBlob(int index)throws IOException;

    /**
     * Gets column value by index.
     * 
     * @param index starts with zero
     * @return a double data from the database
     * @throws IOException 
     */
    public double getDouble(int index)throws IOException;

    /**
     * Gets column value by index.
     * 
     * @param index starts with zero
     * @return a float data from the database
     * @throws IOException 
     */
    public float getFloat(int index)throws IOException;

    /**
     * Gets column value by index.
     * 
     * @param index starts with zero
     * @return a int data from the database
     * @throws IOException 
     */
    public int getInteger(int index)throws IOException;

    /**
     * Gets column value by index.
     * 
     * @param index starts with zero
     * @return a long data from the database
     * @throws IOException 
     */
    public long getLong(int index)throws IOException;

    /**
     * Gets column value by index.
     * 
     * @param index starts with zero
     * @return a short data from the database
     * @throws IOException 
     */
    public short getShort(int index)throws IOException;

    /**
     * Gets column value by index.
     * 
     * @param index starts with zero
     * @return a String data from the database
     * @throws IOException 
     */
    public String getString(int index)throws IOException;
}
