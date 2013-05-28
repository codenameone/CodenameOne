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

import com.codename1.ui.Display;
import java.io.IOException;

/**
 * This is a Database SQLite class, this class allows developers to
 * connect to a database and preform sql queries on the data.
 * 
 * The Database class abstracts the underlying SQLite of the device if 
 * available.
 * 
 * Supported platforms for this feature are: IOS, Android, BB OS5+ and the 
 * Desktop Simulator (which supports only cursor forward navigation).
 * 
 * The SQLite should be used for very large data handling, for small storage 
 * refer to {@link com.codename1.io.Storage} which is more portable.
 * 
 * @author Chen
 */
public abstract class Database {
    
    /**
     * Opens a database or create one if not exists
     * 
     * @param databaseName the name of the database
     * @return Database Object or null if not supported on the platform
     * 
     * @throws IOException if database cannot be created
     */
    public static Database openOrCreate(String databaseName) throws IOException{
        return Display.getInstance().openOrCreate(databaseName);
    }
        
    /**
     * Indicates weather a database exists
     * 
     * @param databaseName the name of the database
     * @return true if database exists
     */
    public static boolean exists(String databaseName){
        return Display.getInstance().exists(databaseName);
    }
    
    /**
     * Deletes database
     * 
     * @param databaseName the name of the database
     * @throws IOException if database cannot be deleted
     */
    public static void delete(String databaseName) throws IOException{
        Display.getInstance().delete(databaseName);
    }
    
    /**
     * Returns the file path of the Database if exists and if supported on 
     * the platform.
     * @return the file path of the database
     */
    public static String getDatabasePath(String databaseName){
        return Display.getInstance().getDatabasePath(databaseName);    
    }
    
    /**
     * Starts a transaction
     * 
     * @throws IOException if database is not opened
     */
    public abstract void beginTransaction() throws IOException;
    
    /**
     * Commits current transaction
     * 
     * @throws IOException if database is not opened or transaction was not started
     */
    public abstract void commitTransaction() throws IOException;
    
    /**
     * Rolls back current transaction 
     * 
     * @throws IOException if database is not opened or transaction was not started
     */
    public abstract void rollbackTransaction() throws IOException;
    
    /**
     * Closes the database
     * 
     * @throws IOException 
     */
    public abstract void close() throws IOException;
    
    /**
     * Execute an update query.
     * Used for INSERT, UPDATE, DELETE and similar sql statements.
     * 
     * @param sql the sql to execute
     * 
     * @throws IOException 
     */
    public abstract void execute(String sql) throws IOException;

    /**
     * Execute an update query with params.
     * Used for INSERT, UPDATE, DELETE and similar sql statements.
     * The sql can be constructed with '?' and the params will be binded to the
     * query
     * 
     * @param sql the sql to execute
     * @param params to bind to the query where the '?' exists
     * 
     * @throws IOException 
     */
    public abstract void execute(String sql, String [] params) throws IOException;
    
    /**
     * Execute an update query with params.
     * Used for INSERT, UPDATE, DELETE and similar sql statements.
     * The sql can be constructed with '?' and the params will be binded to the
     * query
     * 
     * @param sql the sql to execute
     * @param params to bind to the query where the '?' exists, supported object 
     * types are String, byte[], Double, Long and null
     * 
     * @throws IOException 
     */
    public void execute(String sql, Object [] params) throws IOException{
        throw new RuntimeException("not implemented");
    }
    
    
    /**
     * This method should be called with SELECT type statements that return 
     * row set.  
     * 
     * @param sql the sql to execute
     * @param params to bind to the query where the '?' exists
     * @return a cursor to iterate over the results
     * 
     * @throws IOException 
     */
    public abstract Cursor executeQuery(String sql, String [] params) throws IOException;
    
    /**
     * This method should be called with SELECT type statements that return 
     * row set.  
     * 
     * @param sql the sql to execute
     * @return a cursor to iterate over the results
     * 
     * @throws IOException 
     */
    public abstract Cursor executeQuery(String sql) throws IOException;
    
}
