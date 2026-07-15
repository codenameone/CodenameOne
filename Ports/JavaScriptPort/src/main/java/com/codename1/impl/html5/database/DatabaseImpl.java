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

package com.codename1.impl.html5.database;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.teavm.ext.websql.WebSQL;
import java.io.IOException;

/**
 *
 * @author shannah
 */
public class DatabaseImpl extends Database {

    WebSQL.Database impl;
    
    public DatabaseImpl(WebSQL.Database impl) {
        this.impl = impl;
    }
    
    @Override
    public void beginTransaction() throws IOException {
        System.out.println("beginTransaction not supported in Javascript.  It does nothing.");
    }

    @Override
    public void commitTransaction() throws IOException {
        System.out.println("commitTransaction() not supported in Javascript.  It does nothing.");
    }

    @Override
    public void rollbackTransaction() throws IOException {
        System.out.println("rollbackTransaction() not supported in Javascript.  It does nothing.");
    }

    @Override
    public void close() throws IOException {
        
    }

    @Override
    public void execute(String string) throws IOException {
        executeQuery(string);
    }

    @Override
    public void execute(String string, String[] strings) throws IOException {
        impl.executeSql(string, strings);
    }

    @Override
    public Cursor executeQuery(String string, String[] strings) throws IOException {
        return new CursorImpl(impl.executeSql(string, strings));
    }

    @Override
    public Cursor executeQuery(String string) throws IOException {
        return executeQuery(string, new String[]{});
    }
    
}
