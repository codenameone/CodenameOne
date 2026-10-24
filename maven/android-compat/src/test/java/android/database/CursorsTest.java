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
package android.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

public class CursorsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static MatrixCursor sample() {
        MatrixCursor c = new MatrixCursor(new String[] {"_id", "Name", "score"});
        c.addRow(new Object[] {Long.valueOf(1), "ann", Double.valueOf(1.5)});
        c.addRow(Arrays.asList(new Object[] {Long.valueOf(2), "bob", null}));
        c.newRow().add(Long.valueOf(3)).add("cy").add("7");
        return c;
    }

    @Test
    public void navigatesLikeAndroid() {
        MatrixCursor c = sample();
        assertEquals(3, c.getCount());
        assertEquals(-1, c.getPosition());
        assertTrue(c.isBeforeFirst());
        assertTrue(c.moveToFirst());
        assertTrue(c.isFirst());
        assertTrue(c.moveToLast());
        assertTrue(c.isLast());
        assertFalse(c.moveToNext());
        assertTrue(c.isAfterLast());
        assertEquals(3, c.getPosition());
        assertTrue(c.moveToPrevious());
        assertEquals(2, c.getPosition());
        assertTrue(c.move(-2));
        assertEquals(0, c.getPosition());
        assertFalse(c.moveToPosition(-5));
        assertEquals(-1, c.getPosition());
    }

    @Test
    public void readsTypedValues() {
        MatrixCursor c = sample();
        c.moveToPosition(1);
        assertTrue(c.isNull(2));
        assertEquals(0.0, c.getDouble(2), 0);
        assertEquals(Cursor.FIELD_TYPE_NULL, c.getType(2));
        c.moveToPosition(2);
        assertEquals(7, c.getInt(2));
        assertEquals(Cursor.FIELD_TYPE_STRING, c.getType(2));
        c.moveToFirst();
        assertEquals(Cursor.FIELD_TYPE_FLOAT, c.getType(2));
        assertEquals(Cursor.FIELD_TYPE_INTEGER, c.getType(0));
        assertEquals("ann", c.getString(1));
    }

    @Test
    public void columnLookupIsCaseInsensitiveAndStripsTablePrefix() {
        MatrixCursor c = sample();
        assertEquals(1, c.getColumnIndex("name"));
        assertEquals(1, c.getColumnIndex("people.NAME"));
        assertEquals(-1, c.getColumnIndex("nope"));
        try {
            c.getColumnIndexOrThrow("nope");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("column 'nope' does not exist", expected.getMessage());
        }
    }

    @Test
    public void readingOffARowThrows() {
        MatrixCursor c = sample();
        try {
            c.getString(0);
            fail();
        } catch (CursorIndexOutOfBoundsException expected) {
            assertEquals("Before first row.", expected.getMessage());
        }
    }

    @Test
    public void mergeAndWrapperForward() {
        MergeCursor m = new MergeCursor(new Cursor[] {sample(), null, sample()});
        assertEquals(6, m.getCount());
        assertTrue(m.moveToPosition(4));
        assertEquals("bob", m.getString(1));
        CursorWrapper w = new CursorWrapper(m);
        assertTrue(w.moveToLast());
        assertEquals("cy", w.getString(1));
        w.close();
        assertTrue(m.isClosed());
    }

    @Test
    public void observersHearRequeryAndClose() {
        MatrixCursor c = sample();
        final int[] calls = new int[2];
        c.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                calls[0]++;
            }

            @Override
            public void onInvalidated() {
                calls[1]++;
            }
        });
        c.requery();
        c.close();
        assertEquals(1, calls[0]);
        assertEquals(1, calls[1]);
    }

    @Test
    public void databaseUtilsHelpers() {
        assertEquals("'it''s'", DatabaseUtils.sqlEscapeString("it's"));
        assertEquals("'plain'", DatabaseUtils.sqlEscapeString("plain"));
        StringBuilder sb = new StringBuilder();
        DatabaseUtils.appendValueToSql(sb, Boolean.TRUE);
        DatabaseUtils.appendValueToSql(sb, null);
        assertEquals("1NULL", sb.toString());
        assertEquals("(a=1) AND (b=2)", DatabaseUtils.concatenateWhere("a=1", "b=2"));
        assertEquals("b=2", DatabaseUtils.concatenateWhere("", "b=2"));
        String[] joined = DatabaseUtils.appendSelectionArgs(new String[] {"x"}, new String[] {"y", "z"});
        assertEquals(Arrays.asList("x", "y", "z"), Arrays.asList(joined));
        assertEquals(Cursor.FIELD_TYPE_BLOB, DatabaseUtils.getTypeOfObject(new byte[0]));
        assertEquals(DatabaseUtils.STATEMENT_SELECT, DatabaseUtils.getSqlStatementType("  select 1"));
        assertEquals(DatabaseUtils.STATEMENT_UPDATE, DatabaseUtils.getSqlStatementType("DELETE FROM t"));
        assertEquals(DatabaseUtils.STATEMENT_DDL, DatabaseUtils.getSqlStatementType("create table t(x)"));
        CharArrayBuffer buf = new CharArrayBuffer(2);
        MatrixCursor c = sample();
        c.moveToFirst();
        c.copyStringToBuffer(1, buf);
        assertEquals(3, buf.sizeCopied);
        assertEquals("ann", new String(buf.data, 0, buf.sizeCopied));
        c.moveToPosition(1);
        c.copyStringToBuffer(2, buf);
        assertEquals(0, buf.sizeCopied);
        assertNull(c.getString(2));
    }
}
