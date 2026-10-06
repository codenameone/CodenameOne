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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// `getStringExtra` answers null for an extra of another type, as Android
/// does. It used to `toString()` whatever was stored, so an `int` 123 read
/// back as the string "123".
public class IntentStringExtraTest {

    @Test
    public void nonStringExtraReadsAsNull() {
        Intent i = new Intent();
        i.putExtra("n", 123);
        i.putExtra("b", true);
        i.putExtra("s", "text");
        assertNull(i.getStringExtra("n"));
        assertNull(i.getStringExtra("b"));
        assertNull(i.getStringExtra("missing"));
        assertEquals("text", i.getStringExtra("s"));
        assertEquals(123, i.getIntExtra("n", 0));
    }
}
