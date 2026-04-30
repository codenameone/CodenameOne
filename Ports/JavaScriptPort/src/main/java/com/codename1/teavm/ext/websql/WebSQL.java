/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.ext.websql;

import com.codename1.io.Log;
import com.codename1.teavm.jso.util.JS;
import com.codename1.teavm.jso.util.JSType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.core.JSArray;
import com.codename1.html5.js.core.JSString;

/**
 *
 * @author shannah
 */
public class WebSQL {
    private static interface WebSQLInternal extends JSObject {
        SQLDatabase openDatabase(String name, String version, String displayName, int estimatedSize);
    }
    
    private static interface SQLDatabase extends JSObject {
        @JSProperty
        String getVersion();
        
        void transaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback, SQLVoidCallback successCallback);
        
    }
    
    @JSFunctor
    private static interface SQLVoidCallback extends JSObject {
        void handleEvent();
    }
    
    @JSFunctor
    private static interface SQLTransactionErrorCallback extends JSObject {
        void handleEvent(SQLError error);
    }
    
    private static interface SQLError extends JSObject {
        @JSProperty
        int getCode();
        
        @JSProperty
        String getMessage();
    }
    
    
    private static interface SQLTransaction extends JSObject {
        void executeSql(String sql, JSArray arguments, SQLStatementCallback callback, SQLStatementErrorCallback errorCallback);
    }
    
    @JSFunctor
    private static interface SQLStatementCallback extends JSObject {
        void handleEvent(SQLTransaction transaction, SQLResultSet resultSet);
    }
    
    @JSFunctor
    private static interface SQLTransactionCallback extends JSObject {
        void handleEvent(SQLTransaction transaction);
    }
    
    @JSFunctor
    private static interface SQLStatementErrorCallback extends JSObject {
        void handleEvent(SQLTransaction transaction, SQLError error);
    }
    
    private static interface SQLResultSet extends JSObject {
        @JSProperty
        int getInsertId();
        
        @JSProperty
        int getRowsAffected();
        
        @JSProperty
        SQLResultSetRowList getRows();
    }
    
    private static interface SQLResultSetRowList extends JSObject {
        @JSProperty
        int getLength();
        
        JSObject item(int index);
        
    }
    
    
    public static class Database {
        SQLDatabase peer;
        
        private Database(SQLDatabase peer) {
            this.peer = peer;
        }
        
        public ResultSet executeSql(final String sql, final String[] arguments) throws IOException {
            final ResultSet[] out = new ResultSet[1];
            final boolean[] complete = new boolean[1];
            final String[] errorMessage = new String[1];
            final int[] errorCode = new int[1];
            final Object lock = new Object();
            
            final SQLStatementErrorCallback statementErrorCallback = new SQLStatementErrorCallback() {

                @Override
                public void handleEvent(SQLTransaction transaction, SQLError error) {
                    errorMessage[0] = error.getMessage();
                    errorCode[0] = error.getCode();
                    complete[0] = true;
                    new Thread() {
                        public void run() {
                            synchronized(lock) {
                                lock.notifyAll();
                            }
                        }
                    }.start();
                }
                
            };
            
            final SQLStatementCallback statementCallback = new SQLStatementCallback() {

                @Override
                public void handleEvent(SQLTransaction transaction, SQLResultSet resultSet) {
                    out[0] = new ResultSet(resultSet);
                    complete[0] = true;
                    new Thread() {
                        public void run() {
                            synchronized(lock) {
                                lock.notifyAll();
                            }
                        }
                    }.start();
                }
            };
            
            
            SQLTransactionCallback transactionCallback = new SQLTransactionCallback() {

                @Override
                public void handleEvent(SQLTransaction transaction) {
                    int len = arguments != null ? arguments.length : 0;
                    JSArray arr = JSArray.create(len);
                    for (int i=0; i<len; i++) {
                        arr.set(i, JSString.valueOf(arguments[i]));
                    }
                    
                    transaction.executeSql(sql, arr, statementCallback, statementErrorCallback);
                }
                
            };
            
            SQLTransactionErrorCallback transactionErrorCallback = new SQLTransactionErrorCallback() {

                @Override
                public void handleEvent(SQLError error) {
                    errorMessage[0] = error.getMessage();
                    errorCode[0] = error.getCode();
                    complete[0] = true;
                    new Thread() {
                        public void run() {
                            synchronized(lock) {
                                lock.notifyAll();
                            }
                        }
                    }.start();
                }
            };
            
            peer.transaction(transactionCallback, transactionErrorCallback, new SQLVoidCallback() {

                @Override
                public void handleEvent() {
                    // don't thing we need to do anything here.
                }
            });
            
            while (!complete[0]) {
                synchronized(lock) {
                    try {
                        lock.wait(200);
                    } catch (InterruptedException ex) {
                        Log.e(ex);
                    }
                }
            }
            
            if (errorMessage[0] != null) {
                throw new IOException(errorMessage[0]);
                
            }
            
            return out[0];
        }
    }
    
    
    public static Database openDatabase(String name, String version, String displayName, int expectedSize) {
        return new Database(((WebSQLInternal)JS.getGlobal()).openDatabase(name, version, displayName, expectedSize));
    }
    
    public static class ResultSet {
        
        private Map<Integer, String> colNameMap = new HashMap<Integer, String>();
        private Map<String, Integer> colIndexMap = new HashMap<String, Integer>();
        private int columnCount = -1;
        
        SQLResultSet peer;
        int cursor=-1;
        JSObject row;
        private ResultSet(SQLResultSet peer) {
            this.peer = peer;
        }
        
        public int getInsertId() {
            return peer.getInsertId();
        }
        
        public int getRowsAffected() {
            return peer.getRowsAffected();
        }
        
        public int size() {
            return peer.getRows().getLength();
        }
        
        public boolean hasNext() {
            return cursor < size()-1;
        }
        
        public void next() {
            cursor++;
            row = peer.getRows().item(cursor);
        }
        
        public void prev() {
            cursor--;
            if (cursor < 0) {
                row = null;
                return;
            }
            row = peer.getRows().item(cursor);
        }
        
        public void first() {
            if (size() == 0) {
                cursor = -1;
                row = null;
                return;
            }
            cursor = 0;
            row = peer.getRows().item(cursor);
        }
        
        public void last() {
            cursor = peer.getRows().getLength()-1;
            if (cursor >=0) {
                row = peer.getRows().item(cursor);
            } else {
                row = null;
            }
        }
        
        public int position() {
            return cursor;
        }
        
        @JSBody(params={"row","colName"}, script="return row[colName]")
        private native static JSObject get(JSObject row, String colName);
        
        @JSBody(params={"row", "index"}, script="var i=0; for (var key in row){ if (i === index){ return row[key];} i++}; return null")
        private native static JSObject get(JSObject row, int index);
        
        @JSBody(params={"row", "index"}, script="var i=0; for (var key in row){ if (i === index){ return key;} i++}; return null")
        private native static String getColumnName(JSObject row, int index);
        
        @JSBody(params={"row", "colName"}, script="var i=0; for (var key in row){ if (key === colName){ return i;} i++}; return -1")
        private native static int getColumnIndex(JSObject row, String colName);
        
        @JSBody(params={"row"}, script="var i=0; for (var key in row){i++}; return i;")
        private native static int getColumnCount(JSObject row);
        
        public int getColumnCount() {
            if (columnCount == -1) {
                if (size() == 0) {
                    columnCount = 0;
                    
                } else {
                    columnCount = getColumnCount(peer.getRows().item(0));
                }
            }
            return columnCount;
        }
        
        public void setPosition(int i) {
            cursor = i;
            row = peer.getRows().item(0);
        }
        
        public String getColumnName(int index) {
            Integer i = new Integer(index);
            if (colNameMap.containsKey(i)) {
                return colNameMap.get(i);
            }
            if (size() == 0) {
                return null;
            }
            
            String name = getColumnName(peer.getRows().item(0), index);
            if (name != null) {
                colNameMap.put(i, name);
                colIndexMap.put(name, i);
            }
            return name;
            
        }
        
        public int getColumnIndex(String name) {
            if (colIndexMap.containsKey(name)) {
                return colIndexMap.get(name);
            }
            if (size() == 0) {
                return -1;
            }
            
            int index = getColumnIndex(peer.getRows().item(0), name);
            if (index != -1) {
                Integer i = new Integer(index);
                colNameMap.put(i, name);
                colIndexMap.put(name, i);
            }
            return index;
        }
        
        public int getInt(String col) {
            JSObject o = get(row, col);
            if (JS.getType(o) == JSType.STRING) {
                return Integer.parseInt(JS.unwrapString(o));
            }
            return JS.unwrapInt(o);
        }
        
        public double getDouble(String col) {
            JSObject o = get(row, col);
            if (JS.getType(o) == JSType.STRING) {
                return Double.parseDouble(JS.unwrapString(o));
            }
            return JS.unwrapDouble(o);
        }
        
        public String getString(String col) {
            JSObject o = get(row, col);
            if (JS.getType(o) == JSType.NUMBER) {
                double num = JS.unwrapDouble(o);
                if (((int)num) == Math.round(num)) {
                    return String.valueOf((int)num);
                } else {
                    return String.valueOf(num);
                }
            }
            return JS.unwrapString(o);
        }
        
        public boolean getBoolean(String col) {
            return JS.unwrapBoolean(get(row, col));
        }
        
        public int getInt(int col) {
            return getInt(getColumnName(col));
        }
        
        public double getDouble(int col) {
            return getDouble(getColumnName(col));
        }
        
        public String getString(int col) {
            return getString(getColumnName(col));
        }
        
        public boolean getBoolean(int col) {
            return getBoolean(getColumnName(col));
        }
        
        
        
    }   
    
    
}
