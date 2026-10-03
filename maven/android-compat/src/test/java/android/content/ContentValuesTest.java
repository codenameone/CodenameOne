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
package android.content;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ContentValuesTest {

    @Test
    public void keepsInsertionOrder() {
        ContentValues v = new ContentValues();
        v.put("zeta", "z");
        v.put("alpha", Integer.valueOf(1));
        v.putNull("mid");
        List<String> keys = new ArrayList<String>(v.keySet());
        assertEquals("zeta", keys.get(0));
        assertEquals("alpha", keys.get(1));
        assertEquals("mid", keys.get(2));
        assertTrue(v.containsKey("mid"));
        assertNull(v.get("mid"));
        assertEquals(3, v.size());
    }

    @Test
    public void convertsLikeAndroid() {
        ContentValues v = new ContentValues();
        v.put("n", "42");
        v.put("d", Double.valueOf(2.5));
        v.put("bad", "x1");
        v.put("flag", "1");
        v.put("flagText", "TRUE");
        v.put("zero", Integer.valueOf(0));
        assertEquals(Long.valueOf(42), v.getAsLong("n"));
        assertEquals(Integer.valueOf(42), v.getAsInteger("n"));
        assertEquals(Integer.valueOf(2), v.getAsInteger("d"));
        assertEquals("2.5", v.getAsString("d"));
        assertNull(v.getAsLong("bad"));
        assertNull(v.getAsLong("missing"));
        assertEquals(Boolean.TRUE, v.getAsBoolean("flag"));
        assertEquals(Boolean.TRUE, v.getAsBoolean("flagText"));
        assertEquals(Boolean.FALSE, v.getAsBoolean("zero"));
        assertNull(v.getAsByteArray("n"));
        byte[] b = {1, 2, 3};
        v.put("blob", b);
        assertArrayEquals(b, v.getAsByteArray("blob"));
    }

    @Test
    public void copyAndEquality() {
        ContentValues a = new ContentValues();
        a.put("k", "v");
        ContentValues b = new ContentValues(a);
        assertEquals(a, b);
        b.put("k", "w");
        assertFalse(a.equals(b));
        assertEquals("v", a.getAsString("k"));
        b.remove("k");
        assertTrue(b.isEmpty());
    }
}
