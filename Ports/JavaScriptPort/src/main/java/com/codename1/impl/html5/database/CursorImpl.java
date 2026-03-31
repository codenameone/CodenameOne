/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.database;

import com.codename1.db.Cursor;
import com.codename1.db.Row;
import com.codename1.db.RowExt;
import com.codename1.teavm.ext.websql.WebSQL.ResultSet;
import java.io.IOException;


/**
 *
 * @author shannah
 */
public class CursorImpl implements Cursor {

    private ResultSet peer;
    
    CursorImpl(ResultSet peer) {
        this.peer = peer;
    }
    
    @Override
    public boolean first() throws IOException {
        peer.first();
        return true;
    }

    @Override
    public boolean last() throws IOException {
        peer.last();
        return true;
    }

    @Override
    public boolean next() throws IOException {
        if (peer.hasNext()) {
            peer.next();
            return true;
        }
        return false;
    }

    @Override
    public boolean prev() throws IOException {
        peer.prev();
        return true;
    }

    @Override
    public int getColumnIndex(String string) throws IOException {
        return peer.getColumnIndex(string);
    }

    @Override
    public String getColumnName(int i) throws IOException {
        return peer.getColumnName(i);
    }

    @Override
    public int getColumnCount() throws IOException {
        return peer.getColumnCount();
    }

    @Override
    public int getPosition() throws IOException {
        return peer.position();
    }

    @Override
    public boolean position(int i) throws IOException {
        peer.setPosition(i);
        return true;
    }

    @Override
    public void close() throws IOException {
        
    }

    @Override
    public Row getRow() throws IOException {
        return new RowImpl(getPosition());
    }
    
    
    private class RowImpl implements Row {

        int index;
        
        private RowImpl(int index) {
            this.index = index;
        }
        
        @Override
        public byte[] getBlob(int i) throws IOException {
            
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public double getDouble(int i) throws IOException {
            int oldPos = -1;
            if (getPosition() != index) {
                oldPos = getPosition();
                position(index);
            }
            double out = peer.getDouble(i);
            if (oldPos != -1) {
                position(oldPos);
            }
            return out;
        }

        @Override
        public float getFloat(int i) throws IOException {
            return (float)getDouble(i);
        }

        @Override
        public int getInteger(int i) throws IOException {
            int oldPos = -1;
            if (getPosition() != index) {
                oldPos = getPosition();
                position(index);
            }
            int out = peer.getInt(i);
            if (oldPos != -1) {
                position(oldPos);
            }
            return out;
        }

        @Override
        public long getLong(int i) throws IOException {
            return (long)getInteger(i);
        }

        @Override
        public short getShort(int i) throws IOException {
            return (short) getInteger(i);
        }

        @Override
        public String getString(int i) throws IOException {
            int oldPos = -1;
            if (getPosition() != index) {
                oldPos = getPosition();
                position(index);
            }
            String out = peer.getString(i);
            if (oldPos != -1) {
                position(oldPos);
            }
            return out;
        }

    }
}
