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
import com.codename1.db.Database;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen
 */
public class SEDatabase extends Database{
    
    private java.sql.Connection conn;
    
    public SEDatabase(java.sql.Connection conn) {
        this.conn = conn;
    }

    
    @Override
    public void beginTransaction() throws IOException {
        try {
            conn.setAutoCommit(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void commitTransaction() throws IOException {
        try {
            conn.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void execute(String sql) throws IOException {
        try {
            PreparedStatement s =  conn.prepareStatement(sql);  
            s.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        try {
            PreparedStatement s =  conn.prepareStatement(sql);  
            
            if(params != null){
                for (int i = 0; i < params.length; i++) {
                    String param = params[i];              
                    s.setString(i+1, param);
                }
            }
            s.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        try {
            PreparedStatement s =  conn.prepareStatement(sql);  

            if(params != null){
                for (int i = 0; i < params.length; i++) {
                    String param = params[i];              
                    s.setString(i+1, param);
                }
            }
            ResultSet resultSet =  s.executeQuery();
            return new SECursor(resultSet);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        try {
            PreparedStatement s =  conn.prepareStatement(sql);              
            ResultSet resultSet =  s.executeQuery();
            return new SECursor(resultSet);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
}
