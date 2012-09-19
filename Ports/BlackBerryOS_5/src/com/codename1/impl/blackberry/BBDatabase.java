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
import com.codename1.db.Database;
import java.io.IOException;
import net.rim.device.api.database.DatabaseException;
import net.rim.device.api.database.DatabaseIOException;

/**
 *
 * @author Chen
 */
public class BBDatabase extends Database{
    
    private net.rim.device.api.database.Database db;
    
    public BBDatabase(net.rim.device.api.database.Database db) {
        this.db = db;
    }
    
    public void beginTransaction() throws IOException{
        try {
            db.beginTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void commitTransaction() throws IOException{
        try {
            db.commitTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void close() throws IOException{
        try {
            db.close();
        } catch (DatabaseIOException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void execute(String sql) throws IOException {
        try {
            net.rim.device.api.database.Statement statement = db.createStatement(sql);
            statement.prepare();
            statement.execute();
            statement.close();            
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void execute(String sql, String[] params) throws IOException {
        try {
            final net.rim.device.api.database.Statement statement = db.createStatement(sql);
            statement.prepare();
            if(params != null){
                for (int i = 0; i < params.length; i++) {
                    statement.bind(i+1, params[i]);                
                }
            }
            statement.execute();
            statement.close();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void execute(String sql, Object [] params) throws IOException{
        try {
            final net.rim.device.api.database.Statement statement = db.createStatement(sql);
            statement.prepare();
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    Object p = params[i];
                    if (p == null) {
                        statement.bind(i + 1, (String) p);
                    } else {
                        if (p instanceof String) {
                            statement.bind(i + 1, (String) p);
                        } else if (p instanceof byte[]) {
                            statement.bind(i + 1, (byte[]) p);
                        } else if (p instanceof Double) {
                            statement.bind(i + 1, ((Double) p).doubleValue());
                        } else if (p instanceof Long) {
                            statement.bind(i + 1, ((Long) p).longValue());
                        }
                    }
                }
            }
            statement.execute();
            statement.close();
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        try {
            final net.rim.device.api.database.Statement statement = db.createStatement(sql);
            statement.prepare();
            if(params != null){
                for (int i = 0; i < params.length; i++) {
                    statement.bind(i+1, params[i]);                
                }
            }
            net.rim.device.api.database.Cursor cursor = statement.getCursor();           
            BBCursor c = new BBCursor(cursor){

                public void close() throws IOException {
                    super.close();
                    try {
                        statement.close();
                    } catch (DatabaseException ex) {
                        ex.printStackTrace();
                        throw new IOException(ex.getMessage());
                    }
                }
            
            };
            return c;            
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public Cursor executeQuery(String sql) throws IOException {
        try {
            final net.rim.device.api.database.Statement statement = db.createStatement(sql);
            statement.prepare();
            net.rim.device.api.database.Cursor cursor = statement.getCursor();           
            BBCursor c = new BBCursor(cursor){

                public void close() throws IOException {
                    super.close();
                    try {
                        statement.close();
                    } catch (DatabaseException ex) {
                        ex.printStackTrace();
                        throw new IOException(ex.getMessage());
                    }
                }
            
            };
            return c;            
        } catch (DatabaseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void rollbackTransaction() throws IOException {
        try {
            db.rollbackTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

}
