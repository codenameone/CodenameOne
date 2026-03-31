/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
