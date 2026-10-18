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
package com.codename1.xml;

import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Element keeps attributes in a flat array until somebody asks for the
/// Hashtable; after that the table is live in both directions.
class ElementAttributesTest {

    @Test
    void setGetReplaceRemove() {
        Element e = new Element("e");
        assertNull(e.getAttributes());
        e.setAttribute("a", "1");
        e.setAttribute("b", "2");
        e.setAttribute("a", "3");
        assertEquals("3", e.getAttribute("a"));
        e.removeAttribute("a");
        assertNull(e.getAttribute("a"));
        e.removeAttribute("b");
        assertNull(e.getAttributes());
    }

    @Test
    void manyAttributesGrowTheArray() {
        Element e = new Element("e");
        for (int i = 0; i < 20; i++) {
            e.setAttribute("k" + i, "v" + i);
        }
        for (int i = 0; i < 20; i++) {
            assertEquals("v" + i, e.getAttribute("k" + i));
        }
        assertEquals(20, e.getAttributes().size());
    }

    @Test
    void returnedTableIsLive() {
        Element e = new Element("e");
        e.setAttribute("a", "1");
        Hashtable h = e.getAttributes();
        h.put("b", "2");
        assertEquals("2", e.getAttribute("b"));
        e.setAttribute("c", "3");
        assertEquals("3", h.get("c"));
    }

    @Test
    void caseInsensitiveLookupFoldsTheName() {
        XMLParser p = new XMLParser();
        // the default parser folds every character of an attribute name but the first
        Element e = p.parse(new java.io.StringReader("<E aTTR=\"x\"/>"));
        assertEquals("x", e.getAttribute("ATTR"));
        assertEquals("x", e.getAttribute("attr"));
    }

    @Test
    void nonStringKeysWork() {
        Element e = new Element("e");
        e.setAttribute((Object) Integer.valueOf(7), "seven");
        assertNull(e.getAttribute("x"));
        assertEquals("seven", e.getAttributes().get(Integer.valueOf(7)));
    }

    @Test
    void nullKeyIsRejected() {
        Element e = new Element("e");
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> e.setAttribute((String) null, "v"));
        assertNull(e.getAttributes());
    }
}
