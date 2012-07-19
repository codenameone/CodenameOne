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

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import com.codename1.db.Cursor;
import com.codename1.db.Database;
import java.io.IOException;

/**
 *
 * @author Chen
 */
public class AndroidDB extends Database {

    private SQLiteDatabase db;

    public AndroidDB(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public void beginTransaction() throws IOException {
        db.beginTransaction();
    }

    @Override
    public void commitTransaction() throws IOException {
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void close() throws IOException {
        db.close();
    }

    @Override
    public void execute(String sql) throws IOException {
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        try {
            SQLiteStatement s = db.compileStatement(sql);
            for (int i = 0; i < params.length; i++) {
                String p = params[i];
                s.bindString(i + 1, p);
            }
            s.execute();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        android.database.Cursor c = db.rawQuery(sql, params);
        AndroidCursor cursor = new AndroidCursor(c);
        return cursor;
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        return executeQuery(sql, new String[]{});
    }

    @Override
    public void rollbackTransaction() throws IOException {
        db.endTransaction();
    }
}
