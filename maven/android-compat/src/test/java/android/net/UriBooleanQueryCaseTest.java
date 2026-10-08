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
package android.net;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `getBooleanQueryParameter` reads "false" in any case as false, as Android
/// does; `FALSE` and `False` used to read as true.
public class UriBooleanQueryCaseTest {

    @Test
    public void falseIsMatchedIgnoringCase() {
        Uri u = Uri.parse("app://host/p?a=FALSE&b=False&c=false&d=0&e=TRUE&f=yes");
        assertFalse("FALSE read as true", u.getBooleanQueryParameter("a", true));
        assertFalse("False read as true", u.getBooleanQueryParameter("b", true));
        assertFalse(u.getBooleanQueryParameter("c", true));
        assertFalse(u.getBooleanQueryParameter("d", true));
        assertTrue(u.getBooleanQueryParameter("e", false));
        assertTrue(u.getBooleanQueryParameter("f", false));
        assertTrue(u.getBooleanQueryParameter("missing", true));
    }
}
